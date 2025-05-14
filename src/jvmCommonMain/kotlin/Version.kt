package lmdb

import jnr.ffi.byref.IntByReference
import lmdb.Library.Companion.LMDB

/**
 * Returns the version information of the LMDB library for JVM platform
 *
 * @return A [LmdbVersion] object containing the major, minor, and patch version numbers
 */
actual fun lmdbVersion(): LmdbVersion {
    val majorRef = IntByReference()
    val minorRef = IntByReference()
    val patchRef = IntByReference()
    
    LMDB.mdb_version(majorRef, minorRef, patchRef)
    
    return LmdbVersion(
        major = majorRef.value,
        minor = minorRef.value,
        patch = patchRef.value
    )
}