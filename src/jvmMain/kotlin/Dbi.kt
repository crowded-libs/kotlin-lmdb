package lmdb

import lmdb.Library.Companion.LMDB
import lmdb.Library.Companion.RUNTIME
import jnr.ffi.Memory.allocateDirect
import jnr.ffi.NativeType
import jnr.ffi.Pointer
import java.util.concurrent.atomic.AtomicBoolean

actual class Dbi actual constructor(name: String?, tx: Txn, vararg options: DbiOption) : AutoCloseable {
    private val dbiPtr = allocateDirect(RUNTIME, NativeType.ADDRESS)
    internal val ptr: Pointer
    private val closed = AtomicBoolean(false)
    private val env = tx.env

    init {
        check(LMDB.mdb_dbi_open(tx.ptr, name?.toByteArray(), options.asIterable().toFlags().toInt(), dbiPtr))
        ptr = dbiPtr.getPointer(0)
    }

    actual fun stat(tx: Txn) : Stat {
        val mdbStat: Library.MDB_stat = Library.MDB_stat(RUNTIME)
        check(LMDB.mdb_stat(tx.ptr, ptr, mdbStat))
        return Stat(mdbStat.f2_ms_branch_pages.get().toULong(), mdbStat.f1_ms_depth.get().toUInt(),
            mdbStat.f5_ms_entries.get().toULong(), mdbStat.f3_ms_leaf_pages.get().toULong(),
            mdbStat.f4_ms_overflow_pages.get().toULong(), mdbStat.f0_ms_psize.get().toUInt())
    }
    
    actual fun compare(tx: Txn, a: Val, b: Val): Int {
        return LMDB.mdb_cmp(tx.ptr, ptr, a.mdbVal.ptr, b.mdbVal.ptr)
    }
    
    actual fun dupCompare(tx: Txn, a: Val, b: Val): Int {
        return LMDB.mdb_dcmp(tx.ptr, ptr, a.mdbVal.ptr, b.mdbVal.ptr)
    }
    
    actual fun flags(tx: Txn): Set<DbiOption> {
        val flagsRef = jnr.ffi.byref.IntByReference()
        check(LMDB.mdb_dbi_flags(tx.ptr, ptr, flagsRef))
        val flagsInt = flagsRef.value.toUInt()
        return DbiOption.entries.filter { (flagsInt and it.option) != 0u }.toSet()
    }
    
    /**
     * Close the database handle. 
     * 
     * This call is not mutex protected. Handles should only be closed by
     * a single thread, and only if no other threads are going to reference
     * the database handle or one of its cursors any further. Do not close
     * a handle if an existing transaction has modified its database.
     * 
     * Safe to call multiple times; only the first call has an effect.
     */
    actual override fun close() {
        if (closed.compareAndSet(false, true)) {
            LMDB.mdb_dbi_close(env.ptr, ptr)
        }
    }
}