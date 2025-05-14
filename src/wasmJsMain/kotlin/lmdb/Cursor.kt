package lmdb

import lmdb.Constants.MDB_NODUPDATA
import lmdb.Constants.MDB_VAL_DATA_OFFSET
import lmdb.Constants.MDB_VAL_SIZE
import lmdb.Constants.MDB_VAL_SIZE_OFFSET
import lmdb.Constants.POINTER_SIZE
import lmdb.Constants.SUCCESS_CODE
import lmdb.Constants.TYPE_I32
import lmdb.Constants.TYPE_I8

/**
 * Implementation of the Cursor class for the wasmJs target.
 * This implementation uses JavaScript interop to interact with the LMDB WASM binary.
 */
actual class Cursor : AutoCloseable {
    // The cursor pointer (would be a pointer to MDB_cursor in C)
    private var ptr: Int = 0

    // Reference to the transaction to prevent garbage collection
    private var txn: Txn

    internal constructor(txn: Txn, dbi: Dbi) {
        this.txn = txn
        
        // Open a cursor
        val cursorPtr = LMDB.malloc(POINTER_SIZE)
        
        val result = LMDB.mdb_cursor_open(txn.ptr, dbi.ptr, cursorPtr)
        if (result != 0) {
            LMDB.free(cursorPtr)
            throw LmdbException(native_mdb_strerror(result))
        }
        
        ptr = LMDB.getValue(cursorPtr, TYPE_I32)
        LMDB.free(cursorPtr)
    }

    actual internal fun get(option: CursorOption): ValResult {
        // Create MDB_val for key (output)
        val keyPtr = LMDB.malloc(MDB_VAL_SIZE)
        
        // Create MDB_val for data (output)
        val dataPtr = LMDB.malloc(MDB_VAL_SIZE)
        
        // Call mdb_cursor_get
        val result = LMDB.mdb_cursor_get(ptr, keyPtr, dataPtr, option.option.toInt())
        
        // Process result
        if (result == SUCCESS_CODE) {
            // Get key from MDB_val
            val keySize = LMDB.getValue(keyPtr + MDB_VAL_SIZE_OFFSET, TYPE_I32)
            val keyDataPtr = LMDB.getValue(keyPtr + MDB_VAL_DATA_OFFSET, TYPE_I32)
            
            // Copy key from WASM memory
            val keyBytes = ByteArray(keySize)
            for (i in 0 until keySize) {
                keyBytes[i] = LMDB.getValue(keyDataPtr + i, TYPE_I8).toByte()
            }
            
            // Get data from MDB_val
            val dataSize = LMDB.getValue(dataPtr + MDB_VAL_SIZE_OFFSET, TYPE_I32)
            val dataDataPtr = LMDB.getValue(dataPtr + MDB_VAL_DATA_OFFSET, TYPE_I32)
            
            // Copy data from WASM memory
            val dataBytes = ByteArray(dataSize)
            for (i in 0 until dataSize) {
                dataBytes[i] = LMDB.getValue(dataDataPtr + i, TYPE_I8).toByte()
            }
            
            // Clean up
            LMDB.free(keyPtr)
            LMDB.free(dataPtr)
            
            return ValResult(0, keyBytes.toVal(), dataBytes.toVal())
        } else {
            // Clean up
            LMDB.free(keyPtr)
            LMDB.free(dataPtr)
            
            return ValResult(result, ByteArray(0).toVal(), ByteArray(0).toVal())
        }
    }

    actual internal fun get(key: Val, option: CursorOption): ValResult {
        // Create MDB_val for key
        val keyPtr = LMDB.malloc(MDB_VAL_SIZE)
        val keyData = key.toByteArray() ?: ByteArray(0)
        val keyDataPtr = if (keyData.isNotEmpty()) LMDB.malloc(keyData.size) else 0
        // Copy key data to WASM memory
        for (i in keyData.indices) {
            LMDB.setValue(keyDataPtr + i, keyData[i].toInt(), TYPE_I8)
        }
        LMDB.setValue(keyPtr + MDB_VAL_SIZE_OFFSET, keyData.size, TYPE_I32)
        LMDB.setValue(keyPtr + MDB_VAL_DATA_OFFSET, keyDataPtr, TYPE_I32)
        
        // Create MDB_val for data (output)
        val dataPtr = LMDB.malloc(MDB_VAL_SIZE)
        
        // Call mdb_cursor_get
        val result = LMDB.mdb_cursor_get(ptr, keyPtr, dataPtr, option.option.toInt())
        
        // Process result
        if (result == SUCCESS_CODE) {
            // Get key from MDB_val (may have been updated)
            val keySize = LMDB.getValue(keyPtr, "i32")
            val keyDataPtrOut = LMDB.getValue(keyPtr + MDB_VAL_DATA_OFFSET, TYPE_I32)
            
            // Copy key from WASM memory
            val keyBytes = ByteArray(keySize)
            for (i in 0 until keySize) {
                keyBytes[i] = LMDB.getValue(keyDataPtrOut + i, "i8").toByte()
            }
            
            // Get data from MDB_val
            val dataSize = LMDB.getValue(dataPtr + MDB_VAL_SIZE_OFFSET, TYPE_I32)
            val dataDataPtr = LMDB.getValue(dataPtr + MDB_VAL_DATA_OFFSET, TYPE_I32)
            
            // Copy data from WASM memory
            val dataBytes = ByteArray(dataSize)
            for (i in 0 until dataSize) {
                dataBytes[i] = LMDB.getValue(dataDataPtr + i, TYPE_I8).toByte()
            }
            
            // Clean up
            LMDB.free(keyPtr)
            if (keyDataPtr != 0) LMDB.free(keyDataPtr)
            LMDB.free(dataPtr)
            
            return ValResult(0, keyBytes.toVal(), dataBytes.toVal())
        } else {
            // Clean up
            LMDB.free(keyPtr)
            if (keyDataPtr != 0) LMDB.free(keyDataPtr)
            LMDB.free(dataPtr)
            
            return ValResult(result, key, ByteArray(0).toVal())
        }
    }

    actual internal fun get(key: Val, data: Val, option: CursorOption): ValResult {
        // Create MDB_val for key
        val keyPtr = LMDB.malloc(MDB_VAL_SIZE)
        val keyData = key.toByteArray() ?: ByteArray(0)
        val keyDataPtr = if (keyData.isNotEmpty()) LMDB.malloc(keyData.size) else 0
        // Copy key data to WASM memory
        for (i in keyData.indices) {
            LMDB.setValue(keyDataPtr + i, keyData[i].toInt(), TYPE_I8)
        }
        LMDB.setValue(keyPtr + MDB_VAL_SIZE_OFFSET, keyData.size, TYPE_I32)
        LMDB.setValue(keyPtr + MDB_VAL_DATA_OFFSET, keyDataPtr, TYPE_I32)
        
        // Create MDB_val for data
        val dataPtr = LMDB.malloc(MDB_VAL_SIZE)
        val dataData = data.toByteArray() ?: ByteArray(0)
        val dataDataPtr = if (dataData.isNotEmpty()) LMDB.malloc(dataData.size) else 0
        // Copy data data to WASM memory
        for (i in dataData.indices) {
            LMDB.setValue(dataDataPtr + i, dataData[i].toInt(), TYPE_I8)
        }
        LMDB.setValue(dataPtr + MDB_VAL_SIZE_OFFSET, dataData.size, TYPE_I32)
        LMDB.setValue(dataPtr + MDB_VAL_DATA_OFFSET, dataDataPtr, TYPE_I32)
        
        // Call mdb_cursor_get
        val result = LMDB.mdb_cursor_get(ptr, keyPtr, dataPtr, option.option.toInt())
        
        // Process result
        if (result == SUCCESS_CODE) {
            // Get key from MDB_val (may have been updated)
            val keySize = LMDB.getValue(keyPtr, "i32")
            val keyDataPtrOut = LMDB.getValue(keyPtr + MDB_VAL_DATA_OFFSET, TYPE_I32)
            
            // Copy key from WASM memory
            val keyBytes = ByteArray(keySize)
            for (i in 0 until keySize) {
                keyBytes[i] = LMDB.getValue(keyDataPtrOut + i, "i8").toByte()
            }
            
            // Get data from MDB_val (may have been updated)
            val dataSize = LMDB.getValue(dataPtr, "i32")
            val dataDataPtrOut = LMDB.getValue(dataPtr + MDB_VAL_DATA_OFFSET, TYPE_I32)
            
            // Copy data from WASM memory
            val dataBytes = ByteArray(dataSize)
            for (i in 0 until dataSize) {
                dataBytes[i] = LMDB.getValue(dataDataPtrOut + i, TYPE_I8).toByte()
            }
            
            // Clean up
            LMDB.free(keyPtr)
            if (keyDataPtr != 0) LMDB.free(keyDataPtr)
            LMDB.free(dataPtr)
            if (dataDataPtr != 0) LMDB.free(dataDataPtr)
            
            return ValResult(0, keyBytes.toVal(), dataBytes.toVal())
        } else {
            // Clean up
            LMDB.free(keyPtr)
            if (keyDataPtr != 0) LMDB.free(keyDataPtr)
            LMDB.free(dataPtr)
            if (dataDataPtr != 0) LMDB.free(dataDataPtr)
            
            return ValResult(result, key, data)
        }
    }

    actual internal fun put(key: Val, data: Val, option: CursorPutOption): ValResult {
        // Create MDB_val for key
        val keyPtr = LMDB.malloc(MDB_VAL_SIZE)
        val keyData = key.toByteArray() ?: ByteArray(0)
        val keyDataPtr = if (keyData.isNotEmpty()) LMDB.malloc(keyData.size) else 0
        // Copy key data to WASM memory
        for (i in keyData.indices) {
            LMDB.setValue(keyDataPtr + i, keyData[i].toInt(), TYPE_I8)
        }
        LMDB.setValue(keyPtr + MDB_VAL_SIZE_OFFSET, keyData.size, TYPE_I32)
        LMDB.setValue(keyPtr + MDB_VAL_DATA_OFFSET, keyDataPtr, TYPE_I32)
        
        // Create MDB_val for data
        val dataPtr = LMDB.malloc(MDB_VAL_SIZE)
        val dataData = data.toByteArray() ?: ByteArray(0)
        val dataDataPtr = if (dataData.isNotEmpty()) LMDB.malloc(dataData.size) else 0
        // Copy data data to WASM memory
        for (i in dataData.indices) {
            LMDB.setValue(dataDataPtr + i, dataData[i].toInt(), TYPE_I8)
        }
        LMDB.setValue(dataPtr + MDB_VAL_SIZE_OFFSET, dataData.size, TYPE_I32)
        LMDB.setValue(dataPtr + MDB_VAL_DATA_OFFSET, dataDataPtr, TYPE_I32)
        
        // Call mdb_cursor_put
        val result = LMDB.mdb_cursor_put(ptr, keyPtr, dataPtr, option.option.toInt())
        
        // Clean up
        LMDB.free(keyPtr)
        if (keyDataPtr != 0) LMDB.free(keyDataPtr)
        LMDB.free(dataPtr)
        if (dataDataPtr != 0) LMDB.free(dataDataPtr)
        
        // Use buildResult to throw exception on errors
        return buildResult(result, key, data)
    }

    actual fun delete() {
        val result = LMDB.mdb_cursor_del(ptr, 0)
        if (result != 0) {
            throw LmdbException(native_mdb_strerror(result))
        }
    }

    actual fun deleteDuplicateData() {
        val result = LMDB.mdb_cursor_del(ptr, MDB_NODUPDATA)
        if (result != 0) {
            throw LmdbException(native_mdb_strerror(result))
        }
    }

    actual fun countDuplicates(): UInt {
        val countPtr = LMDB.malloc(POINTER_SIZE)
        
        val result = LMDB.mdb_cursor_count(ptr, countPtr)
        if (result != 0) {
            LMDB.free(countPtr)
            throw LmdbException(native_mdb_strerror(result))
        }
        
        val count = LMDB.getValue(countPtr, TYPE_I32)
        LMDB.free(countPtr)
        
        return count.toUInt()
    }

    actual fun renew(txn: Txn) {
        val result = LMDB.mdb_cursor_renew(txn.ptr, ptr)
        if (result != 0) {
            throw LmdbException(native_mdb_strerror(result))
        }
        
        this.txn = txn
    }

    actual override fun close() {
        LMDB.mdb_cursor_close(ptr)
    }
}