package lmdb

import kotlinx.cinterop.toKString

actual fun native_mdb_strerror(result: Int) : String {
    return mdb_strerror(result)!!.toKString()
}