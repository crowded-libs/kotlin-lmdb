package lmdb

/**
 * Returns a string describing the error code for the wasmJs target.
 * 
 * This implementation calls into the WASM binary to get the actual error message.
 *
 * @param result The error code
 * @return A string describing the error
 */
actual fun native_mdb_strerror(result: Int): String {
    // Call into the WASM binary to get the actual error message
    return try {
        LMDB.mdb_strerror(result)
    } catch (e: Throwable) {
        // Fallback to hardcoded error messages if the WASM function fails
        when (result) {
            -30798 -> "MDB_NOTFOUND: Key/data pair not found"
            -30799 -> "MDB_MAP_FULL: Map full"
            -30800 -> "MDB_DBS_FULL: Too many databases"
            -30801 -> "MDB_READERS_FULL: Too many readers"
            -30802 -> "MDB_TXN_FULL: Transaction has too many dirty pages"
            -30803 -> "MDB_CURSOR_FULL: Cursor stack too deep"
            -30804 -> "MDB_PAGE_FULL: Page has not enough space"
            -30805 -> "MDB_MAP_RESIZED: Database contents grew beyond environment map size"
            -30806 -> "MDB_INCOMPATIBLE: Operation and DB incompatible, or DB flags changed"
            -30807 -> "MDB_BAD_RSLOT: Invalid reuse of reader locktable slot"
            -30808 -> "MDB_BAD_TXN: Transaction must abort, has a child, or is invalid"
            -30809 -> "MDB_BAD_VALSIZE: Unsupported size of key/DB name/data, or wrong DUPFIXED size"
            -30810 -> "MDB_BAD_DBI: The specified DBI was changed unexpectedly"
            -30811 -> "MDB_PROBLEM: Unexpected problem"
            else -> "Unknown error: $result"
        }
    }
}