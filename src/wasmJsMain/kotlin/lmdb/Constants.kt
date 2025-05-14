package lmdb

/**
 * Constants for optimizing memory allocations and avoiding repeated string creation
 * in the wasmJs target implementation.
 */
internal object Constants {
    // Memory value types for getValue/setValue operations
    const val TYPE_I32 = "i32"
    const val TYPE_I8 = "i8"
    
    // Common error messages
    const val ERROR_TXN_NOT_READY = "Transaction is not in a ready state"
    const val ERROR_ENV_NOT_OPENED = "Environment must be opened before starting a transaction"
    const val ERROR_KEY_COMPARERS_NOT_IMPLEMENTED = "Custom key comparers are not yet implemented in wasmJs target"
    const val ERROR_DUP_COMPARERS_NOT_IMPLEMENTED = "Custom duplicate comparers are not yet implemented in wasmJs target"
    
    // Memory allocation sizes
    const val POINTER_SIZE = 4        // Size of a pointer in WASM (32-bit)
    const val MDB_VAL_SIZE = 8        // Size of MDB_val structure (size_t + void*)
    const val MDB_STAT_SIZE = 24      // Size of MDB_stat structure (6 * uint32_t)
    const val MDB_ENVINFO_SIZE = 48   // Size of MDB_envinfo structure (6 * uint64_t)
    
    // Memory offsets for MDB_val structure
    const val MDB_VAL_SIZE_OFFSET = 0    // Offset for mv_size field
    const val MDB_VAL_DATA_OFFSET = 4    // Offset for mv_data field
    
    // Common flags and numeric constants
    const val MDB_NODUPDATA = 0x20    // Flag for cursor delete operations
    const val MDB_COMPACT_FLAG = 0x01  // Flag for compact copy operations
    const val SUCCESS_CODE = 0        // Success return code from LMDB operations
}