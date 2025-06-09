package lmdb

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.LongByReference
import com.sun.jna.ptr.PointerByReference

/**
 * JNA interface for LMDB native library
 */
interface LmdbLibrary : Library {
    
    // Version information
    fun mdb_version(major: IntByReference?, minor: IntByReference?, patch: IntByReference?): String
    fun mdb_strerror(err: Int): String
    
    // Environment management
    fun mdb_env_create(env: PointerByReference): Int
    fun mdb_env_open(env: Pointer, path: String, flags: Int, mode: Int): Int
    fun mdb_env_close(env: Pointer)
    fun mdb_env_set_mapsize(env: Pointer, size: Long): Int
    fun mdb_env_set_maxreaders(env: Pointer, readers: Int): Int
    fun mdb_env_set_maxdbs(env: Pointer, dbs: Int): Int
    fun mdb_env_set_flags(env: Pointer, flags: Int, onoff: Int): Int
    fun mdb_env_get_flags(env: Pointer, flags: IntByReference): Int
    fun mdb_env_get_path(env: Pointer, path: PointerByReference): Int
    fun mdb_env_get_maxreaders(env: Pointer, readers: IntByReference): Int
    fun mdb_env_get_maxkeysize(env: Pointer): Int
    fun mdb_env_copy(env: Pointer, path: String): Int
    fun mdb_env_copy2(env: Pointer, path: String, flags: Int): Int
    fun mdb_env_sync(env: Pointer, force: Int): Int
    fun mdb_env_info(env: Pointer, stat: MDB_envinfo): Int
    fun mdb_env_stat(env: Pointer, stat: MDB_stat): Int
    fun mdb_reader_check(env: Pointer, dead: IntByReference): Int
    
    // Transaction management
    fun mdb_txn_begin(env: Pointer, parent: Pointer?, flags: Int, txn: PointerByReference): Int
    fun mdb_txn_commit(txn: Pointer): Int
    fun mdb_txn_abort(txn: Pointer)
    fun mdb_txn_reset(txn: Pointer)
    fun mdb_txn_renew(txn: Pointer): Int
    fun mdb_txn_env(txn: Pointer): Pointer
    fun mdb_txn_id(txn: Pointer): Long
    
    // Database operations
    fun mdb_dbi_open(txn: Pointer, name: String?, flags: Int, dbi: IntByReference): Int
    fun mdb_dbi_close(env: Pointer, dbi: Int)
    fun mdb_drop(txn: Pointer, dbi: Int, del: Int): Int
    fun mdb_stat(txn: Pointer, dbi: Int, stat: MDB_stat): Int
    fun mdb_dbi_flags(txn: Pointer, dbi: Int, flags: IntByReference): Int
    
    // Comparison functions
    fun mdb_cmp(txn: Pointer, dbi: Int, a: MDB_val, b: MDB_val): Int
    fun mdb_dcmp(txn: Pointer, dbi: Int, a: MDB_val, b: MDB_val): Int
    
    // Data operations
    fun mdb_get(txn: Pointer, dbi: Int, key: MDB_val, data: MDB_val): Int
    fun mdb_put(txn: Pointer, dbi: Int, key: MDB_val, data: MDB_val, flags: Int): Int
    fun mdb_del(txn: Pointer, dbi: Int, key: MDB_val, data: MDB_val?): Int
    
    // Cursor operations
    fun mdb_cursor_open(txn: Pointer, dbi: Int, cursor: PointerByReference): Int
    fun mdb_cursor_close(cursor: Pointer)
    fun mdb_cursor_renew(txn: Pointer, cursor: Pointer): Int
    fun mdb_cursor_txn(cursor: Pointer): Pointer
    fun mdb_cursor_dbi(cursor: Pointer): Int
    fun mdb_cursor_get(cursor: Pointer, key: MDB_val, data: MDB_val, op: Int): Int
    fun mdb_cursor_put(cursor: Pointer, key: MDB_val, data: MDB_val, flags: Int): Int
    fun mdb_cursor_del(cursor: Pointer, flags: Int): Int
    fun mdb_cursor_count(cursor: Pointer, count: LongByReference): Int
    
    // Comparator callbacks
    fun mdb_set_compare(txn: Pointer, dbi: Int, cmp: MDB_cmp_func?): Int
    fun mdb_set_dupsort(txn: Pointer, dbi: Int, cmp: MDB_cmp_func?): Int
    
    // Callback interface for comparators
    interface MDB_cmp_func : Callback {
        fun compare(a: MDB_val, b: MDB_val): Int
    }
    
    companion object {
        const val LIBRARY_NAME = "lmdb"
    }
}