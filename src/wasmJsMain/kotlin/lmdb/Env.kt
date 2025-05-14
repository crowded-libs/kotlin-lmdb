package lmdb

import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import lmdb.Constants.ERROR_ENV_NOT_OPENED
import lmdb.Constants.MDB_COMPACT_FLAG
import lmdb.Constants.MDB_ENVINFO_SIZE
import lmdb.Constants.MDB_STAT_SIZE
import lmdb.Constants.POINTER_SIZE
import lmdb.Constants.TYPE_I32

/**
 * Implementation of the Env class for the wasmJs target.
 * This implementation uses @WasmImport to directly call LMDB WASM functions.
 */
@OptIn(UnsafeWasmMemoryApi::class)
actual class Env : AutoCloseable {
    // The environment pointer (would be a pointer to MDB_env in C)
    internal var ptr: Int = 0

    private var isOpened = false
    private var isClosed = false

    init {
        // Create a new environment
        val envPtr = LMDB.malloc(POINTER_SIZE)
        val result = LMDB.mdb_env_create(envPtr)
        if (result != 0) {
            LMDB.free(envPtr)
            throw LmdbException(native_mdb_strerror(result))
        }
        ptr = LMDB.getValue(envPtr, TYPE_I32)
        LMDB.free(envPtr)
    }

    actual var maxDatabases: UInt = 0u
        set(value) {
            val result = LMDB.mdb_env_set_maxdbs(ptr, value.toInt())
            if (result != 0) {
                throw LmdbException(native_mdb_strerror(result))
            }
            field = value
        }

    actual var mapSize: ULong = 1024UL * 1024UL * 50UL
        set(value) {
            val result = LMDB.mdb_env_set_mapsize(ptr, value.toDouble())
            if (result != 0) {
                throw LmdbException(native_mdb_strerror(result))
            }
            field = value
        }

    actual var maxReaders: UInt = 0u
        get() {
            val readersPtr = LMDB.malloc(POINTER_SIZE)
            val result = LMDB.mdb_env_get_maxreaders(ptr, readersPtr)
            if (result != 0) {
                LMDB.free(readersPtr)
                throw LmdbException(native_mdb_strerror(result))
            }
            val readers = LMDB.getValue(readersPtr, TYPE_I32)
            LMDB.free(readersPtr)
            return readers.toUInt()
        }
        set(value) {
            val result = LMDB.mdb_env_set_maxreaders(ptr, value.toInt())
            if (result != 0) {
                throw LmdbException(native_mdb_strerror(result))
            }
            field = value
        }

    actual val maxKeySize: UInt
        get() {
            return LMDB.mdb_env_get_maxkeysize(ptr).toUInt()
        }

    actual val staleReaderCount: UInt
        get() {
            val deadPtr = LMDB.malloc(POINTER_SIZE)
            val result = LMDB.mdb_reader_check(ptr, deadPtr)
            if (result != 0) {
                LMDB.free(deadPtr)
                throw LmdbException(native_mdb_strerror(result))
            }
            val dead = LMDB.getValue(deadPtr, TYPE_I32)
            LMDB.free(deadPtr)
            return dead.toUInt()
        }

    actual var flags: Set<EnvOption> = emptySet()
        get() {
            val flagsPtr = LMDB.malloc(POINTER_SIZE)
            val result = LMDB.mdb_env_get_flags(ptr, flagsPtr)
            if (result != 0) {
                LMDB.free(flagsPtr)
                throw LmdbException(native_mdb_strerror(result))
            }
            val flagsValue = LMDB.getValue(flagsPtr, TYPE_I32)
            LMDB.free(flagsPtr)
            return EnvOption.entries.filter { flagsValue and it.option.toInt() == it.option.toInt() }.toSet()
        }
        set(value) {
            // Get current flags first
            val currentFlags = this.flags

            // Clear flags that are in current but not in new value
            val flagsToClear = currentFlags.minus(value)
            flagsToClear.forEach { flag ->
                val result = LMDB.mdb_env_set_flags(ptr, flag.option.toInt(), 0)
                if (result != 0) {
                    throw LmdbException(native_mdb_strerror(result))
                }
            }

            // Set flags that are in new value but not in current
            val flagsToSet = value.minus(currentFlags)
            flagsToSet.forEach { flag ->
                val result = LMDB.mdb_env_set_flags(ptr, flag.option.toInt(), 1)
                if (result != 0) {
                    throw LmdbException(native_mdb_strerror(result))
                }
            }

            field = value
        }

    actual val stat: Stat?
        get() {
            if (!isOpened) return null

            // Allocate space for MDB_stat structure
            val statPtr = LMDB.malloc(MDB_STAT_SIZE)

            val result = LMDB.mdb_env_stat(ptr, statPtr)
            if (result != 0) {
                LMDB.free(statPtr)
                throw LmdbException(native_mdb_strerror(result))
            }

            // Extract fields from the structure
            val psize = LMDB.getValue(statPtr, TYPE_I32)
            val depth = LMDB.getValue(statPtr + 4, TYPE_I32)
            val branchPages = LMDB.getValue(statPtr + 8, TYPE_I32)
            val leafPages = LMDB.getValue(statPtr + 12, TYPE_I32)
            val overflowPages = LMDB.getValue(statPtr + 16, TYPE_I32)
            val entries = LMDB.getValue(statPtr + 20, TYPE_I32)

            LMDB.free(statPtr)

            return Stat(
                branchPages.toULong(),
                depth.toUInt(),
                entries.toULong(),
                leafPages.toULong(),
                overflowPages.toULong(),
                psize.toUInt()
            )
        }

    actual val info: EnvInfo?
        get() {
            if (!isOpened) return null

            // Allocate space for MDB_envinfo structure
            val infoPtr = LMDB.malloc(MDB_ENVINFO_SIZE)

            val result = LMDB.mdb_env_info(ptr, infoPtr)
            if (result != 0) {
                LMDB.free(infoPtr)
                throw LmdbException(native_mdb_strerror(result))
            }

            // Extract fields from the structure - note that we're using Int for all values
            // In a real implementation, we would need to handle 64-bit values properly
            val mapAddr = LMDB.getValue(infoPtr, TYPE_I32)
            val mapSize = LMDB.getValue(infoPtr + 4, TYPE_I32)
            val lastPgno = LMDB.getValue(infoPtr + 8, TYPE_I32)
            val lastTxnId = LMDB.getValue(infoPtr + 12, TYPE_I32)
            val maxReaders = LMDB.getValue(infoPtr + 16, TYPE_I32)
            val numReaders = LMDB.getValue(infoPtr + 20, TYPE_I32)

            LMDB.free(infoPtr)

            return EnvInfo(
                lastPgno.toLong().toULong(),
                lastTxnId.toLong().toULong(),
                mapAddr.toLong().toULong(),
                mapSize.toLong().toULong(),
                maxReaders.toUInt(),
                numReaders.toUInt()
            )
        }

    actual fun open(path: String, vararg options: EnvOption, mode: String) {
        // In WASM environments, Emscripten's mmap doesn't provide true MAP_SHARED semantics, so we use
        // MDB_WRITEMAP to work around mmap limitations.
        val wasmOptions = options.toMutableList()
        if (!wasmOptions.contains(EnvOption.WriteMap)) {
            wasmOptions.add(EnvOption.WriteMap)
        }
        
        val flags = wasmOptions.toFlags().toInt()
        val modeInt = mode.toInt(8)

        val result = LMDB.mdb_env_open(ptr, path, flags, modeInt)
        if (result != 0) {
            throw LmdbException(native_mdb_strerror(result))
        }

        isOpened = true
    }

    actual fun beginTxn(vararg options: TxnOption): Txn {
        if (!isOpened) {
            throw LmdbException(ERROR_ENV_NOT_OPENED)
        }
        return Txn(this, *options)
    }

    actual fun copyTo(path: String, compact: Boolean) {
        val flags = if (compact) MDB_COMPACT_FLAG else 0

        val result = LMDB.mdb_env_copy2(ptr, path, flags)
        if (result != 0) {
            throw LmdbException(native_mdb_strerror(result))
        }
    }

    actual fun copyTo(path: String) {
        val result = LMDB.mdb_env_copy(ptr, path)
        if (result != 0) {
            throw LmdbException(native_mdb_strerror(result))
        }
    }

    actual fun sync(force: Boolean) {
        val forceInt = if (force) 1 else 0

        val result = LMDB.mdb_env_sync(ptr, forceInt)
        if (result != 0) {
            throw LmdbException(native_mdb_strerror(result))
        }
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
