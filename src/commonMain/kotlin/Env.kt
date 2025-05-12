package lmdb
/**
 * Represents an LMDB environment, which is the container for all database operations.
 * 
 * An LMDB environment supports multiple databases, all residing in the same shared-memory map.
 * It must be created before any other operations can be performed. The environment handle
 * is used to configure settings, create transactions, and manage the database lifecycle.
 *
 * The Env class wraps the MDB_env structure from the native LMDB library.
 *
 * Key features:
 * - All databases share the same memory map managed by this environment
 * - Supports multiple simultaneous read transactions
 * - Allows a single write transaction at a time
 * - Provides full ACID semantics
 * - Manages resources like memory maps and lock files
 *
 * Usage example:
 * ```
 * val env = Env()
 * env.maxDatabases = 10u
 * env.mapSize = (10 * 1024 * 1024).toULong() // 10MB
 * env.open("/path/to/database", EnvOption.NoTLS, EnvOption.NoSubDir)
 * 
 * // Create a transaction
 * env.beginTxn().use { txn ->
 *     // Open a database in the environment
 *     val db = txn.dbiOpen("mydb", DbiOption.Create)
 *     // Perform operations...
 *     txn.commit()
 * }
 * 
 * env.close()
 * ```
 *
 * The environment must be properly closed with [close] when no longer needed
 * to release all resources.
 */
