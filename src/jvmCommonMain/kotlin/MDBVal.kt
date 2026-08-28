package lmdb

import java.nio.ByteBuffer

class MDBVal internal constructor(val buffer: ByteBuffer) {
    companion object {
        /** Shared empty value for misses and no-data results. */
        internal val EMPTY = MDBVal(ByteBuffer.allocateDirect(0))

        internal fun input(data: ByteArray): MDBVal {
            return MDBVal(ByteBuffer.wrap(data))
        }

        /**
         * Create an MDBVal view from a JNA MDB_val structure. Zero-copy: the buffer wraps
         * the native memory the structure points at (LMDB pages or call scratch), so it is
         * only valid until the next operation on the owning cursor/transaction.
         */
        internal fun fromMdbVal(mdbVal: MDB_val): MDBVal {
            val data = mdbVal.mv_data
            if (data == null || mdbVal.mv_size == 0L) {
                return EMPTY
            }
            return MDBVal(data.getByteBuffer(0, mdbVal.mv_size))
        }
    }

    /**
     * Get the size of the data in this MDBVal
     */
    val size: Int
        get() = buffer.remaining()
}

fun MDBVal.toByteArray() : ByteArray {
    val bytes = ByteArray(buffer.remaining())
    // Save current position
    val pos = buffer.position()
    buffer.get(bytes)
    // Restore position
    buffer.position(pos)
    return bytes
}
