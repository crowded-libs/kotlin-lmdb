package lmdb

/**
 * Adapter object that provides the same API as the wasmWasi LMDB object
 * but uses our ES6 module implementation instead of @WasmImport.
 */
object LMDB {
    
    // Memory management
    fun malloc(size: Int): Int = WasmUtils.malloc(size)
    
    fun free(ptr: Int) = WasmUtils.free(ptr)
    
    fun setValue(ptr: Int, value: Int, type: String) = WasmUtils.setValue(ptr, value, type)
    
    fun getValue(ptr: Int, type: String): Int = WasmUtils.getValue(ptr, type)
    
    // String operations
    fun stringToPtr(str: String): Int = WasmUtils.stringToPtr(str)
    
    fun ptrToString(ptr: Int): String = WasmUtils.ptrToString(ptr)
    
    // Directory operations
    fun createDirectory(path: String): Boolean = WasmUtils.createDirectory(path)
    
    fun pathExists(path: String): Boolean = WasmUtils.pathExists(path)
    
    // Filesystem mounting operations
    fun mountIDBFS(path: String): Boolean = WasmUtils.mountIDBFS(path)
    
    fun mountNODEFS(path: String): Boolean = WasmUtils.mountNODEFS(path)
    
    fun mountBestFilesystem(path: String): Boolean = WasmUtils.mountBestFilesystem(path)
    
    // Error handling
    fun mdb_strerror(result: Int): String = WasmUtils.getErrorMessage(result)
    
    // Environment functions
    fun mdb_env_create(envPtrPtr: Int): Int = _mdb_env_create(envPtrPtr)
    
    fun mdb_env_open(envPtr: Int, path: String, flags: Int, mode: Int): Int {
        return WasmUtils.withStringPtr(path) { pathPtr ->
            _mdb_env_open(envPtr, pathPtr, flags, mode)
        }
    }
    
    fun mdb_env_close(envPtr: Int) = _mdb_env_close(envPtr)
    
    fun mdb_env_set_maxdbs(envPtr: Int, maxDbs: Int): Int = _mdb_env_set_maxdbs(envPtr, maxDbs)
    
    fun mdb_env_set_mapsize(envPtr: Int, size: Double): Int = _mdb_env_set_mapsize(envPtr, size)
    
    fun mdb_env_get_maxreaders(envPtr: Int, readersPtr: Int): Int = _mdb_env_get_maxreaders(envPtr, readersPtr)
    
    fun mdb_env_set_maxreaders(envPtr: Int, readers: Int): Int = _mdb_env_set_maxreaders(envPtr, readers)
    
    fun mdb_env_get_maxkeysize(envPtr: Int): Int = _mdb_env_get_maxkeysize(envPtr)
    
    fun mdb_reader_check(envPtr: Int, deadPtr: Int): Int = _mdb_reader_check(envPtr, deadPtr)
    
    fun mdb_env_get_flags(envPtr: Int, flagsPtr: Int): Int = _mdb_env_get_flags(envPtr, flagsPtr)
    
    fun mdb_env_set_flags(envPtr: Int, flags: Int, onoff: Int): Int = _mdb_env_set_flags(envPtr, flags, onoff)
    
    fun mdb_env_stat(envPtr: Int, statPtr: Int): Int = _mdb_env_stat(envPtr, statPtr)
    
    fun mdb_env_info(envPtr: Int, infoPtr: Int): Int = _mdb_env_info(envPtr, infoPtr)
    
    fun mdb_env_copy(envPtr: Int, path: String): Int {
        return WasmUtils.withStringPtr(path) { pathPtr ->
            _mdb_env_copy(envPtr, pathPtr)
        }
    }
    
    fun mdb_env_copy2(envPtr: Int, path: String, flags: Int): Int {
        return WasmUtils.withStringPtr(path) { pathPtr ->
            _mdb_env_copy2(envPtr, pathPtr, flags)
        }
    }
    
    fun mdb_env_sync(envPtr: Int, force: Int): Int = _mdb_env_sync(envPtr, force)
    
    // Transaction functions
    fun mdb_txn_begin(envPtr: Int, parentTxnPtr: Int, flags: Int, txnPtrPtr: Int): Int = 
        _mdb_txn_begin(envPtr, parentTxnPtr, flags, txnPtrPtr)
    
