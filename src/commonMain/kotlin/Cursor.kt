package lmdb
/**
 * Represents a cursor for traversing or modifying a database.
 *
 * A cursor is associated with a specific transaction and database. It provides
 * a way to iterate through the database contents, search for specific items,
 * and perform operations at the current position.
 *
 * The Cursor class wraps the MDB_cursor structure from the native LMDB library.
 *
 * Key characteristics:
 * - Tied to a specific transaction and database
 * - Provides positional access to database contents
 * - Can read, write, or delete data at its current position
 * - Allows iteration through database keys in order
 * - Manages duplicate values for a key when DupSort is enabled
 *
 * Usage example:
 * ```
 * env.beginTxn().use { txn ->
 *     val db = txn.dbiOpen("mydb")
 *     txn.openCursor(db).use { cursor ->
 *         // Iterate through all entries
 *         var result = cursor.first()
 *         while (result.first == 0) {
 *             val key = result.second.toByteArray()
 *             val value = result.third.toByteArray()
 *             println("Key: ${String(key)}, Value: ${String(value)}")
 *             result = cursor.next()
 *         }
 *     }
 * }
 * ```
 *
 * Alternatively, use the sequence API:
 * ```
 * txn.openCursor(db).use { cursor ->
 *     cursor.asSequence().forEach { (status, key, value) ->
 *         println("Key: ${String(key.toByteArray()!!)}, Value: ${String(value.toByteArray()!!)}")
 *     }
 * }
 * ```
 *
 * Note: A cursor cannot be used when its database handle is closed. Nor
 * when its transaction has ended, except with [renew].
 */
expect class Cursor : AutoCloseable {
    /** 
     * Internal implementation for cursor retrieval operations.
     * Users should use the extension functions like [next], [first], etc. instead.
     */
    internal fun get(option: CursorOption): ValResult
    
    /** 
     * Internal implementation for cursor retrieval operations with a key parameter.
     * Users should use the extension functions like [set], [setKey], etc. instead.
     */
    internal fun get(key: Val, option: CursorOption): ValResult
    
    /** 
     * Internal implementation for cursor retrieval operations with key and data parameters.
     * Users should use the extension functions like [getBoth], [getBothRange], etc. instead.
     */
    internal fun get(key: Val, data: Val, option: CursorOption): ValResult
    
    /** 
     * Internal implementation for cursor put operations.
     * Users should use the extension functions like [put], [putCurrent], etc. instead.
     */
    internal fun put(key: Val, data: Val, option: CursorPutOption): ValResult
    
    /**
     * Deletes the key/data pair at the current cursor position.
     *
     * This does not invalidate the cursor, so operations such as [next]
     * can still be used on it. Both [next] and [getCurrent] will return the same
     * record after this operation.
     *
     * @throws LmdbException on errors such as:
     *  - An attempt was made to write in a read-only transaction
     *  - An invalid parameter was specified
     */
    fun delete()
    
    /**
     * Deletes all duplicate data items for the current key.
     *
     * This function is only allowed if the database was opened with [DbiOption.DupSort].
     * The cursor position is unchanged after this operation.
     *
     * @throws LmdbException on errors such as:
     *  - An attempt was made to write in a read-only transaction
     *  - An invalid parameter was specified
     *  - The database wasn't opened with [DbiOption.DupSort]
     */
    fun deleteDuplicateData()
    
    /**
     * Returns the count of duplicate data items for the current key.
     *
     * This call is only valid on databases that support sorted duplicate
     * data items ([DbiOption.DupSort]).
     *
     * @return The number of duplicate data items for the current key
     * 
     * @throws LmdbException on errors such as:
     *  - The cursor is not initialized
     *  - An invalid parameter was specified
     *  - The database wasn't opened with [DbiOption.DupSort]
     */
    fun countDuplicates(): UInt
    
    /**
     * Renews a cursor handle on a new transaction.
     *
     * A cursor is associated with a specific transaction and database.
     * Cursors that are only used in read-only transactions may be reused
     * with this method, to avoid unnecessary allocation overhead.
     * 
     * The cursor may be associated with a new read-only transaction,
     * and referencing the same database handle as it was created with.
     * This may be done whether the previous transaction is live or not.
     *
     * @param txn A transaction handle returned by [Env.beginTxn]
     * 
     * @throws LmdbException on errors such as:
     *  - An invalid parameter was specified
     */
    fun renew(txn: Txn)
    
    /**
     * Closes the cursor handle.
     *
     * The cursor handle will be freed and must not be used again after this call.
     * Its transaction must still be live if it is a write-transaction.
     * 
     * A cursor in a write-transaction can be closed before its transaction ends,
     * and will otherwise be closed when its transaction ends.
     * A cursor in a read-only transaction must be closed explicitly, before
     * or after its transaction ends. It can be reused with [renew] before finally closing it.
     */
    override fun close()
}

