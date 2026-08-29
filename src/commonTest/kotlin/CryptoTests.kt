package lmdb

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CryptoTests {

    @Test
    fun `chacha8 round trip`() {
        val key = ByteArray(32) { it.toByte() }
        val path = pathCreateTestDir()
        Env().use { env ->
            env.setEncryptionChaCha8(key)
            env.open(path)
            env.beginTxn {
                val dbi = dbiOpen(null, DbiOption.Create)
                put(dbi, "k".encodeToByteArray(), "secret".encodeToByteArray())
                commit()
            }
        }
        Env().use { env ->
            env.setEncryptionChaCha8(key)
            env.open(path)
            env.beginTxn {
                val dbi = dbiOpen()
                val value = get(dbi, "k".encodeToByteArray()).toValueByteArray()?.decodeToString()
                assertEquals("secret", value)
            }
        }
    }

    @Test
    fun `chacha8 wrong key cannot read plaintext`() {
        val path = pathCreateTestDir()
        Env().use { env ->
            env.setEncryptionChaCha8(ByteArray(32) { 1 })
            env.open(path)
            env.beginTxn {
                val dbi = dbiOpen(null, DbiOption.Create)
                put(dbi, "k".encodeToByteArray(), "secret".encodeToByteArray())
                commit()
            }
        }
        Env().use { env ->
            env.setEncryptionChaCha8(ByteArray(32) { 2 })
            env.open(path)
            try {
                env.beginTxn {
                    val dbi = dbiOpen()
                    val value = get(dbi, "k".encodeToByteArray()).toValueByteArray()?.decodeToString()
                    assertNotEquals("secret", value)
                }
            } catch (e: LmdbException) {
                // Wrong key decrypts to an invalid page; native often reports PAGE_NOTFOUND.
                assertTrue(
                    e.message?.contains("PAGE_NOTFOUND") == true ||
                        e.message?.contains("CORRUPTED") == true ||
                        e.message?.contains("INVALID") == true,
                    "unexpected failure: ${e.message}"
                )
            }
        }
    }

    @Test
    fun `crc32 round trip`() {
        val path = pathCreateTestDir()
        Env().use { env ->
            env.setChecksumCrc32()
            env.open(path)
            env.beginTxn {
                val dbi = dbiOpen(null, DbiOption.Create)
                put(dbi, "k".encodeToByteArray(), "checked".encodeToByteArray())
                commit()
            }
        }
        Env().use { env ->
            env.setChecksumCrc32()
            env.open(path)
            env.beginTxn {
                val dbi = dbiOpen()
                assertEquals("checked", get(dbi, "k".encodeToByteArray()).toValueByteArray()?.decodeToString())
            }
        }
    }

    @Test
    fun `custom xor encryptor round trip or unsupported`() {
        val xor = EnvEncryptor { src, _, _ -> src.map { (it.toInt() xor 0x5A).toByte() }.toByteArray() }
        val path = pathCreateTestDir()
        try {
            Env().use { env ->
                env.setEncryption(ByteArray(16) { 7 }, xor)
                env.open(path)
                env.beginTxn {
                    val dbi = dbiOpen(null, DbiOption.Create)
                    put(dbi, "k".encodeToByteArray(), "hello".encodeToByteArray())
                    commit()
                }
            }
            Env().use { env ->
                env.setEncryption(ByteArray(16) { 7 }, xor)
                env.open(path)
                env.beginTxn {
                    val dbi = dbiOpen()
                    assertEquals("hello", get(dbi, "k".encodeToByteArray()).toValueByteArray()?.decodeToString())
                }
            }
        } catch (e: LmdbException) {
            assertTrue(
                e.message?.contains("not supported on wasmJs") == true,
                "unexpected failure: ${e.message}"
            )
        }
    }

    @Test
    fun `pageSize can be set before open`() {
        val env = createRandomTestEnv(open = false)
        env.use {
            it.pageSize = 4096u
            it.open(pathCreateTestDir())
            assertEquals(4096u, it.stat!!.pSize)
            assertTrue(it.maxKeySize > 0u)
        }
    }

    @Test
    fun `custom encryptor rejects nonzero macBytes`() {
        val env = createRandomTestEnv(open = false)
        env.use {
            assertFailsWith<LmdbException> {
                it.setEncryption(ByteArray(16) { 1 }, { src, _, _ -> src }, 16u)
            }
        }
    }

    @Test
    fun `encryption after open is rejected`() {
        val env = createRandomTestEnv()
        env.use {
            assertFailsWith<LmdbException> {
                it.setEncryptionChaCha8(ByteArray(32))
            }
        }
    }
}
