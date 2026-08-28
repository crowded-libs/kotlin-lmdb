package lmdb

import com.sun.jna.Pointer

/**
 * Results are views into LMDB pages, valid only until the next operation on this cursor
 * or the end of its transaction. Copy immediately (e.g. [Val.toByteArray]) to retain data.
 * Keys from SET, GET_BOTH, and GET_BOTH_RANGE are copied off call scratch so they survive
 * a subsequent operation on this cursor.
 */
actual class Cursor(txn: Txn, dbi: Dbi) : AutoCloseable {
    private val scratch = NativeScratch()
    private val ptr: Pointer = LmdbJna.mdb_cursor_open(txn.ptr, dbi.dbiHandle)
    private var closed = false

    internal actual fun get(option: CursorOption): ValResult {
        scratch.clearKey()
        scratch.clearData()
        return readResult(
            LmdbJna.mdb_cursor_get(ptr, scratch.keyVal, scratch.dataVal, option.option.toInt()),
            option
        )
    }

    internal actual fun get(key: Val, option: CursorOption): ValResult {
        scratch.stageKey(key.stagingBytes())
        scratch.clearData()
        return readResult(
            LmdbJna.mdb_cursor_get(ptr, scratch.keyVal, scratch.dataVal, option.option.toInt()),
            option
        )
    }

    internal actual fun get(key: Val, data: Val, option: CursorOption): ValResult {
        scratch.stageKey(key.stagingBytes())
        scratch.stageData(data.stagingBytes())
        return readResult(
            LmdbJna.mdb_cursor_get(ptr, scratch.keyVal, scratch.dataVal, option.option.toInt()),
            option
        )
    }

    private fun readResult(rc: Int, option: CursorOption): ValResult {
        if (rc != 0) {
            return buildReadResult(rc, Val.fromMDBVal(MDBVal.EMPTY), Val.fromMDBVal(MDBVal.EMPTY))
        }
        val key = if (leavesKeyOnCallScratch(option)) {
            scratch.snapshotKey()
        } else {
            Val.fromMDBVal(MDBVal.fromMdbVal(scratch.keyVal))
        }
        return buildReadResult(rc, key, Val.fromMDBVal(MDBVal.fromMdbVal(scratch.dataVal)))
    }

    private fun leavesKeyOnCallScratch(option: CursorOption): Boolean = when (option) {
        CursorOption.SET, CursorOption.GET_BOTH, CursorOption.GET_BOTH_RANGE -> true
        else -> false
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
        scratch.stageKey(key.stagingBytes())
        scratch.stageData(data.stagingBytes())
        val result = LmdbJna.mdb_cursor_put(ptr, scratch.keyVal, scratch.dataVal, option.option.toInt())
        return buildResult(result, key, data)
    }

    actual override fun close() {
        if(closed)
            return

        LmdbJna.mdb_cursor_close(ptr)
        scratch.close()
        closed = true
    }
}
