package lmdb

actual class Val(val mdbVal: MDBVal) {
    actual fun toByteArray() : ByteArray? = mdbVal.toByteArray()

    companion object {
        fun fromMDBVal(mdbVal: MDBVal) : Val {
            return Val(mdbVal)
        }
    }
}

actual fun ByteArray.toVal() : Val {
    return Val(MDBVal.input(this))
}
