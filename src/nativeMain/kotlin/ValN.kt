package lmdb

import kotlinx.cinterop.*
import kotlin.native.ref.createCleaner
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.Cleaner

@OptIn(ExperimentalNativeApi::class)
actual class Val(val mdbVal: MDB_val, private val arena: Arena?) {
    private var cleaner: Cleaner? = null
    init {
        if (arena != null) {
            cleaner = createCleaner(this, Val::clean)
        }
    }

    fun clean() {
        arena?.clear()
    }

    @OptIn(UnsafeNumber::class)
    actual fun toByteArray() : ByteArray? {
        val size = mdbVal.mv_size.toInt()
        return this.mdbVal.mv_data?.readBytes(size)
    }

    companion object {
        fun output() : Val {
            val arena = Arena()
            val mdbVal = arena.alloc<MDB_val>()
            return Val(mdbVal, arena)
        }

        @OptIn(UnsafeNumber::class)
        fun input(byteArray: ByteArray): Val = memScoped {
            val arena = Arena()
            return byteArray.usePinned { pinned ->
                val mdbVal = arena.alloc<MDB_val> {
                    mv_data = pinned.startAddressOf
                    mv_size = byteArray.size.convert()
                }
                Val(mdbVal, arena)
            }
        }

        fun forCompare(mdbVal: MDB_val): Val = memScoped {
            Val(mdbVal, null)
        }
        
        fun forCompare(mdbValPtr: CPointer<MDB_val>?): Val {
            return forCompare(checkNotNull(mdbValPtr).pointed)
        }
    }
}

actual fun ByteArray.toVal() : Val {
    return Val.input(this)
}

private val emptyPinnedByte = ByteArray(1).pin()
val Pinned<ByteArray>.startAddressOf: CPointer<ByteVar> get() = if (this.get().isNotEmpty()) this.addressOf(0) else emptyPinnedByte.addressOf(0)