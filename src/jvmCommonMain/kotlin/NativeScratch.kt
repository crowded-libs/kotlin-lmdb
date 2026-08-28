package lmdb

import com.sun.jna.Memory

/**
 * Not thread-safe: a transaction and its cursors are used by at most one thread at a time.
 * Staged MDB_vals and result views built from them stay valid only until the next operation
 * on the same owner.
 */
internal class NativeScratch : AutoCloseable {
    val keyVal = MDB_val()
    val dataVal = MDB_val()
    private var keyMem = Memory(INITIAL)
    private var dataMem = Memory(INITIAL)

    fun stageKey(bytes: ByteArray) {
        keyMem = ensure(keyMem, bytes.size)
        keyMem.write(0, bytes, 0, bytes.size)
        keyVal.mv_data = keyMem
        keyVal.mv_size = bytes.size.toLong()
    }

    fun stageData(bytes: ByteArray) {
        dataMem = ensure(dataMem, bytes.size)
        dataMem.write(0, bytes, 0, bytes.size)
        dataVal.mv_data = dataMem
        dataVal.mv_size = bytes.size.toLong()
    }

    fun clearKey() {
        keyVal.mv_data = null
        keyVal.mv_size = 0
    }

    fun clearData() {
        dataVal.mv_data = null
        dataVal.mv_size = 0
    }

    /** Heap copy of the current key, for ops where LMDB leaves key pointing at this scratch. */
    fun snapshotKey(): Val {
        val data = keyVal.mv_data
        val size = keyVal.mv_size.toInt()
        if (data == null || size <= 0) return Val.fromMDBVal(MDBVal.EMPTY)
        val bytes = ByteArray(size)
        data.read(0, bytes, 0, size)
        return bytes.toVal()
    }

    private fun ensure(mem: Memory, size: Int): Memory {
        if (size <= mem.size()) return mem
        var newSize = mem.size()
        while (newSize < size) newSize = newSize shl 1
        val grown = Memory(newSize)
        mem.close()
        return grown
    }

    override fun close() {
        keyMem.close()
        dataMem.close()
    }

    private companion object {
        const val INITIAL = 512L
    }
}
