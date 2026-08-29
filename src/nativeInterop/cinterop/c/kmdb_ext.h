/* kmdb_ext.h - kotlin-lmdb extensions compiled alongside liblmdb on every
 * target (cinterop, emscripten/wasm, and the prebuilt JVM/Android libs).
 */
#ifndef KMDB_EXT_H
#define KMDB_EXT_H

#include "lmdb.h"

#ifdef __cplusplus
extern "C" {
#endif

#define KMDB_CRYPTO_SLOTS 4

/* Kotlin/Native commonization maps size_t to different unsigned widths on
 * different targets, so the multiplatform binding uses this stable wrapper. */
unsigned long long kotlin_lmdb_txn_id(MDB_txn *txn);

/* Enable built-in ChaCha8 page encryption (unauthenticated, MAC size 0).
 * Must be called before mdb_env_open(). keylen must be 32. */
int kmdb_env_set_encrypt_chacha8(MDB_env *env, const void *key, unsigned int keylen);

/* Enable built-in CRC32 page checksums (4 bytes per page).
 * Must be called before mdb_env_open(). */
int kmdb_env_set_checksum_crc32(MDB_env *env);

/* Custom encrypt/checksum slots (0 .. KMDB_CRYPTO_SLOTS-1). The host must
 * register callbacks with kmdb_set_host_callbacks() before using a slot. */
int kmdb_env_set_encrypt_slot(MDB_env *env, int slot, const void *key,
	unsigned int keylen, unsigned int mac);
int kmdb_env_set_checksum_slot(MDB_env *env, int slot, unsigned int size);

typedef int (*kmdb_enc_host_cb)(int slot, const void *src, unsigned int src_len,
	void *dst, unsigned int dst_len, const void *iv, unsigned int iv_len, int encdec);
typedef void (*kmdb_sum_host_cb)(int slot, const void *src, unsigned int src_len,
	void *dst, unsigned int dst_len, const void *key, unsigned int key_len);

void kmdb_set_host_callbacks(kmdb_enc_host_cb enc, kmdb_sum_host_cb sum);

/* Path-based wrapper for mdb_env_incr_loadfd (LMDB only offers an fd API).
 * The env must be open with MDB_WRITEMAP and have exclusive access. */
int kmdb_env_incr_load_path(MDB_env *env, const char *path);

#ifdef __cplusplus
}
#endif

#endif /* KMDB_EXT_H */
