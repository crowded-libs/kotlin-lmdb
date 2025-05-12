package lmdb

import kotlinx.cinterop.*

/**
 * Returns the version information of the LMDB library for Native platform
 *
 * @return A [LmdbVersion] object containing the major, minor, and patch version numbers
 */
actual fun lmdbVersion(): LmdbVersion {
    return memScoped {
        val majorVar = alloc<IntVar>()
        val minorVar = alloc<IntVar>()
        val patchVar = alloc<IntVar>()
        
        mdb_version(majorVar.ptr, minorVar.ptr, patchVar.ptr)
        
        LmdbVersion(
            major = majorVar.value,
            minor = minorVar.value,
            patch = patchVar.value
        )
    }
}