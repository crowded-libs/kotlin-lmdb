package lmdb

import lmdb.Library.Companion.LMDB

actual fun native_mdb_strerror(result: Int) : String {
    return LMDB.mdb_strerror(result)!!
}