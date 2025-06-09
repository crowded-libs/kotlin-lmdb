package lmdb

import java.nio.ByteBuffer
import java.nio.ByteOrder

class MDBVal internal constructor(val buffer: ByteBuffer) {
    companion object {
        internal fun input(data: ByteArray) : MDBVal {
            // Create a direct ByteBuffer with the data
            val dataBuffer = ByteBuffer.allocateDirect(data.size)
            dataBuffer.put(data)
            dataBuffer.flip()
            
            // Return MDBVal with the data buffer
            return MDBVal(dataBuffer)
        }

        internal fun output() : MDBVal {
            // Create an empty direct ByteBuffer that will be filled by LMDB
            // We'll use a reasonable default size that can be resized by JNI if needed
            val buffer = ByteBuffer.allocateDirect(4096)
            buffer.order(ByteOrder.nativeOrder())
            return MDBVal(buffer)
        }
        
        /**
         * Create an MDBVal from a JNA MDB_val structure
         */
        internal fun fromMdbVal(mdbVal: MDB_val): MDBVal {
            if (mdbVal.mv_data == null || mdbVal.mv_size == 0L) {
                return MDBVal(ByteBuffer.allocateDirect(0))
            }
            
            // Create a ByteBuffer from the native pointer
            val buffer = mdbVal.mv_data!!.getByteBuffer(0, mdbVal.mv_size)
            return MDBVal(buffer)
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