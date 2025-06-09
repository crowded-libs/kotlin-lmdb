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
        isClosed = true
    }
}