/**
 * Represents an LMDB transaction, which provides a consistent view of the database.
 *
 * All database operations require a transaction handle. Transactions can be read-only
 * or read-write. Multiple read-only transactions can run concurrently, but only one
 * write transaction can be active at any point in time.
 *
 * The Txn class wraps the MDB_txn structure from the native LMDB library.
 *
 * Key characteristics:
 * - Required for all database operations
 * - Provides ACID guarantees
 * - Can be read-only or read-write
 * - Can be nested (child transactions)
 * - Thread-confined: must only be used by a single thread
 * - Automatically managed via Kotlin's use() function due to implementing AutoCloseable
 *
 * Usage example:
 * ```
 * env.beginTxn().use { txn ->
 *     val db = txn.dbiOpen("mydb", DbiOption.Create)
 *     txn.put(db, "key".toByteArray().toVal(), "value".toByteArray().toVal())
 *     txn.commit()
 * }
 * ```
 *
 * For read-only operations:
 * ```
 * env.beginTxn(TxnOption.ReadOnly).use { txn ->
 *     val db = txn.dbiOpen("mydb")
 *     val result = txn.get(db, "key".toByteArray().toVal())
 *     // No need to commit read-only transactions, they'll be aborted on close
 * }
 * ```
 */
expect class Txn : AutoCloseable {

    internal var state: TxnState
    internal constructor(env: Env, vararg options: TxnOption)
    internal constructor(env: Env, parent: Txn?, vararg options: TxnOption)
    
    /**
     * The transaction ID.
     * 
     * For a read-only transaction, this corresponds to the snapshot being read;
     * concurrent readers will frequently have the same transaction ID.
     * 
     * @return A transaction ID, valid if this transaction is active.
     */
    val id: ULong

    /**
     * Creates a nested transaction (child transaction).
     *
     * A parent transaction and its cursors may not issue any other operations than
     * [commit] and [abort] while it has active child transactions.
     *
     * Nested transactions can be used to perform multiple operations that can be 
     * committed or aborted independently of the parent transaction.
     *
     * @param options Special options for this transaction, such as [TxnOption.ReadOnly]
     * @return A new transaction handle that is a child of this transaction
     * 
     * @throws LmdbException on errors such as:
     *  - A fatal error occurred earlier
     *  - Out of memory
     */
    fun begin(vararg options: TxnOption): Txn

    /**
     * Abandons all operations performed in this transaction instead of saving them.
     *
     * The transaction handle is freed. It and its cursors must not be used
     * again after this call, except with cursor.renew().
     *
     * Only write transactions free all cursors. Read-only transactions keep their
     * cursors available for reuse via cursor.renew().
     */
    fun abort()

    /**
     * Resets a read-only transaction.
     *
     * This releases the read lock on the database without freeing the transaction object.
     * It allows an application to reuse a transaction object instead of creating a new one.
     * This is useful when you want to release the read lock on the database but plan to
     * execute another read-only transaction soon.
     *
     * After calling this method, [renew] must be called before using the transaction again.
     *
     * This is only valid for read-only transactions. Cursors opened within the transaction 
     * must not be used again until [renew] is called, except with cursor.renew().
     *
     * Note: Reader locks don't generally interfere with writers, but they keep old versions 
     * of database pages allocated. Thus, under heavy load, the database size may grow much more 
     * rapidly than otherwise if read transactions aren't managed properly.
     */
    fun reset()

    /**
     * Renews a read-only transaction that was previously reset.
     *
     * This acquires a new read lock on the database, allowing the transaction
     * to be used again. It must be called before a reset transaction may be used again.
     *
     * This is only valid for read-only transactions.
     *
     * @throws LmdbException on errors such as:
     *  - A fatal error occurred earlier
     *  - An invalid parameter was specified
     */
    fun renew()

    /**
     * Commits all operations performed in this transaction to the database.
     *
     * All database operations are persisted when this method returns successfully.
     * The transaction handle is freed and must not be used again after this call.
     * 
     * Note: Earlier versions of LMDB documentation incorrectly stated that all cursors would be freed. Only 
     * write-transactions free cursors.
     *
     * @throws LmdbException on errors such as:
     *  - An invalid parameter was specified
     *  - No disk space
     *  - A low-level I/O error occurred while writing
     *  - Out of memory
     */
    fun commit()

    /**
     * Opens a database in the environment.
     *
     * A database handle denotes the name and parameters of a database in the environment,
     * independently of whether such a database exists.
     * 
     * The database handle will be private to the current transaction until
     * the transaction is successfully committed. If the transaction is
     * aborted, the handle will be closed automatically.
     * After a successful commit, the handle will be available to other transactions.
     *
     * Note: This function must not be called from multiple concurrent
     * transactions in the same process. A transaction that uses
     * this function must finish (either commit or abort) before
     * any other transaction in the process may use this function.
     *
     * @param name The name of the database to open, or null for the default database.
     *             To use named databases, [Env.maxDatabases] must be set before opening the environment.
     * @param options Special options for this database. See [DbiOption] for details.
     * @return A database handle (Dbi)
     * 
     * @throws LmdbException on errors such as:
     *  - The specified database doesn't exist and [DbiOption.Create] was not specified
     *  - Too many databases have been opened
     */
    fun dbiOpen(name: String? = null, vararg options: DbiOption): Dbi

    /**
     * Opens a database in the environment with custom key and value comparers.
     * 
     * Custom comparers allow you to define how keys and values are compared when
     * determining their order in the database. This is useful for specialized sorting
     * requirements or for handling complex data structures.
     *
     * Note: The same comparison function must be used by every program accessing the database.
     * This function must be called before any data access functions are used,
     * otherwise data corruption may occur.
     * 
     * @param name The name of the database to open, or null for the default database
     * @param config Configuration object that can contain key and duplicate value comparers
     * @param options Special options for this database. See [DbiOption] for details.
     * @return A database handle (Dbi)
     *
     * @throws LmdbException on errors such as:
     *  - The specified database doesn't exist and [DbiOption.Create] was not specified
     *  - Too many databases have been opened
     */
    fun dbiOpen(name: String?, config: DbiConfig, vararg options: DbiOption): Dbi
    
    /**
     * Deletes (drops) a database and close its handle.
     *
     * This permanently removes the database from the environment.
     * If the environment has been opened by other processes, they should close the database
     * before this operation is performed.
     *
     * Note: Only a single thread should call this function, and it should only be called if
     * no other threads are going to reference the database handle or one of its cursors any further.
     *
     * @param dbi A database handle returned by [dbiOpen]
     * 
     * @throws LmdbException if the operation fails
     */
    fun drop(dbi: Dbi)

    /**
     * Empties a database, removing all key/data pairs.
     *
     * The database remains open after this operation and can be used again.
     * This is equivalent to deleting all items one by one, but is much faster.
     *
     * @param dbi A database handle returned by [dbiOpen]
     * 
     * @throws LmdbException if the operation fails
     */
    fun empty(dbi: Dbi)

    /**
     * Retrieves a key/data pair from a database.
     *
     * If the database supports duplicate keys ([DbiOption.DupSort]) then the first data item
     * for the key will be returned. Retrieval of other items requires the use of a cursor.
     *
     * Note: The memory pointed to by the returned values is owned by the database.
     * The caller need not dispose of the memory, and may not modify it in any way.
     * For values returned in a read-only transaction, any modification attempts will
     * cause undefined behavior.
     *
     * Values returned from the database are valid only until a subsequent update operation,
     * or the end of the transaction.
     *
     * @param dbi A database handle returned by [dbiOpen]
     * @param key The key to search for in the database
     * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
     *         The key and data values are only valid if status is 0.
     */
    fun get(dbi: Dbi, key: Val): ValResult

    /**
     * Stores a key/data pair in a database.
     *
     * The default behavior is to enter the new key/data pair, replacing any previously existing key
     * if duplicates are disallowed, or adding a duplicate data item if duplicates are allowed ([DbiOption.DupSort]).
     *
     * @param dbi A database handle returned by [dbiOpen]
     * @param key The key to store in the database
     * @param data The data to store
     * @param options Special options for this operation. See [PutOption] for details.
     * 
     * @throws LmdbException on errors such as:
     *  - The database is full (map size limit reached)
     *  - The transaction has too many dirty pages
     *  - An attempt was made to write in a read-only transaction
     *  - An invalid parameter was specified
     *  - Key exists ([PutOption.NoOverwrite]) or key/data pair exists ([PutOption.NoDupData])
     */
    fun put(dbi: Dbi, key: Val, data: Val, vararg options: PutOption)

    /**
     * Deletes a key/data pair from a database.
     *
     * If the database does not support sorted duplicate data items ([DbiOption.DupSort])
     * this will delete any data associated with the key.
     *
     * If the database supports sorted duplicates and the data parameter is null,
     * all of the duplicate data items for the key will be deleted. Otherwise, if
     * the data parameter is not null, only the matching data item will be deleted.
     *
     * @param dbi A database handle returned by [dbiOpen]
     * @param key The key to delete from the database
     * 
     * @throws LmdbException on errors such as:
     *  - An attempt was made to write in a read-only transaction
     *  - An invalid parameter was specified
     *  - The key/data pair was not found
     */
    fun delete(dbi: Dbi, key: Val)

    /**
     * Deletes a specific key/data pair from a database.
     *
     * This version of delete is used with databases that support sorted duplicate data items ([DbiOption.DupSort]).
     * It allows deletion of a specific data item associated with the key.
     *
     * @param dbi A database handle returned by [dbiOpen]
     * @param key The key to delete from the database
     * @param data The data value to delete
     * 
     * @throws LmdbException on errors such as:
     *  - An attempt was made to write in a read-only transaction
     *  - An invalid parameter was specified
     *  - The key/data pair was not found
     */
    fun delete(dbi: Dbi, key: Val, data: Val)

    /**
     * Creates a cursor handle for a database.
     *
     * A cursor is associated with a specific transaction and database.
     * A cursor cannot be used when its database handle is closed, nor
     * when its transaction has ended, except with cursor.renew().
     *
     * A cursor in a write-transaction can be closed before its transaction ends,
     * and will otherwise be closed when its transaction ends.
     * A cursor in a read-only transaction must be closed explicitly, before
     * or after its transaction ends. It can be reused with cursor.renew() before finally closing it.
     *
     * @param dbi A database handle returned by [dbiOpen]
     * @return A cursor handle
     * 
     * @throws LmdbException on errors such as:
     *  - An invalid parameter was specified
     */
    fun openCursor(dbi: Dbi): Cursor

    /**
     * Closes the transaction.
     *
     * For write transactions, this is equivalent to calling [abort].
     * For read-only transactions, the transaction will be reset if [Env.NoTLS] flag is used,
     * otherwise it will be aborted.
     *
     * Transactions should typically be managed with Kotlin's use() function, which
     * automatically calls close() when the transaction block completes.
     */
    override fun close()
}

