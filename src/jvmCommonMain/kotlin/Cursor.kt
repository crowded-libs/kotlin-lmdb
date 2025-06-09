package lmdb

import com.sun.jna.Pointer

actual class Cursor(txn: Txn, dbi: Dbi) : AutoCloseable {
    private val ptr: Pointer
    private var closed = false

    init {
        ptr = LmdbJna.mdb_cursor_open(txn.ptr, dbi.dbiHandle)
    }

    internal actual fun get(option: CursorOption): ValResult {
        val key = MDBVal.output()
        val data = MDBVal.output()
        val result = LmdbJna.mdb_cursor_get(ptr, key.buffer, data.buffer, option.option.toInt())
        return buildReadResult(result, Val.fromMDBVal(key), Val.fromMDBVal(data))
    }

    internal actual fun get(key: Val, option: CursorOption): ValResult {
        // For operations like SET_RANGE, LMDB may return a different key than the input
        val keyBuffer = if (option == CursorOption.SET_RANGE || option == CursorOption.SET_KEY) {
            // Use an output buffer that can hold the result key
            val outputKey = MDBVal.output().buffer
            val inputData = key.toByteArray()
            if (inputData != null) {
                outputKey.put(inputData)
                outputKey.flip()
            }
            outputKey
        } else {
            key.mdbVal.buffer
        }
        
        val data = MDBVal.output()
        val result = LmdbJna.mdb_cursor_get(ptr, keyBuffer, data.buffer, option.option.toInt())
        val resultKey = Val.fromMDBVal(MDBVal(keyBuffer))
        return buildReadResult(result, resultKey, Val.fromMDBVal(data))
    }

    internal actual fun get(key: Val, data: Val, option: CursorOption): ValResult {
        // For operations like MDB_GET_BOTH_RANGE, we need a larger buffer
        // because LMDB will return the found value which may be larger than the search prefix
        val dataBuffer = if (option == CursorOption.GET_BOTH_RANGE || option == CursorOption.GET_BOTH) {
            // Use an output buffer that can hold the result
            MDBVal.output().buffer
        } else {
            // For other operations, use the input data buffer
            data.mdbVal.buffer
        }
        
        // For MDB_GET_BOTH_RANGE, we need to copy the search prefix into the buffer
        if (option == CursorOption.GET_BOTH_RANGE || option == CursorOption.GET_BOTH) {
            val inputData = data.toByteArray()
            if (inputData != null) {
                dataBuffer.put(inputData)
                dataBuffer.flip()
            }
        }
        
        val result = LmdbJna.mdb_cursor_get(ptr, key.mdbVal.buffer, dataBuffer, option.option.toInt())
        
        // Create a new Val from the updated buffer
        val resultData = Val.fromMDBVal(MDBVal(dataBuffer))
        return buildReadResult(result, key, resultData)
    }

    actual fun delete() {
        check(LmdbJna.mdb_cursor_del(ptr, CursorDeleteOption.NONE.option.toInt()))
    }

    actual fun deleteDuplicateData() {
        check(LmdbJna.mdb_cursor_del(ptr, CursorDeleteOption.NO_DUP_DATA.option.toInt()))
    }

    actual fun countDuplicates(): UInt {
        val count = LmdbJna.mdb_cursor_count(ptr)
        if (count < 0) {
            throw LmdbException("Failed to count duplicates: ${LmdbJna.mdb_strerror(count.toInt())}")
        }
        return count.toUInt()
    }
    
    actual fun renew(txn: Txn) {
        check(LmdbJna.mdb_cursor_renew(txn.ptr, ptr))
    }

    internal actual fun put(key: Val, data: Val, option: CursorPutOption): ValResult {
        val result = LmdbJna.mdb_cursor_put(ptr, key.mdbVal.buffer, data.mdbVal.buffer, option.option.toInt())
        return buildResult(result, key, data)
    }

    actual override fun close() {
        if(closed)
            return

        LmdbJna.mdb_cursor_close(ptr)
        closed = true
    }
}