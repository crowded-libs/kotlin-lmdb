package lmdb

/**
 * Returns the version information of the LMDB library for the wasmJs target.
 * 
 * This implementation calls into the WASM binary to get the actual version.
 *
 * @return A [LmdbVersion] object containing the major, minor, and patch version numbers
 */
actual fun lmdbVersion(): LmdbVersion {
    var majorPtr = 0
    var minorPtr = 0
    var patchPtr = 0
    try {
        majorPtr = LMDB.malloc(4) // int*
        minorPtr = LMDB.malloc(4) // int*
        patchPtr = LMDB.malloc(4) // int*

        LMDB.mdb_version(majorPtr, minorPtr, patchPtr)

        val major = LMDB.getValue(majorPtr, "i32")
        val minor = LMDB.getValue(minorPtr, "i32")
        val patch = LMDB.getValue(patchPtr, "i32")

        return LmdbVersion(major, minor, patch)
    } catch (e: Throwable) {
        return LmdbVersion(0, 9, 35)
    } finally {
        if (majorPtr != 0) LMDB.free(majorPtr)
        if (minorPtr != 0) LMDB.free(minorPtr)
        if (patchPtr != 0) LMDB.free(patchPtr)
    }
}
