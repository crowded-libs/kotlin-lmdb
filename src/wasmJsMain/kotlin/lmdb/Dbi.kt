package lmdb

import lmdb.Constants.MDB_VAL_DATA_OFFSET
import lmdb.Constants.MDB_VAL_SIZE
import lmdb.Constants.MDB_VAL_SIZE_OFFSET
import lmdb.Constants.MDB_STAT_SIZE
import lmdb.Constants.POINTER_SIZE
import lmdb.Constants.TYPE_I32
import lmdb.Constants.TYPE_I8

/**
 * Implementation of the Dbi class for the wasmJs target.
 * This implementation uses JavaScript interop to interact with the LMDB WASM binary.
 */
actual class Dbi actual constructor(
    name: String?,
    val tx: Txn,
    vararg options: DbiOption
) : AutoCloseable {
    // The database handle (would be an unsigned int in C)
    internal var ptr: Int = 0

    init {
        // Open the database once and store the handle
        var flags = options.asIterable().toFlags().toInt()
        
        val dbiPtr = LMDB.malloc(POINTER_SIZE)
        
        val result = LMDB.mdb_dbi_open(tx.ptr, name, flags, dbiPtr)
        if (result != 0) {
            LMDB.free(dbiPtr)
            throw LmdbException(native_mdb_strerror(result))
        }
        
        ptr = LMDB.getValue(dbiPtr, TYPE_I32)
        LMDB.free(dbiPtr)
    }

    actual fun stat(tx: Txn): Stat {
        // Allocate space for MDB_stat structure
        val statPtr = LMDB.malloc(MDB_STAT_SIZE)
        
        val result = LMDB.mdb_stat(tx.ptr, ptr, statPtr)
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

    actual fun compare(tx: Txn, a: Val, b: Val): Int {
        // Create MDB_val for a
        val aPtr = LMDB.malloc(MDB_VAL_SIZE)
        val aData = a.toByteArray() ?: ByteArray(0)
        val aDataPtr = if (aData.isNotEmpty()) LMDB.malloc(aData.size) else 0
        // Copy a data to WASM memory
        for (i in aData.indices) {
            LMDB.setValue(aDataPtr + i, aData[i].toInt(), TYPE_I8)
        }
        LMDB.setValue(aPtr + MDB_VAL_SIZE_OFFSET, aData.size, TYPE_I32)
        LMDB.setValue(aPtr + MDB_VAL_DATA_OFFSET, aDataPtr, TYPE_I32)
        
        // Create MDB_val for b
        val bPtr = LMDB.malloc(MDB_VAL_SIZE)
        val bData = b.toByteArray() ?: ByteArray(0)
        val bDataPtr = if (bData.isNotEmpty()) LMDB.malloc(bData.size) else 0
        // Copy b data to WASM memory
        for (i in bData.indices) {
            LMDB.setValue(bDataPtr + i, bData[i].toInt(), TYPE_I8)
        }
        LMDB.setValue(bPtr + MDB_VAL_SIZE_OFFSET, bData.size, TYPE_I32)
        LMDB.setValue(bPtr + MDB_VAL_DATA_OFFSET, bDataPtr, TYPE_I32)
        
        // Call mdb_cmp
        val result = LMDB.mdb_cmp(tx.ptr, ptr, aPtr, bPtr)
        
        // Clean up
        LMDB.free(aPtr)
        if (aDataPtr != 0) LMDB.free(aDataPtr)
        LMDB.free(bPtr)
        if (bDataPtr != 0) LMDB.free(bDataPtr)
        
        return result
    }

    actual fun dupCompare(tx: Txn, a: Val, b: Val): Int {
        // Create MDB_val for a
        val aPtr = LMDB.malloc(MDB_VAL_SIZE)
        val aData = a.toByteArray() ?: ByteArray(0)
        val aDataPtr = if (aData.isNotEmpty()) LMDB.malloc(aData.size) else 0
        // Copy a data to WASM memory
        for (i in aData.indices) {
            LMDB.setValue(aDataPtr + i, aData[i].toInt(), TYPE_I8)
        }
        LMDB.setValue(aPtr + MDB_VAL_SIZE_OFFSET, aData.size, TYPE_I32)
        LMDB.setValue(aPtr + MDB_VAL_DATA_OFFSET, aDataPtr, TYPE_I32)
        
        // Create MDB_val for b
        val bPtr = LMDB.malloc(MDB_VAL_SIZE)
        val bData = b.toByteArray() ?: ByteArray(0)
        val bDataPtr = if (bData.isNotEmpty()) LMDB.malloc(bData.size) else 0
        // Copy b data to WASM memory
        for (i in bData.indices) {
            LMDB.setValue(bDataPtr + i, bData[i].toInt(), TYPE_I8)
        }
        LMDB.setValue(bPtr + MDB_VAL_SIZE_OFFSET, bData.size, TYPE_I32)
        LMDB.setValue(bPtr + MDB_VAL_DATA_OFFSET, bDataPtr, TYPE_I32)
        
        // Call mdb_dcmp
        val result = LMDB.mdb_dcmp(tx.ptr, ptr, aPtr, bPtr)
        
        // Clean up
        LMDB.free(aPtr)
        if (aDataPtr != 0) LMDB.free(aDataPtr)
        LMDB.free(bPtr)
        if (bDataPtr != 0) LMDB.free(bDataPtr)
        
        return result
    }

    actual fun flags(tx: Txn): Set<DbiOption> {
        val flagsPtr = LMDB.malloc(POINTER_SIZE)
        val result = LMDB.mdb_dbi_flags(tx.ptr, ptr, flagsPtr)
        if (result != 0) {
            LMDB.free(flagsPtr)
            throw LmdbException(native_mdb_strerror(result))
        }
        
        val flagsValue = LMDB.getValue(flagsPtr, TYPE_I32)
        LMDB.free(flagsPtr)
        
        return DbiOption.entries.filter { flagsValue and it.option.toInt() == it.option.toInt() }.toSet()
    }

    actual override fun close() {
        LMDB.mdb_dbi_close(tx.env.ptr, ptr)
    }
}