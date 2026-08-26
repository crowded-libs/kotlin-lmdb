package lmdb

import com.sun.jna.Pointer

/**
 * Results are zero-copy views into LMDB pages (or this cursor's scratch): valid only until
 * the next operation on this cursor, never after its transaction ends. Copy immediately
 * (e.g. [Val.toByteArray]) to retain data. For MDB_SET the returned key aliases the input
 * scratch rather than the stored key.
 */
actual class Cursor(txn: Txn, dbi: Dbi) : AutoCloseable {
    private val ptr: Pointer = LmdbJna.mdb_cursor_open(txn.ptr, dbi.dbiHandle)
    private val scratch = NativeScratch()
    private var closed = false

    internal actual fun get(option: CursorOption): ValResult {
        scratch.clearKey()
        scratch.clearData()
        return readResult(LmdbJna.mdb_cursor_get(ptr, scratch.keyVal, scratch.dataVal, option.option.toInt()))
    }

    internal actual fun get(key: Val, option: CursorOption): ValResult {
        scratch.stageKey(key.stagingBytes())
        scratch.clearData()
        return readResult(LmdbJna.mdb_cursor_get(ptr, scratch.keyVal, scratch.dataVal, option.option.toInt()))
    }

    internal actual fun get(key: Val, data: Val, option: CursorOption): ValResult {
        scratch.stageKey(key.stagingBytes())
        scratch.stageData(data.stagingBytes())
        return readResult(LmdbJna.mdb_cursor_get(ptr, scratch.keyVal, scratch.dataVal, option.option.toInt()))
    }

    private fun readResult(rc: Int): ValResult =
        if (rc == 0) {
            // LMDB rewrote the MDB_vals to point at its pages (or left them on scratch for
            // exact-match ops); wrap without copying.
            buildReadResult(
                rc,
                Val.fromMDBVal(MDBVal.fromMdbVal(scratch.keyVal)),
                Val.fromMDBVal(MDBVal.fromMdbVal(scratch.dataVal))
            )
        } else {
            buildReadResult(rc, Val.fromMDBVal(MDBVal.EMPTY), Val.fromMDBVal(MDBVal.EMPTY))
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
