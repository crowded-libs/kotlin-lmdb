@file:JsModule("./lmdb-wrapper.mjs")

package lmdb

import kotlin.wasm.WasmImport

/**
 * LMDB WASM module functions using WasmImport for direct access.
 * Following the pattern from skiko project for reliable WASM module loading.
 */

// Memory management functions
@WasmImport("./lmdb-wrapper.mjs", "_malloc")
external fun _malloc(size: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_free")
external fun _free(ptr: Int)

// Memory utility functions (basic WASM operations only)
@WasmImport("./lmdb-wrapper.mjs", "getValue")
external fun _getValue(ptr: Int, type: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "setValue") 
external fun _setValue(ptr: Int, value: Int, type: Int)

// We'll handle string conversions at the adapter level using these basic operations

// Environment functions
@WasmImport("./lmdb-wrapper.mjs", "_mdb_env_create")
external fun _mdb_env_create(envPtrPtr: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_env_open")
external fun _mdb_env_open(envPtr: Int, path: Int, flags: Int, mode: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_env_close")
external fun _mdb_env_close(envPtr: Int)

@WasmImport("./lmdb-wrapper.mjs", "_mdb_env_set_maxdbs")
external fun _mdb_env_set_maxdbs(envPtr: Int, maxDbs: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_env_set_mapsize")
external fun _mdb_env_set_mapsize(envPtr: Int, size: Double): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_env_get_maxreaders")
external fun _mdb_env_get_maxreaders(envPtr: Int, readersPtr: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_env_set_maxreaders")
external fun _mdb_env_set_maxreaders(envPtr: Int, readers: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_env_get_maxkeysize")
external fun _mdb_env_get_maxkeysize(envPtr: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_reader_check")
external fun _mdb_reader_check(envPtr: Int, deadPtr: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_env_get_flags")
external fun _mdb_env_get_flags(envPtr: Int, flagsPtr: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_env_set_flags")
external fun _mdb_env_set_flags(envPtr: Int, flags: Int, onoff: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_env_stat")
external fun _mdb_env_stat(envPtr: Int, statPtr: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_env_info")
external fun _mdb_env_info(envPtr: Int, infoPtr: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_env_copy")
external fun _mdb_env_copy(envPtr: Int, path: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_env_copy2")
external fun _mdb_env_copy2(envPtr: Int, path: Int, flags: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_env_sync")
external fun _mdb_env_sync(envPtr: Int, force: Int): Int

// Transaction functions
@WasmImport("./lmdb-wrapper.mjs", "_mdb_txn_begin")
external fun _mdb_txn_begin(envPtr: Int, parentTxnPtr: Int, flags: Int, txnPtrPtr: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_txn_commit")
external fun _mdb_txn_commit(txnPtr: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_txn_abort")
external fun _mdb_txn_abort(txnPtr: Int)

@WasmImport("./lmdb-wrapper.mjs", "_mdb_txn_reset")
external fun _mdb_txn_reset(txnPtr: Int)

@WasmImport("./lmdb-wrapper.mjs", "_mdb_txn_renew")
external fun _mdb_txn_renew(txnPtr: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_txn_id")
external fun _mdb_txn_id(txnPtr: Int): Int

// Database functions
@WasmImport("./lmdb-wrapper.mjs", "_mdb_dbi_open")
external fun _mdb_dbi_open(txnPtr: Int, name: Int, flags: Int, dbiPtr: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_dbi_close")
external fun _mdb_dbi_close(envPtr: Int, dbi: Int)

@WasmImport("./lmdb-wrapper.mjs", "_mdb_drop")
external fun _mdb_drop(txnPtr: Int, dbi: Int, del: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_stat")
external fun _mdb_stat(txnPtr: Int, dbi: Int, statPtr: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_dbi_flags")
external fun _mdb_dbi_flags(txnPtr: Int, dbi: Int, flagsPtr: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_cmp")
external fun _mdb_cmp(txnPtr: Int, dbi: Int, aPtr: Int, bPtr: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_dcmp")
external fun _mdb_dcmp(txnPtr: Int, dbi: Int, aPtr: Int, bPtr: Int): Int

// Data operations
@WasmImport("./lmdb-wrapper.mjs", "_mdb_get")
external fun _mdb_get(txnPtr: Int, dbi: Int, keyPtr: Int, dataPtr: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_put")
external fun _mdb_put(txnPtr: Int, dbi: Int, keyPtr: Int, dataPtr: Int, flags: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_del")
external fun _mdb_del(txnPtr: Int, dbi: Int, keyPtr: Int, dataPtr: Int): Int

// Cursor operations
@WasmImport("./lmdb-wrapper.mjs", "_mdb_cursor_open")
external fun _mdb_cursor_open(txnPtr: Int, dbi: Int, cursorPtrPtr: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_cursor_close")
external fun _mdb_cursor_close(cursorPtr: Int)

@WasmImport("./lmdb-wrapper.mjs", "_mdb_cursor_get")
external fun _mdb_cursor_get(cursorPtr: Int, keyPtr: Int, dataPtr: Int, op: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_cursor_put")
external fun _mdb_cursor_put(cursorPtr: Int, keyPtr: Int, dataPtr: Int, flags: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_cursor_del")
external fun _mdb_cursor_del(cursorPtr: Int, flags: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_cursor_count")
external fun _mdb_cursor_count(cursorPtr: Int, countPtr: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_mdb_cursor_renew")
external fun _mdb_cursor_renew(txnPtr: Int, cursorPtr: Int): Int

// Error handling
@WasmImport("./lmdb-wrapper.mjs", "_mdb_strerror")
external fun _mdb_strerror(result: Int): Int

// Version information
@WasmImport("./lmdb-wrapper.mjs", "_mdb_version")
external fun _mdb_version(majorPtr: Int, minorPtr: Int, patchPtr: Int): Int

// Filesystem functions
@WasmImport("./lmdb-wrapper.mjs", "_mkdir")
external fun _mkdir(path: Int, mode: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_access")
external fun _access(path: Int, mode: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_rmdir")
external fun _rmdir(path: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_unlink")
external fun _unlink(path: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_opendir")
external fun _opendir(path: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_readdir")
external fun _readdir(dirPtr: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "_closedir")
external fun _closedir(dirPtr: Int): Int

// Filesystem mounting functions
@WasmImport("./lmdb-wrapper.mjs", "mountIDBFS")
external fun _mountIDBFS(pathPtr: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "mountNODEFS")
external fun _mountNODEFS(pathPtr: Int): Int

@WasmImport("./lmdb-wrapper.mjs", "mountFilesystem")
external fun _mountFilesystem(pathPtr: Int): Int

// Filesystem sync function
@WasmImport("./lmdb-wrapper.mjs", "syncFilesystem")
external fun _syncFilesystem(): Int