/**
 * Retrieves a key/data pair from a database.
 *
 * This is a convenience overload that takes a ByteArray key instead of a Val.
 *
 * @param dbi A database handle returned by [Txn.dbiOpen]
 * @param key The key to search for in the database as a ByteArray
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Txn.get(dbi: Dbi, key: ByteArray) = this.get(dbi, key.toVal())

/**
 * Stores a key/data pair in a database.
 *
 * This is a convenience overload that takes ByteArray key and data instead of Val objects.
 *
 * @param dbi A database handle returned by [Txn.dbiOpen]
 * @param key The key to store in the database as a ByteArray
 * @param data The data to store as a ByteArray
 * @param options Special options for this operation. See [PutOption] for details.
 */
fun Txn.put(dbi: Dbi, key: ByteArray, data: ByteArray, vararg options: PutOption) =
    this.put(dbi, key.toVal(), data.toVal(), *options)

/**
 * Deletes a key/data pair from a database.
 *
 * This is a convenience overload that takes a ByteArray key instead of a Val.
 *
 * @param dbi A database handle returned by [Txn.dbiOpen]
 * @param key The key to delete from the database as a ByteArray
 */
fun Txn.delete(dbi: Dbi, key: ByteArray) = this.delete(dbi, key.toVal())

/**
 * Deletes a specific key/data pair from a database.
 *
 * This is a convenience overload that takes ByteArray key and data instead of Val objects.
 *
 * @param dbi A database handle returned by [Txn.dbiOpen]
 * @param key The key to delete from the database as a ByteArray
 * @param data The data to delete as a ByteArray
 */
