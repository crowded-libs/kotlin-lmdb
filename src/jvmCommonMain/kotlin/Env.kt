package lmdb

import jnr.ffi.byref.PointerByReference
import lmdb.Library.Companion.LMDB
import jnr.ffi.Pointer
import jnr.ffi.byref.IntByReference

actual class Env : AutoCloseable {
    private val envPtr = PointerByReference()
    internal val ptr: Pointer

    init {
        LMDB.mdb_env_create(envPtr)
        ptr = envPtr.value
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
            LMDB.mdb_env_set_maxdbs(ptr, value.toInt())
            field = value
        }

    actual var mapSize: ULong = 1024UL * 1024UL * 100UL
        set(value) {
            LMDB.mdb_env_set_mapsize(ptr, value.toLong())
            field = value
        }

    actual var maxReaders: UInt = 0u
        get() {
            val readersRef = IntByReference()
            check(LMDB.mdb_env_get_maxreaders(ptr, readersRef))
            return readersRef.value.toUInt()
        }
        set(value) {
            LMDB.mdb_env_set_maxreaders(ptr, value.toInt())
            field = value
        }
        
    actual val maxKeySize: UInt
        get() {
            return LMDB.mdb_env_get_maxkeysize(ptr).toUInt()
        }
        
    actual val staleReaderCount: UInt
        get() {
            val deadRef = IntByReference()
            check(LMDB.mdb_reader_check(ptr, deadRef))
            return deadRef.value.toUInt()
        }

    actual var flags: Set<EnvOption> = emptySet()
        get() {
            val flagsRef = IntByReference()
            check(LMDB.mdb_env_get_flags(ptr, flagsRef))
            val flagsValue = flagsRef.value.toUInt()
            return EnvOption.entries.filter { flagsValue and it.option == it.option }.toSet()
        }
        set(value) {
            // Get current flags first
            val currentFlags = this.flags
            
            // Clear flags that are in current but not in new value
            val flagsToClear = currentFlags.minus(value)
            flagsToClear.forEach { flag ->
                check(LMDB.mdb_env_set_flags(ptr, flag.option.toInt(), 0))
            }
            
            // Set flags that are in new value but not in current
            val flagsToSet = value.minus(currentFlags)
            flagsToSet.forEach { flag ->
                check(LMDB.mdb_env_set_flags(ptr, flag.option.toInt(), 1))
            }
            
            field = value
        }

    actual val stat: Stat?
        get() {
            val mdbStat: Library.MDB_stat = Library.MDB_stat(Library.RUNTIME)
            LMDB.mdb_env_stat(ptr, mdbStat)
            return Stat(mdbStat.f2_ms_branch_pages.get().toULong(), mdbStat.f1_ms_depth.get().toUInt(),
                mdbStat.f5_ms_entries.get().toULong(), mdbStat.f3_ms_leaf_pages.get().toULong(),
                mdbStat.f4_ms_overflow_pages.get().toULong(), mdbStat.f0_ms_psize.get().toUInt())
        }

    actual val info: EnvInfo?
        get() {
            val mdbEnvInfo: Library.MDB_envinfo = Library.MDB_envinfo(Library.RUNTIME)
            check(LMDB.mdb_env_info(ptr, mdbEnvInfo))
            return EnvInfo(mdbEnvInfo.f2_me_last_pgno.longValue().toULong(), mdbEnvInfo.f3_me_last_txnid.longValue().toULong(),
                mdbEnvInfo.f0_me_mapaddr.longValue().toULong(), mdbEnvInfo.f1_me_mapsize.longValue().toULong(),
                mdbEnvInfo.f4_me_maxreaders.intValue().toUInt(), mdbEnvInfo.f5_me_numreaders.intValue().toUInt())
        }

    actual fun open(path: String, vararg options: EnvOption, mode: String) {
        isOpened = true
        check(LMDB.mdb_env_open(ptr, path, options.asIterable().toFlags().toInt(), mode.toInt(8)))
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
        check(LMDB.mdb_env_copy2(ptr, path, flags))
    }
    
    actual fun copyTo(path: String) {
        check(LMDB.mdb_env_copy(ptr, path))
    }

    actual fun sync(force: Boolean) {
        val forceInt = if (force) 1 else 0
        check(LMDB.mdb_env_sync(ptr, forceInt))
    }

    actual override fun close() {
        if (isClosed)
            return
        if (isOpened) {
            LMDB.mdb_env_close(ptr)
        }
        isOpened = false
        isClosed = true
    }
}