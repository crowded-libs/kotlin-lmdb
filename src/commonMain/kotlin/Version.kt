package lmdb

/**
 * Represents the version information of the LMDB library
 *
 * @property major The major version number
 * @property minor The minor version number
 * @property patch The patch version number
 */
data class LmdbVersion(val major: Int, val minor: Int, val patch: Int) {
    override fun toString(): String = "$major.$minor.$patch"
}

/**
 * Returns the version information of the LMDB library
 *
 * @return A [LmdbVersion] object containing the major, minor, and patch version numbers
 */
expect fun lmdbVersion(): LmdbVersion