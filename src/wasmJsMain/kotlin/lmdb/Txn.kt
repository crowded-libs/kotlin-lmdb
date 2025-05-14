package lmdb

import lmdb.Constants.ERROR_DUP_COMPARERS_NOT_IMPLEMENTED
import lmdb.Constants.ERROR_KEY_COMPARERS_NOT_IMPLEMENTED
import lmdb.Constants.ERROR_TXN_NOT_READY
import lmdb.Constants.MDB_VAL_DATA_OFFSET
import lmdb.Constants.MDB_VAL_SIZE
import lmdb.Constants.MDB_VAL_SIZE_OFFSET
import lmdb.Constants.POINTER_SIZE
import lmdb.Constants.SUCCESS_CODE
import lmdb.Constants.TYPE_I32
import lmdb.Constants.TYPE_I8

/**
 * Implementation of the Txn class for the wasmJs target.
 * This implementation uses JavaScript interop to interact with the LMDB WASM binary.
 */
actual class Txn : AutoCloseable {
    // The transaction pointer (would be a pointer to MDB_txn in C)
    internal var ptr: Int = 0

    // Reference to the environment
    internal var env: Env

    // Reference to the parent transaction (if any)
    private var parent: Txn? = null

    actual var state: TxnState = TxnState.Ready
        internal set

    actual val id: ULong
        get() {
            if (state != TxnState.Ready) {
                throw LmdbException(ERROR_TXN_NOT_READY)
            }
            return LMDB.mdb_txn_id(ptr).toULong()
        }

    internal actual constructor(env: Env, vararg options: TxnOption) {
        this.env = env
        
        // Begin a transaction
        val flags = options.asIterable().toFlags().toInt()
        val txnPtrPtr = LMDB.malloc(POINTER_SIZE)
        
        val result = LMDB.mdb_txn_begin(env.ptr, 0, flags, txnPtrPtr)
        if (result != 0) {
            LMDB.free(txnPtrPtr)
            throw LmdbException(native_mdb_strerror(result))
        }
        
        ptr = LMDB.getValue(txnPtrPtr, TYPE_I32)
        LMDB.free(txnPtrPtr)
    }

    internal actual constructor(env: Env, parent: Txn?, vararg options: TxnOption) {
        this.env = env
        this.parent = parent
        
        // Begin a transaction with a parent
        val parentTxnPtr = parent?.ptr ?: 0
        val flags = options.asIterable().toFlags().toInt()
        val txnPtrPtr = LMDB.malloc(POINTER_SIZE)
        
        val result = LMDB.mdb_txn_begin(env.ptr, parentTxnPtr, flags, txnPtrPtr)
        if (result != 0) {
            LMDB.free(txnPtrPtr)
            throw LmdbException(native_mdb_strerror(result))
        }
        
        ptr = LMDB.getValue(txnPtrPtr, TYPE_I32)
        LMDB.free(txnPtrPtr)
    }

    actual fun begin(vararg options: TxnOption): Txn {
        if (state != TxnState.Ready) {
            throw LmdbException(ERROR_TXN_NOT_READY)
        }

        return Txn(env, this, *options)
    }

    actual fun abort() {
        if (state != TxnState.Ready) {
            return
        }
        
        LMDB.mdb_txn_abort(ptr)
        state = TxnState.Done
    }

    actual fun reset() {
        if (state != TxnState.Ready) {
            return
        }
        
        LMDB.mdb_txn_reset(ptr)
        state = TxnState.Reset
    }

    actual fun renew() {
        if (state != TxnState.Reset) {
            return
        }
        
        val result = LMDB.mdb_txn_renew(ptr)
        if (result != 0) {
            throw LmdbException(native_mdb_strerror(result))
        }
        
        state = TxnState.Ready
    }

    actual fun commit() {
        if (state != TxnState.Ready) {
            return
        }
        
        val result = LMDB.mdb_txn_commit(ptr)
        if (result != 0) {
            throw LmdbException(native_mdb_strerror(result))
        }
        
        state = TxnState.Done
    }

    actual fun dbiOpen(name: String?, vararg options: DbiOption): Dbi {
        if (state != TxnState.Ready) {
            throw LmdbException(ERROR_TXN_NOT_READY)
        }
        
        return Dbi(name, this, *options)
    }

    actual fun dbiOpen(name: String?, config: DbiConfig, vararg options: DbiOption): Dbi {
        if (state != TxnState.Ready) {
            throw LmdbException(ERROR_TXN_NOT_READY)
        }
        
        val dbi = Dbi(name, this, *options)
        
        // Set key comparer if provided
        config.keyComparer?.let { comparer ->
            // For now, throw an exception to indicate this is not yet implemented
            throw LmdbException(ERROR_KEY_COMPARERS_NOT_IMPLEMENTED)
        }
        
        // Set duplicate data comparer if provided  
        config.dupComparer?.let { comparer ->
            // For now, throw an exception to indicate this is not yet implemented
            throw LmdbException(ERROR_DUP_COMPARERS_NOT_IMPLEMENTED)
        }
        
        return dbi
    }

    actual fun drop(dbi: Dbi) {
        if (state != TxnState.Ready) {
            throw LmdbException(ERROR_TXN_NOT_READY)
        }
        
        val result = LMDB.mdb_drop(ptr, dbi.ptr, 1)
        if (result != 0) {
            throw LmdbException(native_mdb_strerror(result))
        }
    }

    actual fun empty(dbi: Dbi) {
        if (state != TxnState.Ready) {
            throw LmdbException(ERROR_TXN_NOT_READY)
        }
        
        val result = LMDB.mdb_drop(ptr, dbi.ptr, 0)
        if (result != 0) {
            throw LmdbException(native_mdb_strerror(result))
        }
    }

    actual fun get(dbi: Dbi, key: Val): ValResult {
        if (state != TxnState.Ready) {
            throw LmdbException(ERROR_TXN_NOT_READY)
        }
        
        // Create MDB_val for key
        val keyPtr = LMDB.malloc(MDB_VAL_SIZE)
        val keyData = key.toByteArray() ?: ByteArray(0)
        val keyDataPtr = if (keyData.isNotEmpty()) LMDB.malloc(keyData.size) else 0
        // Copy key data to WASM memory
        for (i in keyData.indices) {
            LMDB.setValue(keyDataPtr + i, keyData[i].toInt() and 0xFF, TYPE_I8)
        }
        LMDB.setValue(keyPtr + MDB_VAL_SIZE_OFFSET, keyData.size, TYPE_I32)
        LMDB.setValue(keyPtr + MDB_VAL_DATA_OFFSET, keyDataPtr, TYPE_I32)
        
        // Create MDB_val for data (output) - must be zero-initialized
        val dataPtr = LMDB.malloc(MDB_VAL_SIZE)
        LMDB.setValue(dataPtr + MDB_VAL_SIZE_OFFSET, 0, TYPE_I32)
        LMDB.setValue(dataPtr + MDB_VAL_DATA_OFFSET, 0, TYPE_I32)
        
        // Call mdb_get
        val result = LMDB.mdb_get(ptr, dbi.ptr, keyPtr, dataPtr)
        
        // Process result
        if (result == SUCCESS_CODE) {
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
            
            return ValResult(0, key, dataBytes.toVal())
        } else {
            // Clean up
            LMDB.free(keyPtr)
            if (keyDataPtr != 0) LMDB.free(keyDataPtr)
            LMDB.free(dataPtr)
            
            return ValResult(result, key, ByteArray(0).toVal())
        }
    }

    actual fun put(dbi: Dbi, key: Val, data: Val, vararg options: PutOption) {
        if (state != TxnState.Ready) {
            throw LmdbException(ERROR_TXN_NOT_READY)
        }
        
        val flags = options.asIterable().toFlags().toInt()
        
        // Create MDB_val for key
        val keyPtr = LMDB.malloc(MDB_VAL_SIZE)
        val keyData = key.toByteArray() ?: ByteArray(0)
        val keyDataPtr = if (keyData.isNotEmpty()) LMDB.malloc(keyData.size) else 0
        // Copy key data to WASM memory
        for (i in keyData.indices) {
            LMDB.setValue(keyDataPtr + i, keyData[i].toInt() and 0xFF, TYPE_I8)
        }
        LMDB.setValue(keyPtr + MDB_VAL_SIZE_OFFSET, keyData.size, TYPE_I32)
        LMDB.setValue(keyPtr + MDB_VAL_DATA_OFFSET, keyDataPtr, TYPE_I32)
        
        // Create MDB_val for data
        val dataPtr = LMDB.malloc(MDB_VAL_SIZE)
        val dataData = data.toByteArray() ?: ByteArray(0)
        val dataDataPtr = if (dataData.isNotEmpty()) LMDB.malloc(dataData.size) else 0
        // Copy data data to WASM memory
        for (i in dataData.indices) {
            LMDB.setValue(dataDataPtr + i, dataData[i].toInt() and 0xFF, TYPE_I8)
        }
        LMDB.setValue(dataPtr + MDB_VAL_SIZE_OFFSET, dataData.size, TYPE_I32)
        LMDB.setValue(dataPtr + MDB_VAL_DATA_OFFSET, dataDataPtr, TYPE_I32)
        
        // Call mdb_put
        val result = LMDB.mdb_put(ptr, dbi.ptr, keyPtr, dataPtr, flags)
        
        // Clean up
        LMDB.free(keyPtr)
        if (keyDataPtr != 0) LMDB.free(keyDataPtr)
        LMDB.free(dataPtr)
        if (dataDataPtr != 0) LMDB.free(dataDataPtr)
        
        if (result != 0) {
            throw LmdbException(native_mdb_strerror(result))
        }
    }

    actual fun delete(dbi: Dbi, key: Val) {
        if (state != TxnState.Ready) {
            throw LmdbException(ERROR_TXN_NOT_READY)
        }
        
        // Create MDB_val for key
        val keyPtr = LMDB.malloc(MDB_VAL_SIZE)
        val keyData = key.toByteArray() ?: ByteArray(0)
        val keyDataPtr = if (keyData.isNotEmpty()) LMDB.malloc(keyData.size) else 0
        // Copy key data to WASM memory
        for (i in keyData.indices) {
            LMDB.setValue(keyDataPtr + i, keyData[i].toInt() and 0xFF, TYPE_I8)
        }
        LMDB.setValue(keyPtr + MDB_VAL_SIZE_OFFSET, keyData.size, TYPE_I32)
        LMDB.setValue(keyPtr + MDB_VAL_DATA_OFFSET, keyDataPtr, TYPE_I32)
        
        // Call mdb_del with null data
        val result = LMDB.mdb_del(ptr, dbi.ptr, keyPtr, 0)

        // Clean up
        LMDB.free(keyPtr)
        if (keyDataPtr != 0) LMDB.free(keyDataPtr)
        
        if (result != 0) {
            throw LmdbException(native_mdb_strerror(result))
        }
    }

    actual fun delete(dbi: Dbi, key: Val, data: Val) {
        if (state != TxnState.Ready) {
            throw LmdbException(ERROR_TXN_NOT_READY)
        }
        
        // Create MDB_val for key
        val keyPtr = LMDB.malloc(MDB_VAL_SIZE)
        val keyData = key.toByteArray() ?: ByteArray(0)
        val keyDataPtr = if (keyData.isNotEmpty()) LMDB.malloc(keyData.size) else 0
        // Copy key data to WASM memory
        for (i in keyData.indices) {
            LMDB.setValue(keyDataPtr + i, keyData[i].toInt() and 0xFF, TYPE_I8)
        }
        LMDB.setValue(keyPtr + MDB_VAL_SIZE_OFFSET, keyData.size, TYPE_I32)
        LMDB.setValue(keyPtr + MDB_VAL_DATA_OFFSET, keyDataPtr, TYPE_I32)
        
        // Create MDB_val for data
        val dataPtr = LMDB.malloc(MDB_VAL_SIZE)
        val dataData = data.toByteArray() ?: ByteArray(0)
        val dataDataPtr = if (dataData.isNotEmpty()) LMDB.malloc(dataData.size) else 0
        // Copy data data to WASM memory
        for (i in dataData.indices) {
            LMDB.setValue(dataDataPtr + i, dataData[i].toInt() and 0xFF, TYPE_I8)
        }
        LMDB.setValue(dataPtr + MDB_VAL_SIZE_OFFSET, dataData.size, TYPE_I32)
        LMDB.setValue(dataPtr + MDB_VAL_DATA_OFFSET, dataDataPtr, TYPE_I32)
        
        // Call mdb_del
        val result = LMDB.mdb_del(ptr, dbi.ptr, keyPtr, dataPtr)
        
        // Clean up
        LMDB.free(keyPtr)
        if (keyDataPtr != 0) LMDB.free(keyDataPtr)
        LMDB.free(dataPtr)
        if (dataDataPtr != 0) LMDB.free(dataDataPtr)
        
        if (result != 0) {
            throw LmdbException(native_mdb_strerror(result))
        }
    }

    actual fun openCursor(dbi: Dbi): Cursor {
        if (state != TxnState.Ready) {
            throw LmdbException(ERROR_TXN_NOT_READY)
        }
        
        return Cursor(this, dbi)
    }

    actual override fun close() {
        if (state == TxnState.Done) {
            return
        }
        
        LMDB.mdb_txn_abort(ptr)
        state = TxnState.Done
    }
}