/**
 * Represents a key or data value in the LMDB database.
 *
 * Val is a wrapper around the MDB_val structure from the native LMDB library.
 * It encapsulates data with its size, allowing efficient direct access to database entries
 * without requiring extra allocations and copies.
 *
 * The Val class is used for both keys and data values in LMDB operations.
 * Values returned from the database are valid only until a subsequent update operation,
 * or the end of the transaction.
 *
 * Important limitations:
 * - Key sizes must be between 1 and the maxKeySize value (typically 511 bytes)
 * - The same applies to data sizes in databases with the [DbiOption.DupSort] flag
 * - Other data items can theoretically be up to 4GB, though practical limits are lower
 */
expect class Val {
    /**
     * Converts this Val to a ByteArray.
     *
     * For values from the database, this typically creates a copy of the data.
     * 
     * @return The data as a ByteArray, or null if the Val doesn't contain valid data
     */
    fun toByteArray(): ByteArray?
}

/**
 * Converts a ByteArray to a Val.
 *
 * This is used to prepare data for storing in the database or for key lookups.
 *
 * @return A new Val containing the data from this ByteArray
 */
expect fun ByteArray.toVal(): Val

/**
 * Extracts the value (data) from a result Triple returned by database operations.
 *
 * Database operations (like [Txn.get], [Cursor.next], etc.) return a Triple containing
 * status, key, and value. This extension function extracts the value component
 * as a ByteArray, handling the status check.
 *
 * @return The value as a ByteArray, or null if the operation failed (status != 0)
 */
fun ValResult.toValueByteArray() =
    if(this.resultCode == 0) {
       this.data.toByteArray()
    } else {
        null
    }

/**
 * Extracts the key from a result Triple returned by database operations.
 *
 * Database operations (like [Txn.get], [Cursor.next], etc.) return a Triple containing
 * status, key, and value. This extension function extracts the key component
 * as a ByteArray, handling the status check.
 *
 * @return The key as a ByteArray, or null if the operation failed (status != 0)
 */
fun ValResult.toKeyByteArray() =
    if(this.resultCode == 0) {
        this.key.toByteArray()
    } else {
        null
    }

