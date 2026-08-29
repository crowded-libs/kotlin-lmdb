/* kmdb_ext.c - kotlin-lmdb extensions. See kmdb_ext.h. */
#include "kmdb_ext.h"
#include "chacha8.h"

#include <errno.h>
#include <string.h>

#ifdef _WIN32
#include <windows.h>
#else
#include <fcntl.h>
#include <unistd.h>
#endif

unsigned long long
kotlin_lmdb_txn_id(MDB_txn *txn)
{
	return (unsigned long long)mdb_txn_id(txn);
}

#if MDB_RPAGE_CACHE

static kmdb_enc_host_cb kmdb_enc_host;
static kmdb_sum_host_cb kmdb_sum_host;

void
kmdb_set_host_callbacks(kmdb_enc_host_cb enc, kmdb_sum_host_cb sum)
{
	kmdb_enc_host = enc;
	kmdb_sum_host = sum;
}

#ifdef __EMSCRIPTEN__
extern int kmdb_js_enc(int slot, const void *src, unsigned int src_len,
	void *dst, unsigned int dst_len, const void *iv, unsigned int iv_len, int encdec);
extern void kmdb_js_sum(int slot, const void *src, unsigned int src_len,
	void *dst, unsigned int dst_len, const void *key, unsigned int key_len);
#endif

static void
kmdb_fold_iv(uint8_t out[CHACHA8_IV_SIZE], const void *iv, size_t ivlen)
{
	const unsigned char *p = (const unsigned char *)iv;
	size_t i;
	memset(out, 0, CHACHA8_IV_SIZE);
	/* LMDB's IV is pgno||txnid (8 or 16 bytes). ChaCha8 only takes 8.
	 * XOR successive bytes so both halves mix in; do not put txnid in
	 * the block counter (a 64KB page already uses 1024 blocks). */
	for (i = 0; i < ivlen; i++)
		out[i % CHACHA8_IV_SIZE] ^= p[i];
}

static int
kmdb_chacha8_encfunc(const MDB_val *src, MDB_val *dst, const MDB_val *key, int encdec)
{
	uint8_t iv[CHACHA8_IV_SIZE];
	(void)encdec;
	kmdb_fold_iv(iv, key[1].mv_data, key[1].mv_size);
	chacha8(src->mv_data, src->mv_size,
		(const uint8_t *)key[0].mv_data,
		iv,
		(char *)dst->mv_data);
	return 0;
}

int
kmdb_env_set_encrypt_chacha8(MDB_env *env, const void *key, unsigned int keylen)
{
	MDB_val kv;
	if (!key || keylen != CHACHA8_KEY_SIZE)
		return EINVAL;
	kv.mv_size = keylen;
	kv.mv_data = (void *)key;	/* mdb_env_set_encrypt deep-copies the key */
	return mdb_env_set_encrypt(env, kmdb_chacha8_encfunc, &kv, 0);
}

static uint32_t
kmdb_crc32(uint32_t crc, const unsigned char *buf, size_t len)
{
	static uint32_t table[256];
	static int have_table = 0;
	if (!have_table) {
		uint32_t rem;
		int i, j;
		for (i = 0; i < 256; i++) {
			rem = i;
			for (j = 0; j < 8; j++)
				rem = (rem & 1) ? (rem >> 1) ^ 0xedb88320 : rem >> 1;
			table[i] = rem;
		}
		have_table = 1;
	}
	crc = ~crc;
	while (len--)
		crc = (crc >> 8) ^ table[(crc ^ *buf++) & 0xff];
	return ~crc;
}

static void
kmdb_crc32_sumfunc(const MDB_val *src, MDB_val *dst, const MDB_val *key)
{
	uint32_t crc;
	(void)key;
	crc = kmdb_crc32(0, (const unsigned char *)src->mv_data, src->mv_size);
	memcpy(dst->mv_data, &crc, sizeof(crc));
}

int
kmdb_env_set_checksum_crc32(MDB_env *env)
{
	return mdb_env_set_checksum(env, kmdb_crc32_sumfunc, sizeof(uint32_t));
}

static int
kmdb_slot_enc(int slot, const MDB_val *src, MDB_val *dst, const MDB_val *key, int encdec)
{
#ifdef __EMSCRIPTEN__
	if (!kmdb_enc_host)
		return kmdb_js_enc(slot, src->mv_data, (unsigned int)src->mv_size,
			dst->mv_data, (unsigned int)dst->mv_size,
			key[1].mv_data, (unsigned int)key[1].mv_size, encdec);
#endif
	if (!kmdb_enc_host)
		return ENOTSUP;
	return kmdb_enc_host(slot, src->mv_data, (unsigned int)src->mv_size,
		dst->mv_data, (unsigned int)dst->mv_size,
		key[1].mv_data, (unsigned int)key[1].mv_size, encdec);
}

