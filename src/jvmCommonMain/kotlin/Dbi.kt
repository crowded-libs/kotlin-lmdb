package lmdb

import java.util.concurrent.atomic.AtomicBoolean

actual class Dbi actual constructor(name: String?, tx: Txn, vararg options: DbiOption) : AutoCloseable {
    internal val dbiHandle: Int
    private val closed = AtomicBoolean(false)
    private val env = tx.env

    init {
        dbiHandle = LmdbJna.mdb_dbi_open(tx.ptr, name, options.asIterable().toFlags().toInt())
        if (dbiHandle < 0) {
            throw LmdbException("Failed to open database: ${LmdbJna.mdb_strerror(dbiHandle)}")
        }
    }

    actual fun stat(tx: Txn) : Stat {
        val mdbStat = LmdbJna.Stat()
        check(LmdbJna.mdb_stat(tx.ptr, dbiHandle, mdbStat))
        return Stat(
            branchPages = mdbStat.branchPages.toULong(),
            depth = mdbStat.depth.toUInt(),
            entries = mdbStat.entries.toULong(),
            leafPages = mdbStat.leafPages.toULong(),
            overflowPages = mdbStat.overflowPages.toULong(),
            pSize = mdbStat.pageSize.toUInt()
        )
    }
    
    actual fun compare(tx: Txn, a: Val, b: Val): Int {
        return LmdbJna.mdb_cmp(tx.ptr, dbiHandle, a.mdbVal.buffer, b.mdbVal.buffer)
    }
    
    actual fun dupCompare(tx: Txn, a: Val, b: Val): Int {
        return LmdbJna.mdb_dcmp(tx.ptr, dbiHandle, a.mdbVal.buffer, b.mdbVal.buffer)
    }
    
    actual fun flags(tx: Txn): Set<DbiOption> {
        val flagsValue = LmdbJna.mdb_dbi_flags(tx.ptr, dbiHandle)
        if (flagsValue < 0) {
            throw LmdbException("Failed to get database flags: ${LmdbJna.mdb_strerror(flagsValue)}")
        }
        val flagsUInt = flagsValue.toUInt()
        return DbiOption.entries.filter { (flagsUInt and it.option) != 0u }.toSet()
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
            LmdbJna.mdb_dbi_close(env.ptr, dbiHandle)
        }
    }
}