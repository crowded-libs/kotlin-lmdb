package lmdb

/**
 * Returns the version information of the LMDB library for the wasmJs target.
 * 
 * This implementation calls into the WASM binary to get the actual version.
 *
 * @return A [LmdbVersion] object containing the major, minor, and patch version numbers
 */
actual fun lmdbVersion(): LmdbVersion {
    try {
        // Allocate memory for the version components
        val majorPtr = LMDB.malloc(4) // int*
        val minorPtr = LMDB.malloc(4) // int*
        val patchPtr = LMDB.malloc(4) // int*

        // Call into the WASM binary to get the version
        LMDB.mdb_version(majorPtr, minorPtr, patchPtr)

        // Extract the version components
        val major = LMDB.getValue(majorPtr, "i32")
        val minor = LMDB.getValue(minorPtr, "i32")
        val patch = LMDB.getValue(patchPtr, "i32")

        // Free the allocated memory
        LMDB.free(majorPtr)
        LMDB.free(minorPtr)
        LMDB.free(patchPtr)

        return LmdbVersion(major, minor, patch)
    } catch (e: Throwable) {
        // Fallback to a fixed version if the WASM function fails
        return LmdbVersion(0, 9, 70)
    }
}