expect class Env() : AutoCloseable {

    /**
     * Sets the maximum number of named databases for the environment.
     * 
     * This configuration is only needed if multiple databases will be used in the
     * environment. Simpler applications that use the environment as a single unnamed
     * database can ignore this option.
     *
     * Must be set before calling [open].
     * 
     * Note: A moderate number of databases are inexpensive, but a large number can
     * become costly: 7-120 words per transaction, and database open operations perform
     * a linear search of the opened slots.
     */
    var maxDatabases: UInt
    
    /**
     * Sets the size of the memory map to use for this environment in bytes.
     * 
     * The size should be a multiple of the OS page size. The default is 10MB.
     * The size of the memory map is also the maximum size of the database.
     * The value should be chosen as large as possible, to accommodate future growth
     * of the database.
     *
     * The new size takes effect immediately for the current process but
     * will not be persisted to any others until a write transaction has been
     * committed by the current process.
     *
     * If the mapsize is increased by another process, and data has grown
     * beyond the range of the current mapsize, [Txn.begin] will return an error.
     * This function may be called with a size of zero to adopt the new size.
     *
     * Any attempt to set a size smaller than the space already consumed
     * by the environment will be silently changed to the current size of the used space.
     */
    var mapSize: ULong
    
    /**
     * Sets the maximum number of threads/reader slots for the environment.
     * 
     * This defines the number of slots in the lock table that is used to track readers in the
     * environment. The default is 126.
     *
     * Starting a read-only transaction normally ties a lock table slot to the
     * current thread until the environment closes or the thread exits. If
     * [EnvOption.NoTLS] is in use, transaction begin ties the slot to the
     * transaction object until it or the environment object is destroyed.
     * 
     * Must be set before calling [open].
     */
    var maxReaders: UInt
    
    /**
     * Returns the maximum size of keys and MDB_DUPSORT data we can write.
     * 
     * This is a limit of the LMDB database that can't be changed and is determined
     * at compile time. The default is typically 511 bytes.
     * 
     * This represents an important constraint: keys and duplicate data values have
     * a maximum size limited by this value. Other data values can theoretically be
     * up to 4GB, though practical limits are much lower.
     */
    val maxKeySize: UInt
    
    /**
     * Returns the number of stale readers that were cleared.
     *
     * Stale readers are transactions that were not properly closed and are still
     * taking up space in the lock table. This property checks for such stale entries
     * and removes them, returning the count of entries that were removed.
     *
     * Long-lived transactions prevent reuse of pages freed by newer transactions,
     * causing the database to grow quickly. This is especially problematic if
     * a process with active transactions is aborted.
     * 
     * @return The number of stale readers that were cleared from the lock table
     */
    val staleReaderCount: UInt
    
    /**
     * Returns statistics about the LMDB environment.
     * 
     * The statistics include page size, B-tree depth, and counts of various page types.
     * Returns null if the environment is not open.
     */
    val stat: Stat?
    
    /**
     * Returns information about the LMDB environment.
     * 
     * The information includes memory map address and size, last page number,
     * last transaction ID, and reader counts.
     * Returns null if the environment is not open.
     */
    val info: EnvInfo?
    
    /**
     * Gets or sets the environment flags.
     * 
     * This allows setting or unsetting flags after the environment has been opened.
     * Not all flags can be changed after opening. See the flag descriptions in [EnvOption]
     * for details on which flags can be changed.
     */
    var flags: Set<EnvOption>

    /**
     * Opens this environment at the provided [path] with the specified [options].
     *
     * If this function fails, [close] must be called to discard the [Env] handle.
     * Various other options (like [maxDatabases], [maxReaders], and [mapSize]) should be
     * set before calling this method, depending on your requirements.
     * 
     * @param path The directory in which the database files reside. 
     *             This directory must already exist and be writable.
     * @param options Special options for this environment. See [EnvOption] for available flags.
     * @param mode The UNIX permissions to set on created files and semaphores (default "0664").
     *             This parameter is ignored on Windows.
     *
     * @throws LmdbException on errors such as:
     *  - Version mismatch between the LMDB library version that created the database environment
     *  - Invalid or corrupted environment file headers
     *  - Path does not exist or is not accessible
     *  - Environment is locked by another process
     */
    fun open(path: String, vararg options: EnvOption, mode: String = "0664")

    /**
     * Creates a transaction for use with the environment.
     *
     * A transaction is required for all database operations. Transactions can be read-only
     * or read-write. Multiple read-only transactions can run simultaneously, but only one
     * write transaction can be active at a time.
     * 
     * Note: A transaction and its cursors must only be used by a single thread.
     * A thread may only have a single transaction at a time unless using nested transactions.
     * If [EnvOption.NoTLS] is in use, this does not apply to read-only transactions.
     * 
     * @param options Special options for this transaction, such as [TxnOption.ReadOnly]
     * @return A new transaction handle
     * 
     * @throws LmdbException on errors such as:
     *  - A fatal error occurred earlier and the environment must be shut down
     *  - Another process wrote data beyond this environment's mapsize and the map must be resized
     *  - A read-only transaction was requested and the reader lock table is full
     *  - Out of memory
     */
    fun beginTxn(vararg options: TxnOption) : Txn

    /**
     * Copies the environment to the specified path with optional compaction.
     * 
     * This function may be used to make a backup of an existing environment.
     * No lockfile is created, since it gets recreated as needed.
     * 
     * Warning: This call can trigger significant file size growth if run in
     * parallel with write transactions, because it employs a read-only
     * transaction which prevents reuse of free pages.
     * 
     * @param path The directory where the copy will be stored.
     *             This directory must already exist and be writable but must otherwise be empty.
     * @param compact If true, perform compaction while copying: omit free pages and
     *                sequentially renumber all pages in output. This option consumes more CPU
     *                and runs more slowly than the default.
     *
     * @throws LmdbException if the copy operation fails
     */
    fun copyTo(path: String, compact: Boolean = false)
    
    /**
     * Copies the environment to the specified path.
     * 
     * This is a simpler version of [copyTo] that doesn't support compaction.
     * 
     * @param path The directory where the copy will be stored.
     *             This directory must already exist and be writable but must otherwise be empty.
     *
     * @throws LmdbException if the copy operation fails
     */
    fun copyTo(path: String)

    /**
     * Flushes the data buffers to disk.
     *
     * Data is always written to disk when [Txn.commit] is called,
     * but the operating system may keep it buffered. LMDB always flushes the OS buffers
     * upon commit as well, unless the environment was opened with [EnvOption.NoSync]
     * or in part [EnvOption.NoMetaSync].
     * 
     * This call is not valid if the environment was opened with [EnvOption.ReadOnly].
     *
     * @param force If true, force a synchronous flush. Otherwise
     *              if the environment has the [EnvOption.NoSync] flag set the flushes
     *              will be omitted, and with [EnvOption.MapAsync] they will be asynchronous.
     *
     * @throws LmdbException on errors such as:
     *  - The environment is read-only
     *  - An invalid parameter was specified
     *  - An error occurred during synchronization
     */
    fun sync(force: Boolean)

    /**
     * Closes the environment and releases the memory map.
     *
     * Only a single thread may call this function. All transactions, databases,
     * and cursors must already be closed before calling this function. Attempts to
     * use any such handles after calling this function will cause undefined behavior.
     * The environment handle will be freed and must not be used again after this call.
     */
    override fun close()
}

/**
 * Opens a transaction, executes the given block, and automatically closes the transaction.
 *
 * This extension function provides a convenient way to use transactions with Kotlin's
 * scoping functions. It ensures proper resource management by automatically closing
 * the transaction when the block completes, either normally or with an exception.
 *
 * Usage example:
 * ```
 * env.beginTxn { txn ->
 *     val db = txn.dbiOpen("mydb", DbiOption.Create)
 *     txn.put(db, "key".toByteArray().toVal(), "value".toByteArray().toVal())
 *     txn.commit()
 * }
 * ```
 *
 * @param options Special options for this transaction, such as [TxnOption.ReadOnly]
 * @param block The code block to execute within the transaction
 */
inline fun Env.beginTxn(vararg options: TxnOption, crossinline block: Txn.() -> Unit) {
    beginTxn(*options).use {
        block(it)
    }
}