fun Txn.delete(dbi: Dbi, key: ByteArray, data: ByteArray) = this.delete(dbi, key.toVal(), data.toVal())

/**
 * Checks if the transaction is in a ready state for performing operations.
 *
 * This function verifies that the transaction is in the [TxnState.Ready] state,
 * which means it can be used for database operations. A transaction may not be
 * ready if it has been committed, aborted, or reset.
 *
 * @throws LmdbException if the transaction is not in the Ready state
 */
fun Txn.checkReady() {
    if (this.state != TxnState.Ready) {
        throw LmdbException("Transaction is in an invalid state to perform operation")
    }
}

/**
 * Represents the possible states of a transaction.
 */
enum class TxnState {
    /**
     * The transaction is ready for use in database operations.
     */
    Ready,
    
    /**
     * The transaction has been committed or aborted and is no longer usable.
     */
    Done,
    
    /**
     * The transaction has been reset and needs to be renewed before use.
     */
    Reset,
    
    /**
     * The transaction has been released and is no longer usable.
     */
    Released
}

/**
 * Type alias for a function that compares two Val objects.
 *
 * The function should return:
 * - A negative value if a < b
 * - Zero if a == b
 * - A positive value if a > b
 *
 * This is used for custom key and data comparison functions in databases.
 */
typealias ValCompare = (Val, Val) -> Int