static void
kmdb_slot_sum(int slot, const MDB_val *src, MDB_val *dst, const MDB_val *key)
{
	const void *kdata = key ? key->mv_data : NULL;
	unsigned int klen = key ? (unsigned int)key->mv_size : 0;
#ifdef __EMSCRIPTEN__
	if (!kmdb_sum_host) {
		kmdb_js_sum(slot, src->mv_data, (unsigned int)src->mv_size,
			dst->mv_data, (unsigned int)dst->mv_size, kdata, klen);
		return;
	}
#endif
	if (!kmdb_sum_host)
		return;
	kmdb_sum_host(slot, src->mv_data, (unsigned int)src->mv_size,
		dst->mv_data, (unsigned int)dst->mv_size, kdata, klen);
}

#define KMDB_ENC_TRAMP(n) \
static int kmdb_enc_##n(const MDB_val *src, MDB_val *dst, const MDB_val *key, int encdec) { \
	return kmdb_slot_enc(n, src, dst, key, encdec); \
}
#define KMDB_SUM_TRAMP(n) \
static void kmdb_sum_##n(const MDB_val *src, MDB_val *dst, const MDB_val *key) { \
	kmdb_slot_sum(n, src, dst, key); \
}

KMDB_ENC_TRAMP(0)
KMDB_ENC_TRAMP(1)
KMDB_ENC_TRAMP(2)
KMDB_ENC_TRAMP(3)
KMDB_SUM_TRAMP(0)
KMDB_SUM_TRAMP(1)
KMDB_SUM_TRAMP(2)
KMDB_SUM_TRAMP(3)

static MDB_enc_func *kmdb_enc_tramps[KMDB_CRYPTO_SLOTS] = {
	kmdb_enc_0, kmdb_enc_1, kmdb_enc_2, kmdb_enc_3
};
static MDB_sum_func *kmdb_sum_tramps[KMDB_CRYPTO_SLOTS] = {
	kmdb_sum_0, kmdb_sum_1, kmdb_sum_2, kmdb_sum_3
};

int
kmdb_env_set_encrypt_slot(MDB_env *env, int slot, const void *key,
	unsigned int keylen, unsigned int mac)
{
	MDB_val kv;
	if (slot < 0 || slot >= KMDB_CRYPTO_SLOTS || !key || keylen == 0)
		return EINVAL;
	kv.mv_size = keylen;
	kv.mv_data = (void *)key;
	return mdb_env_set_encrypt(env, kmdb_enc_tramps[slot], &kv, mac);
}

int
kmdb_env_set_checksum_slot(MDB_env *env, int slot, unsigned int size)
{
	if (slot < 0 || slot >= KMDB_CRYPTO_SLOTS || size == 0)
		return EINVAL;
	return mdb_env_set_checksum(env, kmdb_sum_tramps[slot], size);
}

#else /* !MDB_RPAGE_CACHE */

void
kmdb_set_host_callbacks(kmdb_enc_host_cb enc, kmdb_sum_host_cb sum)
{
	(void)enc; (void)sum;
}

int
kmdb_env_set_encrypt_chacha8(MDB_env *env, const void *key, unsigned int keylen)
{
	(void)env; (void)key; (void)keylen;
	return ENOTSUP;
}

int
kmdb_env_set_checksum_crc32(MDB_env *env)
{
	(void)env;
	return ENOTSUP;
}

int
kmdb_env_set_encrypt_slot(MDB_env *env, int slot, const void *key,
	unsigned int keylen, unsigned int mac)
{
	(void)env; (void)slot; (void)key; (void)keylen; (void)mac;
	return ENOTSUP;
}

int
kmdb_env_set_checksum_slot(MDB_env *env, int slot, unsigned int size)
{
	(void)env; (void)slot; (void)size;
	return ENOTSUP;
}

#endif /* MDB_RPAGE_CACHE */

int
kmdb_env_incr_load_path(MDB_env *env, const char *path)
{
	int rc;
#ifdef _WIN32
	HANDLE fd = CreateFileA(path, GENERIC_READ, FILE_SHARE_READ, NULL,
		OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
	if (fd == INVALID_HANDLE_VALUE)
		return ENOENT;
	rc = mdb_env_incr_loadfd(env, fd);
	CloseHandle(fd);
#else
	int fd = open(path, O_RDONLY);
	if (fd < 0)
		return errno;
	rc = mdb_env_incr_loadfd(env, fd);
	close(fd);
#endif
	return rc;
}
