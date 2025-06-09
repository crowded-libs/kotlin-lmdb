package lmdb

/**
 * Returns the version information of the LMDB library for JVM platform
 *
 * @return A [LmdbVersion] object containing the major, minor, and patch version numbers
 */
actual fun lmdbVersion(): LmdbVersion {
    val versionString = LmdbJna.mdb_version()
    val parts = versionString.split(".")
    
    if (parts.size != 3) {
        throw LmdbException("Invalid version string format: $versionString")
    }
    
    return LmdbVersion(
        major = parts[0].toIntOrNull() ?: 0,
        minor = parts[1].toIntOrNull() ?: 0,
        patch = parts[2].toIntOrNull() ?: 0
    )
}