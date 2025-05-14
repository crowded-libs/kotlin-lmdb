package lmdb

/**
 * Utility functions for working with WASM memory and strings in the wasmJs target.
 * Following skiko's pattern of handling string conversions without JS dependencies.
 */

/**
 * Helper object for WASM memory operations
 */
object WasmUtils {
    
    // Type constants for getValue/setValue operations
    private const val TYPE_I8 = 0
    private const val TYPE_I16 = 1
    private const val TYPE_I32 = 2

    /**
     * Allocate memory in WASM
     */
    fun malloc(size: Int): Int {
        return _malloc(size)
    }
    
    /**
     * Free memory in WASM
     */
    fun free(ptr: Int) {
        _free(ptr)
    }
    
    /**
     * Read an integer value from WASM memory
     */
    fun getValue(ptr: Int, type: String = "i32"): Int {
        val typeInt = when (type) {
            "i8" -> TYPE_I8
            "i16" -> TYPE_I16
            "i32" -> TYPE_I32
            else -> TYPE_I32
        }
        val result = _getValue(ptr, typeInt)
        
        // Apply proper masking to handle sign extension issues
        return when (type) {
            "i8" -> result and 0xFF  // Mask to unsigned byte
            "i16" -> result and 0xFFFF  // Mask to unsigned short
            else -> result  // i32 doesn't need masking
        }
    }
    
    /**
     * Write an integer value to WASM memory
     */
    fun setValue(ptr: Int, value: Int, type: String = "i32") {
        val typeInt = when (type) {
            "i8" -> TYPE_I8
            "i16" -> TYPE_I16
            "i32" -> TYPE_I32
            else -> TYPE_I32
        }
        _setValue(ptr, value, typeInt)
    }
    
    /**
     * Convert a WASM string pointer to a Kotlin string
     * Reads UTF-8 bytes from memory until null terminator
     */
    fun ptrToString(ptr: Int): String {
        if (ptr == 0) return ""
        
        val bytes = mutableListOf<Byte>()
        var offset = 0
        
        while (true) {
            val byte = getValue(ptr + offset, "i8").toByte()
            if (byte == 0.toByte()) break
            bytes.add(byte)
            offset++
        }
        
        return bytes.toByteArray().decodeToString()
    }
    
    /**
     * Convert a Kotlin string to a WASM string pointer
     * Note: The caller is responsible for freeing the returned pointer
     */
    fun stringToPtr(str: String): Int {
        val bytes = str.encodeToByteArray()
        val ptr = _malloc(bytes.size + 1) // +1 for null terminator
        
        // Write each byte to memory
        bytes.forEachIndexed { index, byte ->
            _setValue(ptr + index, byte.toInt() and 0xFF, TYPE_I8)
        }
        
        // Add null terminator
        _setValue(ptr + bytes.size, 0, TYPE_I8)
        
        return ptr
    }
    
    /**
     * Execute a block with a temporary string pointer
     */
    inline fun <T> withStringPtr(str: String, block: (ptr: Int) -> T): T {
        val ptr = stringToPtr(str)
        try {
            return block(ptr)
        } finally {
            free(ptr)
        }
    }
    
    /**
     * Execute a block with a temporary memory allocation
     */
    inline fun <T> withMemory(size: Int, block: (ptr: Int) -> T): T {
        val ptr = malloc(size)
        try {
            return block(ptr)
        } finally {
            free(ptr)
        }
    }
    
    /**
     * Create a directory in the WASM filesystem
     */
    fun createDirectory(path: String): Boolean {
        return withStringPtr(path) { pathPtr ->
            _mkdir(pathPtr, 493) == 0 // 0755 in octal = 493 in decimal
        }
    }
    
    /**
     * Check if a path exists in the WASM filesystem
     */
    fun pathExists(path: String): Boolean {
        return withStringPtr(path) { pathPtr ->
            _access(pathPtr, 0) == 0
        }
    }
    
    /**
     * Get error message from LMDB error code
     */
    fun getErrorMessage(errorCode: Int): String {
        val msgPtr = _mdb_strerror(errorCode)
        return ptrToString(msgPtr)
    }
    
    /**
     * Mount IDBFS for persistent storage at the given path (browser environments)
     * Returns true if successful, false if IDBFS is not available
     */
    fun mountIDBFS(path: String): Boolean {
        println("Mounting IDBFS at path: $path")
        return try {
            withStringPtr(path) { pathPtr ->
                _mountIDBFS(pathPtr) == 1
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Mount NODEFS for real filesystem access at the given path (Node.js environments)
     * Returns true if successful, false if NODEFS is not available
     */
    fun mountNODEFS(path: String): Boolean {
        println("Mounting NODEFS at path: $path")
        return try {
            withStringPtr(path) { pathPtr ->
                _mountNODEFS(pathPtr) == 1
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Mount the best available filesystem for the current environment
     * Automatically detects environment and chooses NODEFS for Node.js or IDBFS for browser
     * Returns true if successful, false if no persistent filesystem is available
     */
    fun mountBestFilesystem(path: String): Boolean {
        println("Mounting best available filesystem at path: $path")
        return try {
            withStringPtr(path) { pathPtr ->
                _mountFilesystem(pathPtr) == 1
            }
        } catch (e: Exception) {
            false
        }
    }

}