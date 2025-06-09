package lmdb

actual fun native_mdb_strerror(result: Int) : String {
    return LmdbJna.mdb_strerror(result)
}