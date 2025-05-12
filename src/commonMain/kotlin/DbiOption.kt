package lmdb

enum class DbiOption(val option: UInt) {

    /**
     * Keys are strings to be compared in reverse order, from the end of the strings
     * to the beginning. By default, Keys are treated as strings and compared from beginning to end.
     */
    ReverseKey(0x02u),

    /**
     * Duplicate keys may be used in the database. (Or, from another perspective, keys may have
     * multiple data items, stored in sorted order.) By default keys must be unique
     * and may have only a single data item.
     */
    DupSort(0x04u),

    /**
     * Keys are binary integers in native byte order, either unsigned int or size_t, and
     * will be sorted as such. The keys must all be of the same size.
     */
    IntegerKey(0x08u),

    /**
     * This flag may only be used in combination with MDB_DUPSORT. This option tells the
     * library that the data items for this database are all the same size, which allows
     * further optimizations in storage and retrieval. When all data items are the same size,
     * the MDB_GET_MULTIPLE and MDB_NEXT_MULTIPLE cursor operations may be used
     * to retrieve multiple items at once.
     */
    DupFixed(0x10u),

    /**
     * This option specifies that duplicate data items are binary integers, similar to MDB_INTEGERKEY keys.
     */
    IntegerDup(0x20u),

    /**
     * This option specifies that duplicate data items should be compared as strings in reverse order.
     */
    ReverseDup(0x40u),

    /**
     * Create the named database if it doesn't exist. This option is not allowed in a read-only
     * transaction or a read-only environment.
     */
    Create(0x40000u)
}

fun Iterable<DbiOption>.toFlags(): UInt =
    fold(0u) { acc, value -> acc or value.option }