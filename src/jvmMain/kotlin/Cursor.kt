import Library.Companion.LMDB
import jnr.ffi.Pointer
import jnr.ffi.byref.NativeLongByReference
import jnr.ffi.byref.PointerByReference

actual class Cursor(txn: Txn, dbi: Dbi) : AutoCloseable {
    private val csrPtr = PointerByReference()
    private val ptr: Pointer
    private var closed = false

    init {
        check(LMDB.mdb_cursor_open(txn.ptr, dbi.ptr, csrPtr))
        ptr = csrPtr.value
    }

    internal actual fun get(option: CursorOption): ValResult {
        val key = MDBVal.output()
        val data = MDBVal.output()
        val result = LMDB.mdb_cursor_get(ptr, key.ptr, data.ptr, option.option.toInt())
        return buildReadResult(result, Val.fromMDBVal(key), Val.fromMDBVal(data))
    }

    internal actual fun get(key: Val, option: CursorOption): ValResult {
        val data = MDBVal.output()
        val result = LMDB.mdb_cursor_get(ptr, key.mdbVal.ptr, data.ptr, option.option.toInt())
        return buildReadResult(result, key, Val.fromMDBVal(data))
    }

    internal actual fun get(key: Val, data: Val, option: CursorOption): ValResult {
        val result = LMDB.mdb_cursor_get(ptr, key.mdbVal.ptr, data.mdbVal.ptr, option.option.toInt())
        return buildReadResult(result, key, data)
    }

    actual fun delete() {
        check(LMDB.mdb_cursor_del(ptr, CursorDeleteOption.NONE.option.toInt()))
    }

    actual fun deleteDuplicateData() {
        check(LMDB.mdb_cursor_del(ptr, CursorDeleteOption.NO_DUP_DATA.option.toInt()))
    }

    actual fun countDuplicates(): ULong {
        val countPtr = NativeLongByReference()
        check(LMDB.mdb_cursor_count(ptr, countPtr))
        return countPtr.value.toLong().toULong()
    }
    
    actual fun renew(txn: Txn) {
        check(LMDB.mdb_cursor_renew(txn.ptr, ptr))
    }

    internal actual fun put(key: Val, data: Val, option: CursorPutOption): ValResult {
        val result = LMDB.mdb_cursor_put(ptr, key.mdbVal.ptr, data.mdbVal.ptr, option.option.toInt())
        return buildResult(result, key, data)
    }

    actual override fun close() {
        if(closed)
            return

        LMDB.mdb_cursor_close(ptr)
        closed = true
    }
}