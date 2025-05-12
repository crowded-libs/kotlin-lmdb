package lmdb

import kotlinx.cinterop.*

actual class Cursor(txn: Txn, dbi: Dbi) : AutoCloseable {
    private val arena = Arena()
    private val ptr: CPointer<MDB_cursor>

    init {
        memScoped {
            val ptrVar = arena.allocPointerTo<MDB_cursor>()
            check(mdb_cursor_open(txn.ptr, dbi.dbi, ptrVar.ptr))
            ptr = checkNotNull(ptrVar.value)
        }
    }

    internal actual fun get(option: CursorOption) : ValResult {
        val key = Val.output()
        val data = Val.output()
        val result = mdb_cursor_get(ptr, key.mdbVal.ptr, data.mdbVal.ptr, getMdbCursorOp(option))
        return buildReadResult(result, key, data)
    }

    internal actual fun get(key: Val, option: CursorOption): ValResult {
        val mdbData = Val.output()
        val result = mdb_cursor_get(ptr, key.mdbVal.ptr, mdbData.mdbVal.ptr, getMdbCursorOp(option))
        return buildReadResult(result, key, mdbData)
    }

    internal actual fun get(key: Val, data: Val, option: CursorOption): ValResult {
        val result = mdb_cursor_get(ptr, key.mdbVal.ptr, data.mdbVal.ptr, getMdbCursorOp(option))
        return buildReadResult(result, key, data)
    }
    
    private fun getMdbCursorOp(option: CursorOption): MDB_cursor_op = when(option) {
        CursorOption.FIRST -> MDB_cursor_op.MDB_FIRST
        CursorOption.FIRST_DUP -> MDB_cursor_op.MDB_FIRST_DUP
        CursorOption.GET_BOTH -> MDB_cursor_op.MDB_GET_BOTH
        CursorOption.GET_BOTH_RANGE -> MDB_cursor_op.MDB_GET_BOTH_RANGE
        CursorOption.GET_CURRENT -> MDB_cursor_op.MDB_GET_CURRENT
        CursorOption.GET_MULTIPLE -> MDB_cursor_op.MDB_GET_MULTIPLE
        CursorOption.LAST -> MDB_cursor_op.MDB_LAST
        CursorOption.LAST_DUP -> MDB_cursor_op.MDB_LAST_DUP
        CursorOption.NEXT -> MDB_cursor_op.MDB_NEXT
        CursorOption.NEXT_DUP -> MDB_cursor_op.MDB_NEXT_DUP
        CursorOption.NEXT_MULTIPLE -> MDB_cursor_op.MDB_NEXT_MULTIPLE
        CursorOption.NEXT_NODUP -> MDB_cursor_op.MDB_NEXT_NODUP
        CursorOption.PREV -> MDB_cursor_op.MDB_PREV
        CursorOption.PREV_DUP -> MDB_cursor_op.MDB_PREV_DUP
        CursorOption.PREV_NODUP -> MDB_cursor_op.MDB_PREV_NODUP
        CursorOption.SET -> MDB_cursor_op.MDB_SET
        CursorOption.SET_KEY -> MDB_cursor_op.MDB_SET_KEY
        CursorOption.SET_RANGE -> MDB_cursor_op.MDB_SET_RANGE
    }

    internal actual fun put(key: Val, data: Val, option: CursorPutOption): ValResult {
        val result = mdb_cursor_put(ptr, key.mdbVal.ptr, data.mdbVal.ptr, option.option)
        return buildReadResult(result, key, data)
    }

    actual fun delete() {
        check(mdb_cursor_del(ptr, CursorDeleteOption.NONE.option))
    }

    actual fun deleteDuplicateData() {
        check(mdb_cursor_del(ptr, CursorDeleteOption.NO_DUP_DATA.option))
    }
    
    actual fun countDuplicates(): UInt {
        memScoped {
            val countVar = alloc<UIntVar>()
            check(mdb_cursor_count(ptr, countVar.ptr.reinterpret()))
            return countVar.value
        }
    }
    
    actual fun renew(txn: Txn) {
        check(mdb_cursor_renew(txn.ptr, ptr))
    }

    actual override fun close() {
        mdb_cursor_close(ptr)
    }
}