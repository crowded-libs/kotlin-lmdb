package lmdb

import lmdb.Library.Companion.MEMORY
import jnr.ffi.Pointer

class MDBVal private constructor(val ptr: Pointer) {
    companion object {
        internal fun input(data: ByteArray) : MDBVal {
            val ptr = MEMORY.allocateTemporary(16, false)
            val dataPtr = MEMORY.allocateDirect(data.size)
            dataPtr.put(0, data, 0, data.size)
            ptr.putLong(0, data.size.toLong())
            ptr.putAddress(Long.SIZE_BYTES.toLong(), dataPtr.address())
            return MDBVal(ptr)
        }

        internal fun output() : MDBVal {
            val ptr = MEMORY.allocateTemporary(16, false)
            return MDBVal(ptr)
        }
        
        /**
         * Create an MDBVal from a JNR pointer
         */
        internal fun fromJNRPointer(pointer: Pointer): MDBVal {
            return MDBVal(pointer)
        }
    }
}

fun MDBVal.toByteArray() : ByteArray {
    val addr = ptr.getAddress(8)
    val size = ptr.getLong(0)
    val pointer = MEMORY.newPointer(addr, size)
    val bytes = ByteArray(size.toInt())
    pointer.get(0, bytes, 0, size.toInt())
    return bytes
}