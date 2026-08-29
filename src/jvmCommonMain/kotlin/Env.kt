package lmdb

import com.sun.jna.Pointer

actual class Env : AutoCloseable {
    internal val ptr: Pointer

    init {
        ptr = LmdbJna.mdb_env_create()
    }

    private var _isOpened = false
    var isOpened: Boolean
        get() = _isOpened
        private set(value) {
            _isOpened = value
        }
    private var isClosed = false

    actual var maxDatabases: UInt = 0u
        set(value) {
            check(LmdbJna.mdb_env_set_maxdbs(ptr, value.toInt()))
            field = value
        }

    actual var mapSize: ULong = 1024UL * 1024UL * 100UL
        set(value) {
            check(LmdbJna.mdb_env_set_mapsize(ptr, value.toLong()))
            field = value
        }

    actual var maxReaders: UInt = 0u
        get() {
            val result = LmdbJna.mdb_env_get_maxreaders(ptr)
            if (result < 0) {
                throw LmdbException("Failed to get max readers: ${LmdbJna.mdb_strerror(result)}")
            }
            return result.toUInt()
        }
        set(value) {
            check(LmdbJna.mdb_env_set_maxreaders(ptr, value.toInt()))
            field = value
        }
        
    actual val maxKeySize: UInt
        get() {
            return LmdbJna.mdb_env_get_maxkeysize(ptr).toUInt()
        }
        
    actual val staleReaderCount: UInt
        get() {
            val result = LmdbJna.mdb_reader_check(ptr)
            if (result < 0) {
                throw LmdbException("Failed to check readers: ${LmdbJna.mdb_strerror(result)}")
            }
            return result.toUInt()
        }

    actual var flags: Set<EnvOption> = emptySet()
        get() {
            val flagsValue = LmdbJna.mdb_env_get_flags(ptr)
            if (flagsValue < 0) {
                throw LmdbException("Failed to get environment flags: ${LmdbJna.mdb_strerror(flagsValue)}")
            }
            val flagsUInt = flagsValue.toUInt()
            return EnvOption.entries.filter { flagsUInt and it.option == it.option }.toSet()
        }
        set(value) {
            // Get current flags first
            val currentFlags = this.flags
            
            // Clear flags that are in current but not in new value
            val flagsToClear = currentFlags.minus(value)
            flagsToClear.forEach { flag ->
                check(LmdbJna.mdb_env_set_flags(ptr, flag.option.toInt(), 0))
            }
            
            // Set flags that are in new value but not in current
            val flagsToSet = value.minus(currentFlags)
            flagsToSet.forEach { flag ->
                check(LmdbJna.mdb_env_set_flags(ptr, flag.option.toInt(), 1))
            }
            
            field = value
        }

    actual var pageSize: UInt = 0u
        set(value) {
            checkOpened(false)
            check(LmdbJna.mdb_env_set_pagesize(ptr, value.toInt()))
            field = value
        }

    private var encryptSlot: Int? = null
    private var checksumSlot: Int? = null

    actual fun setEncryptionChaCha8(key: ByteArray) {
        checkOpened(false)
        if (key.size != 32) throw LmdbException("ChaCha8 key must be 32 bytes")
        val mem = com.sun.jna.Memory(key.size.toLong())
        mem.write(0, key, 0, key.size)
        check(LmdbJna.kmdb_env_set_encrypt_chacha8(ptr, mem, key.size))
    }

    actual fun setEncryption(key: ByteArray, encryptor: EnvEncryptor, macBytes: UInt) {
        checkOpened(false)
        if (macBytes != 0u) throw LmdbException("AEAD macBytes is not supported")
        if (key.isEmpty()) throw LmdbException("Encryption key must not be empty")
        EnvCryptoHost.install()
        encryptSlot?.let { EnvCryptoRegistry.freeEncryptor(it) }
        val slot = EnvCryptoRegistry.allocEncryptor(encryptor)
        encryptSlot = slot
        val mem = com.sun.jna.Memory(key.size.toLong())
        mem.write(0, key, 0, key.size)
        val rc = LmdbJna.kmdb_env_set_encrypt_slot(ptr, slot, mem, key.size, macBytes.toInt())
        if (rc != 0) {
            EnvCryptoRegistry.freeEncryptor(slot)
            encryptSlot = null
            check(rc)
        }
    }

    actual fun setChecksumCrc32() {
        checkOpened(false)
        check(LmdbJna.kmdb_env_set_checksum_crc32(ptr))
    }

    actual fun setChecksum(checksum: EnvChecksum, size: UInt) {
        checkOpened(false)
        if (size == 0u) throw LmdbException("Checksum size must be greater than 0")
        EnvCryptoHost.install()
        checksumSlot?.let { EnvCryptoRegistry.freeChecksum(it) }
        val slot = EnvCryptoRegistry.allocChecksum(checksum)
        checksumSlot = slot
        val rc = LmdbJna.kmdb_env_set_checksum_slot(ptr, slot, size.toInt())
        if (rc != 0) {
            EnvCryptoRegistry.freeChecksum(slot)
            checksumSlot = null
            check(rc)
        }
    }

    private fun checkOpened(mustBeOpen: Boolean) {
        if (mustBeOpen && !_isOpened) throw LmdbException("Env is not open")
        if (!mustBeOpen && _isOpened) throw LmdbException("Env is already open")
    }

    actual val stat: Stat?
        get() {
            val mdbStat = LmdbJna.Stat()
            check(LmdbJna.mdb_env_stat(ptr, mdbStat))
            return Stat(
                branchPages = mdbStat.branchPages.toULong(),
                depth = mdbStat.depth.toUInt(),
                entries = mdbStat.entries.toULong(),
                leafPages = mdbStat.leafPages.toULong(),
                overflowPages = mdbStat.overflowPages.toULong(),
                pSize = mdbStat.pageSize.toUInt()
            )
        }

    actual val info: EnvInfo?
        get() {
            val mdbEnvInfo = LmdbJna.EnvInfo()
            check(LmdbJna.mdb_env_info(ptr, mdbEnvInfo))
            return EnvInfo(
                lastPgNo = mdbEnvInfo.lastPgno.toULong(),
                lastTxnId = mdbEnvInfo.lastTxnid.toULong(),
                mapAddr = mdbEnvInfo.mapaddr.toULong(),
                mapSize = mdbEnvInfo.mapsize.toULong(),
                maxReader = mdbEnvInfo.maxReaders.toUInt(),
                numReaders = mdbEnvInfo.numReaders.toUInt()
            )
        }

    actual fun open(path: String, vararg options: EnvOption, mode: String) {
        isOpened = true
        check(LmdbJna.mdb_env_open(ptr, path, options.asIterable().toFlags().toInt(), mode.toInt(8)))
    }

    actual fun beginTxn(vararg options: TxnOption) : Txn {
        return Txn(this, *options)
    }

    actual fun copyTo(path: String, compact: Boolean) {
        val flags = if (compact) {
            0x01
        } else {
            0
        }
        check(LmdbJna.mdb_env_copy2(ptr, path, flags))
    }
    
    actual fun copyTo(path: String) {
        check(LmdbJna.mdb_env_copy(ptr, path))
    }

    actual fun sync(force: Boolean) {
        val forceInt = if (force) 1 else 0
        check(LmdbJna.mdb_env_sync(ptr, forceInt))
    }

    actual override fun close() {
        if (isClosed)
            return
        if (isOpened) {
            LmdbJna.mdb_env_close(ptr)
        }
        isOpened = false
        encryptSlot?.let { EnvCryptoRegistry.freeEncryptor(it) }
        checksumSlot?.let { EnvCryptoRegistry.freeChecksum(it) }
        encryptSlot = null
        checksumSlot = null
        isClosed = true
    }
}