    fun mdb_txn_commit(txnPtr: Int): Int = _mdb_txn_commit(txnPtr)
    
    fun mdb_txn_abort(txnPtr: Int) = _mdb_txn_abort(txnPtr)
    
    fun mdb_txn_reset(txnPtr: Int) = _mdb_txn_reset(txnPtr)
    
    fun mdb_txn_renew(txnPtr: Int): Int = _mdb_txn_renew(txnPtr)
    
    fun mdb_txn_id(txnPtr: Int): Int = _mdb_txn_id(txnPtr)
    
    // Database functions
    fun mdb_dbi_open(txnPtr: Int, name: String?, flags: Int, dbiPtr: Int): Int {
        return if (name != null) {
            WasmUtils.withStringPtr(name) { namePtr ->
                _mdb_dbi_open(txnPtr, namePtr, flags, dbiPtr)
            }
        } else {
            _mdb_dbi_open(txnPtr, 0, flags, dbiPtr)
        }
    }
    
    fun mdb_dbi_close(envPtr: Int, dbi: Int) = _mdb_dbi_close(envPtr, dbi)
    
    fun mdb_drop(txnPtr: Int, dbi: Int, del: Int): Int = _mdb_drop(txnPtr, dbi, del)
    
    fun mdb_stat(txnPtr: Int, dbi: Int, statPtr: Int): Int = _mdb_stat(txnPtr, dbi, statPtr)
    
    fun mdb_dbi_flags(txnPtr: Int, dbi: Int, flagsPtr: Int): Int = _mdb_dbi_flags(txnPtr, dbi, flagsPtr)
    
    fun mdb_cmp(txnPtr: Int, dbi: Int, aPtr: Int, bPtr: Int): Int = _mdb_cmp(txnPtr, dbi, aPtr, bPtr)
    
    fun mdb_dcmp(txnPtr: Int, dbi: Int, aPtr: Int, bPtr: Int): Int = _mdb_dcmp(txnPtr, dbi, aPtr, bPtr)
    
    // Data operations
    fun mdb_get(txnPtr: Int, dbi: Int, keyPtr: Int, dataPtr: Int): Int = _mdb_get(txnPtr, dbi, keyPtr, dataPtr)
    
    fun mdb_put(txnPtr: Int, dbi: Int, keyPtr: Int, dataPtr: Int, flags: Int): Int = 
        _mdb_put(txnPtr, dbi, keyPtr, dataPtr, flags)
    
    fun mdb_del(txnPtr: Int, dbi: Int, keyPtr: Int, dataPtr: Int): Int = _mdb_del(txnPtr, dbi, keyPtr, dataPtr)
    
    // Cursor operations
    fun mdb_cursor_open(txnPtr: Int, dbi: Int, cursorPtrPtr: Int): Int = _mdb_cursor_open(txnPtr, dbi, cursorPtrPtr)
    
    fun mdb_cursor_close(cursorPtr: Int) = _mdb_cursor_close(cursorPtr)
    
    fun mdb_cursor_get(cursorPtr: Int, keyPtr: Int, dataPtr: Int, op: Int): Int = 
        _mdb_cursor_get(cursorPtr, keyPtr, dataPtr, op)
    
    fun mdb_cursor_put(cursorPtr: Int, keyPtr: Int, dataPtr: Int, flags: Int): Int = 
        _mdb_cursor_put(cursorPtr, keyPtr, dataPtr, flags)
    
    fun mdb_cursor_del(cursorPtr: Int, flags: Int): Int = _mdb_cursor_del(cursorPtr, flags)
    
    fun mdb_cursor_count(cursorPtr: Int, countPtr: Int): Int = _mdb_cursor_count(cursorPtr, countPtr)
    
    fun mdb_cursor_renew(txnPtr: Int, cursorPtr: Int): Int = _mdb_cursor_renew(txnPtr, cursorPtr)
    
    // Version information
    fun mdb_version(majorPtr: Int, minorPtr: Int, patchPtr: Int): Int = _mdb_version(majorPtr, minorPtr, patchPtr)
}