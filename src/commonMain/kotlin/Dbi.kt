package lmdb
/**
 * Represents a database handle in the LMDB environment.
 *
 * A database handle denotes the name and parameters of a database,
 * independently of whether such a database exists. The handle may be discarded
 * by calling [close].
 *
 * The Dbi class wraps the MDB_dbi handle from the native LMDB library.
 *
 * Key characteristics:
 * - Each database within an environment is identified by its handle
 * - Can be named or unnamed (the default/unnamed database)
 * - Supports various configuration options like duplicate keys, key ordering, etc.
 * - Multiple databases in the same environment share the same memory map
 * - Can have custom key and data comparison functions
 *
 * Usage example:
 * ```
 * env.beginTxn().use { txn ->
 *     // Open a database in the environment
 *     val db = txn.dbiOpen("mydb", DbiOption.Create, DbiOption.DupSort)
 *     
 *     // Use the database...
 *     
 *     txn.commit()
 * }
 * ```
 *
 * Note: Database handles are owned by the environment after a successful commit.
 * They can be used by future transactions until explicitly closed.
 */
expect class Dbi(name: String? = null, tx: Txn, vararg options: DbiOption) : AutoCloseable {
    
    /**
     * Retrieves statistics for this database.
     *
     * The statistics include page size, B-tree depth, and counts of various page types.
     * This provides information about the internal structure and size of the database.
     *
     * @param tx A transaction handle
     * @return A Stat object containing the database statistics
     * 
     * @throws LmdbException if an invalid parameter was specified
     */
    fun stat(tx: Txn): Stat
    
    /**
     * Compare two keys according to the comparison function of this database.
     *
     * This returns a comparison as if the two data items were keys in the database.
     * The comparison function is either the default lexicographical comparison or
     * a custom one set when opening the database.
     *
     * @param tx A transaction handle
     * @param a The first key to compare
     * @param b The second key to compare
     * @return Less than 0 if a < b, 0 if a == b, greater than 0 if a > b
     * 
     * @throws LmdbException if an invalid parameter was specified
     */
    fun compare(tx: Txn, a: Val, b: Val): Int
    
    /**
     * Compare two data items according to the duplicate sorting function of this database.
     *
     * This returns a comparison as if the two items were data items of this database 
     * with the [DbiOption.DupSort] flag. The comparison function is either the default 
     * lexicographical comparison or a custom one set when opening the database.
     *
     * @param tx A transaction handle
     * @param a The first data item to compare
     * @param b The second data item to compare
     * @return Less than 0 if a < b, 0 if a == b, greater than 0 if a > b
     * 
     * @throws LmdbException if an invalid parameter was specified or the database
     *         was not opened with [DbiOption.DupSort]
     */
    fun dupCompare(tx: Txn, a: Val, b: Val): Int
    
    /**
     * Retrieves the flags that were used to open this database.
     *
     * @param tx A transaction handle
     * @return A set of [DbiOption] flags associated with this database
     * 
     * @throws LmdbException if an invalid parameter was specified
     */
    fun flags(tx: Txn): Set<DbiOption>
    
    /**
     * Close the database handle.
     * 
     * This is generally unnecessary, but lets [Txn.dbiOpen] reuse the handle value.
     * It's usually better to set a bigger [Env.maxDatabases] unless that value
     * would be impractically large.
     *
     * This call is not mutex protected. Handles should only be closed by
     * a single thread, and only if no other threads are going to reference
     * the database handle or one of its cursors any further. Do not close
     * a handle if an existing transaction has modified its database.
     * 
     * Safe to call multiple times; only the first call has an effect.
     */
    override fun close()
}