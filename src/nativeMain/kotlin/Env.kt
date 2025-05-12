package lmdb

import kotlinx.cinterop.*

actual class Env : AutoCloseable {
    internal val ptr: CPointer<MDB_env>

    init {
        ptr = memScoped {
            val ptrVar = allocPointerTo<MDB_env>()
            check(mdb_env_create(ptrVar.ptr))
            checkNotNull(ptrVar.value)
        }
    }


    var isOpened: Boolean = false
        private set
    private var isClosed = false

    actual var maxDatabases: UInt = 0u
        set(value) {
            check(mdb_env_set_maxdbs(ptr, value))
            field = value
        }

    @OptIn(UnsafeNumber::class)
    actual var mapSize: ULong = 1024UL * 1024UL * 50UL
        set(value) {
            check(mdb_env_set_mapsize(ptr, value.convert()))
            field = value
        }

    actual var maxReaders: UInt = 0u
        get() {
            return memScoped {
                val readersVar = alloc<UIntVar>()
                check(mdb_env_get_maxreaders(ptr, readersVar.ptr))
                readersVar.value
            }
        }
        set(value) {
            check(mdb_env_set_maxreaders(ptr, value))
            field = value
        }
        
    actual val maxKeySize: UInt
        get() {
            return mdb_env_get_maxkeysize(ptr).toUInt()
        }
        
    actual val staleReaderCount: UInt
        get() {
            return memScoped {
                val deadVar = alloc<IntVar>()
                check(mdb_reader_check(ptr, deadVar.ptr))
                deadVar.value.toUInt()
            }
        }

    actual var flags: Set<EnvOption> = emptySet()
        get() {
            val flagsValue = memScoped {
                val flagsVar = alloc<UIntVar>()
                check(mdb_env_get_flags(ptr, flagsVar.ptr))
                flagsVar.value
            }
            return EnvOption.entries.filter { flagsValue and it.option == it.option }.toSet()
        }
        set(value) {
            // Get current flags first
            val currentFlags = this.flags
            
            // Clear flags that are in current but not in new value
            val flagsToClear = currentFlags.minus(value)
            flagsToClear.forEach { flag ->
                check(mdb_env_set_flags(ptr, flag.option, 0))
            }
            
            // Set flags that are in new value but not in current
            val flagsToSet = value.minus(currentFlags)
            flagsToSet.forEach { flag ->
                check(mdb_env_set_flags(ptr, flag.option, 1))
            }
            
            field = value
        }

    @OptIn(UnsafeNumber::class)
    actual val stat: Stat?
        get() {
            return memScoped {
                val statPtr = alloc<MDB_stat>()
                check(mdb_env_stat(ptr, statPtr.ptr))
                Stat(
                    statPtr.ms_branch_pages.convert(), statPtr.ms_depth, statPtr.ms_entries.convert(), statPtr.ms_leaf_pages.convert(),
                    statPtr.ms_overflow_pages.convert(), statPtr.ms_psize
                )
            }
        }

    @OptIn(UnsafeNumber::class)
    actual val info: EnvInfo?
        get() {
            return memScoped {
            val envInfo = alloc<MDB_envinfo>()
            check(mdb_env_info(ptr, envInfo.ptr))
                EnvInfo(envInfo.me_last_pgno.convert(), envInfo.me_last_txnid.convert(), envInfo.me_mapaddr.toLong().toULong(),
                    envInfo.me_mapsize.convert(), envInfo.me_maxreaders, envInfo.me_numreaders)
            }
        }

    actual fun open(path: String, vararg options: EnvOption, mode: String) {
        isOpened = true
        val result = mdb_env_open(ptr, path, options.asIterable().toFlags(), mode.toUShort(8).convert())
        check(result)
    }

    actual fun beginTxn(vararg options: TxnOption) : Txn {
        return Txn(this, *options)
    }

    actual fun copyTo(path: String, compact: Boolean) {
        val flags = if (compact) {
            0x01u
        } else {
            0u
        }
        check(mdb_env_copy2(ptr, path, flags))
    }
    
    actual fun copyTo(path: String) {
        check(mdb_env_copy(ptr, path))
    }

    actual fun sync(force: Boolean) {
        val forceInt = if(force) 1 else 0
        check(mdb_env_sync(ptr, forceInt))
    }

    actual override fun close() {
        if (isClosed)
            return
        if (isOpened) {
            mdb_env_close(ptr)
        }
        isClosed = true
    }
}