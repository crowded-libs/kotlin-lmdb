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

/**
 * Bytes to stage into native scratch for a call. Avoids a copy when the Val wraps a whole
 * heap array (the [toVal] path); falls back to copying out of the buffer otherwise.
 */
internal fun Val.stagingBytes(): ByteArray {
    val b = mdbVal.buffer
    return if (b.hasArray() && b.arrayOffset() == 0 && b.position() == 0 && b.limit() == b.array().size) {
        b.array()
    } else {
        toByteArray() ?: ByteArray(0)
    }
}