/**
 * Positions the cursor at the next key/data pair.
 * 
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.next() = get(CursorOption.NEXT)

/**
 * Positions the cursor at the next data item of the current key.
 * 
 * Only valid for databases with [DbiOption.DupSort].
 * 
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.nextDuplicate() = get(CursorOption.NEXT_DUP)

/**
 * Positions the cursor at the first data item of the next key.
 * 
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.nextNoDuplicate() = get(CursorOption.NEXT_NODUP)

/**
 * Positions the cursor at the previous key/data pair.
 * 
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.previous() = get(CursorOption.PREV)

/**
 * Positions the cursor at the last data item of the previous key.
 * 
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.previousNoDuplicate() = get(CursorOption.PREV_NODUP)

/**
 * Positions the cursor at the previous data item of the current key.
 * 
 * Only valid for databases with [DbiOption.DupSort].
 * 
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.previousDuplicate() = get(CursorOption.PREV_DUP)

/**
 * Positions the cursor at the last key/data pair.
 * 
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.last() = get(CursorOption.LAST)

/**
 * Positions the cursor at the last data item of the current key.
 * 
 * Only valid for databases with [DbiOption.DupSort].
 * 
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.lastDuplicate() = get(CursorOption.LAST_DUP)

/**
 * Positions the cursor at the first key/data pair.
 * 
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.first() = get(CursorOption.FIRST)

/**
 * Positions the cursor at the first data item of the current key.
 * 
 * Only valid for databases with [DbiOption.DupSort].
 * 
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.firstDuplicate() = get(CursorOption.FIRST_DUP)

/**
 * Returns the key/data pair at the current cursor position.
 * 
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.getCurrent() = get(CursorOption.GET_CURRENT)

/**
 * Positions the cursor at the specified key/data pair.
 * 
 * Only valid for databases with [DbiOption.DupSort].
 * 
 * @param key The key to find
 * @param data The data value to find
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.getBoth(key: ByteArray, data: ByteArray) = get(key.toVal(), data.toVal(), CursorOption.GET_BOTH)

/**
 * Positions the cursor at the specified key/data pair.
 * 
 * Only valid for databases with [DbiOption.DupSort].
 * 
 * @param key The key to find
 * @param data The data value to find
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.getBoth(key: Val, data: Val) = get(key, data, CursorOption.GET_BOTH)

/**
 * Positions the cursor at the specified key and the first data value greater than or equal to the specified data.
 * 
 * Only valid for databases with [DbiOption.DupSort].
 * 
 * @param key The key to find
 * @param data The data value to find
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.getBothRange(key: ByteArray, data: ByteArray) = get(key.toVal(), data.toVal(), CursorOption.GET_BOTH_RANGE)

/**
 * Positions the cursor at the specified key and the first data value greater than or equal to the specified data.
 * 
 * Only valid for databases with [DbiOption.DupSort].
 * 
 * @param key The key to find
 * @param data The data value to find
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.getBothRange(key: Val, data: Val) = get(key, data, CursorOption.GET_BOTH_RANGE)

/**
 * Positions the cursor at the specified key.
 * 
 * @param key The key to find
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.set(key: ByteArray) = get(key.toVal(), CursorOption.SET)

/**
 * Positions the cursor at the specified key.
 * 
 * @param key The key to find
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.set(key: Val) = get(key, CursorOption.SET)

/**
 * Positions the cursor at the specified key, returning both the key and data.
 * 
 * @param key The key to find
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.setKey(key: ByteArray) = get(key.toVal(), CursorOption.SET_KEY)

/**
 * Positions the cursor at the specified key, returning both the key and data.
 * 
 * @param key The key to find
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.setKey(key: Val) = get(key, CursorOption.SET_KEY)

/**
 * Positions the cursor at the first key greater than or equal to the specified key.
 * 
 * @param key The key to find
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.setRange(key: ByteArray) = get(key.toVal(), CursorOption.SET_RANGE)

/**
 * Positions the cursor at the first key greater than or equal to the specified key.
 * 
 * @param key The key to find
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.setRange(key: Val) = get(key, CursorOption.SET_RANGE)

/**
 * Stores a new key/data pair at the current cursor position.
 * 
 * The cursor is positioned at the new item, or on failure usually near it.
 *
 * @param key The key to store
 * @param data The data to store
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.put(key: ByteArray, data: ByteArray) = put(key.toVal(), data.toVal(), CursorPutOption.NONE)

/**
 * Stores a new key/data pair at the current cursor position.
 * 
 * The cursor is positioned at the new item, or on failure usually near it.
 *
 * @param key The key to store
 * @param data The data to store
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.put(key: Val, data: Val) = put(key, data, CursorPutOption.NONE)

/**
 * Appends the key/data pair to the end of the database.
 * 
 * This option allows fast bulk loading when keys are already known to be in the
 * correct order. Loading unsorted keys with this flag will cause a MDB_KEYEXIST error.
 *
 * @param key The key to store
 * @param data The data to store
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.putAppend(key: ByteArray, data: ByteArray) = put(key.toVal(), data.toVal(), CursorPutOption.APPEND)

/**
 * Appends the key/data pair to the end of the database.
 * 
 * This option allows fast bulk loading when keys are already known to be in the
 * correct order. Loading unsorted keys with this flag will cause a MDB_KEYEXIST error.
 *
 * @param key The key to store
 * @param data The data to store
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.putAppend(key: Val, data: Val) = put(key, data, CursorPutOption.APPEND)

/**
 * Replaces the data at the current cursor position.
 * 
 * The key parameter must still be provided, and must match the current key. 
 * If using sorted duplicates ([DbiOption.DupSort]), the data item must still sort into the same place.
 * This is intended for when the new data is the same size as the old.
 *
 * @param key The key at the current position
 * @param data The new data to store
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.putCurrent(key: ByteArray, data: ByteArray) = put(key.toVal(), data.toVal(), CursorPutOption.CURRENT)

/**
 * Replaces the data at the current cursor position.
 * 
 * The key parameter must still be provided, and must match the current key. 
 * If using sorted duplicates ([DbiOption.DupSort]), the data item must still sort into the same place.
 * This is intended for when the new data is the same size as the old.
 *
 * @param key The key at the current position
 * @param data The new data to store
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.putCurrent(key: Val, data: Val) = put(key, data, CursorPutOption.CURRENT)

/**
 * Reserves space for data of the given size, but doesn't copy the given data.
 * 
 * Instead, it returns a pointer to the reserved space which the caller can fill in later.
 * This saves an extra memory copy if the data is being generated later.
 * This flag must not be specified if the database was opened with [DbiOption.DupSort].
 *
 * @param key The key to store
 * @param data The data (will be used for size only, not content)
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.putReserve(key: ByteArray, data: ByteArray) = put(key.toVal(), data.toVal(), CursorPutOption.RESERVE)

/**
 * Reserves space for data of the given size, but doesn't copy the given data.
 * 
 * Instead, it returns a pointer to the reserved space which the caller can fill in later.
 * This saves an extra memory copy if the data is being generated later.
 * This flag must not be specified if the database was opened with [DbiOption.DupSort].
 *
 * @param key The key to store
 * @param data The data (will be used for size only, not content)
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.putReserve(key: Val, data: Val) = put(key, data, CursorPutOption.RESERVE)

/**
 * Appends the data as a duplicate for the current key.
 * 
 * Only for databases opened with [DbiOption.DupSort]. This option allows fast bulk loading
 * when duplicate data items are known to be in the correct order.
 *
 * @param key The key to store
 * @param data The data to store
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.putAppendDuplicate(key: ByteArray, data: ByteArray) = put(key.toVal(), data.toVal(), CursorPutOption.APPENDDUP)

/**
 * Appends the data as a duplicate for the current key.
 * 
 * Only for databases opened with [DbiOption.DupSort]. This option allows fast bulk loading
 * when duplicate data items are known to be in the correct order.
 *
 * @param key The key to store
 * @param data The data to store
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.putAppendDuplicate(key: Val, data: Val) = put(key, data, CursorPutOption.APPENDDUP)

/**
 * Stores multiple contiguous data elements in a single request.
 * 
 * This is for use with [DbiOption.DupFixed] databases only.
 *
 * @param key The key to store
 * @param data The data array to store
 * @param size The number of data items to store
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.putMultiple(key: ByteArray, data: ByteArray, size: Int) = put(key.toVal(), data.toVal(), CursorPutOption.MULTIPLE)

/**
 * Stores multiple contiguous data elements in a single request.
 * 
 * This is for use with [DbiOption.DupFixed] databases only.
 *
 * @param key The key to store
 * @param data The data array to store
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.putMultiple(key: Val, data: Val) = put(key, data, CursorPutOption.MULTIPLE)

/**
 * Stores the key/data pair only if the key/data pair does not already exist.
 * 
 * This flag may only be specified if the database was opened with [DbiOption.DupSort].
 * The function will return MDB_KEYEXIST if the key/data pair already exists in the database.
 *
 * @param key The key to store
 * @param data The data to store
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.putNoDuplicateData(key: ByteArray, data: ByteArray) = put(key.toVal(), data.toVal(), CursorPutOption.NODUPDATA)

/**
 * Stores the key/data pair only if the key/data pair does not already exist.
 * 
 * This flag may only be specified if the database was opened with [DbiOption.DupSort].
 * The function will return MDB_KEYEXIST if the key/data pair already exists in the database.
 *
 * @param key The key to store
 * @param data The data to store
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.putNoDuplicateData(key: Val, data: Val) = put(key, data, CursorPutOption.NODUPDATA)

/**
 * Stores the key/data pair only if the key does not already exist.
 * 
 * The function will return MDB_KEYEXIST if the key already exists in the database,
 * even if the database supports duplicates ([DbiOption.DupSort]).
 *
 * @param key The key to store
 * @param data The data to store
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.putNoOverwrite(key: ByteArray, data: ByteArray) = put(key.toVal(), data.toVal(), CursorPutOption.NOOVERWRITE)

/**
 * Stores the key/data pair only if the key does not already exist.
 * 
 * The function will return MDB_KEYEXIST if the key already exists in the database,
 * even if the database supports duplicates ([DbiOption.DupSort]).
 *
 * @param key The key to store
 * @param data The data to store
 * @return A Triple containing (status, key, data). Status is 0 for success, or an error code.
 */
fun Cursor.putNoOverwrite(key: Val, data: Val) = put(key, data, CursorPutOption.NOOVERWRITE)

/**
 * Creates a [Sequence] from this cursor that lazily iterates through all entries.
 * 
 * This is a convenient way to iterate through the database using Kotlin's
 * sequence operations like map, filter, forEach, etc.
 * 
 * The sequence automatically checks for errors and stops iteration when
 * the end of the database is reached or an error occurs.
 *
 * @return A sequence of key/data pairs from the database
 */
fun Cursor.asSequence(): Sequence<ValResult> = sequence {
    var hasNext = true
    while (hasNext) {
        val result = next()
        if (result.resultCode == 0) {
            yield(result)
        } else {
            hasNext = false
            check(result.resultCode)
        }
    }
}