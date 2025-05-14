package lmdb

/**
 * WASM-WASI implementation of the Val class.
 * This implementation provides a wrapper around a ByteArray for use with WASI.
 */
actual class Val(private val bytes: ByteArray) {
    /**
     * Converts this Val to a ByteArray.
     *
     * @return The data as a ByteArray, or null if the Val doesn't contain valid data
     */
    actual fun toByteArray(): ByteArray? {
        return bytes
    }
    
    /**
     * Converts this Val to a String by decoding the bytes as UTF-8.
     *
     * @return The data as a String, or an empty string if no data
     */
    override fun toString(): String {
        return bytes.decodeToString()
    }
}

/**
 * Converts a ByteArray to a Val.
 *
 * @return A new Val containing the data from this ByteArray
 */
actual fun ByteArray.toVal(): Val {
    return Val(this)
}
