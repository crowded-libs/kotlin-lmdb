package lmdb

/**
 * Custom page cipher used with [Env.setEncryption].
 *
 * [transform] is invoked once per page. The returned array must be the same
 * length as [src]. [iv] is the page number and txn id (upstream IV).
 * [encrypt] is true when writing, false when reading.
 */
fun interface EnvEncryptor {
    fun transform(src: ByteArray, iv: ByteArray, encrypt: Boolean): ByteArray
}

/**
 * Custom page checksum used with [Env.setChecksum].
 * The returned array must be exactly the size passed to [Env.setChecksum].
 */
fun interface EnvChecksum {
    fun digest(src: ByteArray, key: ByteArray?): ByteArray
}

/**
 * Slot table for custom encrypt/checksum callbacks. LMDB's C callbacks do not
 * receive the env pointer, so each active custom cipher occupies a trampoline slot.
 */
internal object EnvCryptoRegistry {
    const val SLOT_COUNT = 4

    private val encryptors = arrayOfNulls<EnvEncryptor>(SLOT_COUNT)
    private val checksums = arrayOfNulls<EnvChecksum>(SLOT_COUNT)

    fun allocEncryptor(encryptor: EnvEncryptor): Int {
        for (i in encryptors.indices) {
            if (encryptors[i] == null) {
                encryptors[i] = encryptor
                return i
            }
        }
        throw LmdbException("No free encryption slots (max $SLOT_COUNT concurrent custom encryptors)")
    }

    fun freeEncryptor(slot: Int) {
        if (slot in encryptors.indices) encryptors[slot] = null
    }

    fun encryptor(slot: Int): EnvEncryptor? =
        encryptors.getOrNull(slot)

    fun allocChecksum(checksum: EnvChecksum): Int {
        for (i in checksums.indices) {
            if (checksums[i] == null) {
                checksums[i] = checksum
                return i
            }
        }
        throw LmdbException("No free checksum slots (max $SLOT_COUNT concurrent custom checksums)")
    }

    fun freeChecksum(slot: Int) {
        if (slot in checksums.indices) checksums[slot] = null
    }

    fun checksum(slot: Int): EnvChecksum? =
        checksums.getOrNull(slot)
}
