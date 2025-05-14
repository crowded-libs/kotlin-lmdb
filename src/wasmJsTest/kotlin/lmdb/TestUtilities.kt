package lmdb

import kotlin.uuid.Uuid

// Console interface for logging in wasmJs
external object console {
    fun error(message: String)
    fun log(message: String)
}

/**
 * Creates a temporary directory for testing in the wasmJs target.
 * This uses the WASM filesystem functions to create a directory in the virtual filesystem.
 * 
 * @return A virtual path string for testing
 */
actual fun pathCreateTestDir(): String {
    val path = "/tmp/" + Uuid.random().toString()
    // Create the directory in the virtual filesystem
    if (WasmUtils.createDirectory(path)) {
        return path
    } else {
        // Fallback if directory creation fails
        console.error("Failed to create test directory: $path")
        return path
    }
}

/**
 * Creates a test environment with a random path in the wasmJs target.
 * 
 * @param open Whether to open the environment
 * @param mapSize The map size to use, or null for default
 * @param maxDatabases The maximum number of databases to allow, or null for default
 * @return A new Env instance
 */
actual fun createRandomTestEnv(open: Boolean, mapSize: ULong?, maxDatabases: UInt?): Env {
    val path = pathCreateTestDir()
    
    val env = Env()
    if (mapSize != null) {
        env.mapSize = mapSize
    }
    if (maxDatabases != null) {
        env.maxDatabases = maxDatabases
    }
    if (open) {
        try {
            env.open(path)
        } catch (e: Exception) {
            console.error("Failed to open environment: ${e.message}")
            throw e
        }
    }
    return env
}

/**
 * JavaScript implementation of the registerCustomComparer function for the wasmJs target.
 * This function registers a custom comparison function with the ValComparerRegistry.
 * 
 * @param slot The custom comparison slot (must be one of CUSTOM_1, CUSTOM_2, CUSTOM_3, or CUSTOM_4)
 * @param compareFunction The custom comparison function to register
 */
actual fun registerCustomComparer(slot: ValComparer, compareFunction: ValCompare) {
    ValComparerRegistry.registerCustomComparer(slot, compareFunction)
}

/**
 * JavaScript implementation of the clearCustomComparers function for the wasmJs target.
 * This function clears all registered custom comparison functions.
 */
actual fun clearCustomComparers() {
    ValComparerRegistry.clearCustomComparers()
}
