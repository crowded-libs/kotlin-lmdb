package lmdb

enum class PutOption(val option: UInt) {

    /**
     * enter the new key/data pair only if it does not already appear in the database.
     * This flag may only be specified if the database was opened with MDB_DUPSORT.
     * The function will return MDB_KEYEXIST if the key/data pair already appears in the database.
     */
    NoDupData(0x20u),

    /**
     * enter the new key/data pair only if the key does not already appear in the database.
     * The function will return MDB_KEYEXIST if the key already appears in the database,
     * even if the database supports duplicates (MDB_DUPSORT). The data parameter will be set
     * to point to the existing item.
     */
    NoOverwrite(0x10u),

    /**
     * reserve space for data of the given size, but don't copy the given data.
     * Instead, return a pointer to the reserved space, which the caller can fill in
     * later - before the next update operation or the transaction ends. This saves an
     * extra memcpy if the data is being generated later. LMDB does nothing else with this memory,
     * the caller is expected to modify all of the space requested. This flag must not be
     * specified if the database was opened with MDB_DUPSORT.
     */
    Reserve(0x10000u),

    /**
     * append the given key/data pair to the end of the database. This option allows
     * fast bulk loading when keys are already known to be in the correct order.
     * Loading unsorted keys with this flag will cause a MDB_KEYEXIST error.
     */
    Append(0x20000u),

    /**
     * as above, but for sorted dup data.
     * @see PutOption.Append
     */
    AppendDup(0x40000u)
}

fun Iterable<PutOption>.toFlags(): UInt =
    fold(0u) { acc, value -> acc or value.option }