package lmdb

import com.sun.jna.Memory

/**
 * Reusable native staging for LMDB calls: one MDB_val pair plus grow-on-demand input
 * buffers, allocated once per owner (cursor or transaction) instead of per operation.
 *
 * Not thread-safe by design — it rides on the LMDB contract that a transaction and its
 * cursors are used by at most one thread at a time. After a successful call the MDB_vals
 * point either at LMDB-owned pages or at this scratch, so results built from them are
 * views that stay valid only until the next operation on the same owner.
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

    private fun ensure(mem: Memory, size: Int): Memory {
        if (size <= mem.size()) return mem
        var newSize = mem.size()
        while (newSize < size) newSize = newSize shl 1
        mem.close()
        return Memory(newSize)
    }

    override fun close() {
        keyMem.close()
        dataMem.close()
    }

    private companion object {
        const val INITIAL = 512L
    }
}
