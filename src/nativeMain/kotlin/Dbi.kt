package lmdb

import kotlinx.cinterop.*
import kotlin.concurrent.Volatile

actual class Dbi actual constructor(name: String?, tx: Txn, vararg options: DbiOption) : AutoCloseable {
    private val arena = Arena()
    private val dbiPtr = arena.alloc<MDB_dbiVar>()
    internal val dbi: MDB_dbi
    private val env = tx.env
    
    @Volatile
    private var isClosed = false

    init {
        check(mdb_dbi_open(tx.ptr, name, options.asIterable().toFlags(), dbiPtr.ptr))
        dbi = dbiPtr.ptr.pointed.value
    }

    @OptIn(UnsafeNumber::class)
    actual fun stat(tx: Txn) : Stat {
        val statPtr: CValue<MDB_stat> = cValue<MDB_stat>()
        memScoped {
            check(mdb_stat(tx.ptr, dbi, statPtr))
        }
        val pointed = memScoped {
            statPtr.ptr.pointed
        }
        return Stat(
            pointed.ms_branch_pages.convert(), pointed.ms_depth, pointed.ms_entries.convert(), pointed.ms_leaf_pages.convert(),
            pointed.ms_overflow_pages.convert(), pointed.ms_psize
        )
    }
    
    actual fun compare(tx: Txn, a: Val, b: Val): Int {
        return mdb_cmp(tx.ptr, dbi, a.mdbVal.ptr, b.mdbVal.ptr)
    }
    
    actual fun dupCompare(tx: Txn, a: Val, b: Val): Int {
        return mdb_dcmp(tx.ptr, dbi, a.mdbVal.ptr, b.mdbVal.ptr)
    }
    
    actual fun flags(tx: Txn): Set<DbiOption> {
        memScoped {
            val flagsPtr = alloc<UIntVar>()
            check(mdb_dbi_flags(tx.ptr, dbi, flagsPtr.ptr))
            val flagsInt = flagsPtr.value
            return DbiOption.entries.filter { (flagsInt and it.option) != 0u }.toSet()
        }
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
        if (!isClosed) {
            isClosed = true
            mdb_dbi_close(env.ptr, dbi)
            arena.clear()
        }
    }
}