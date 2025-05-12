package lmdb

import kotlinx.cinterop.toKString
import kotlin.test.Test
import kotlin.test.assertEquals

class InteropTests {

    @Test
    fun `can pin and retrieve values from MDB_val`() {
        val original = "original value"
        val mdbVal = Val.input(original.encodeToByteArray())
        val newValue = mdbVal.toByteArray()
        assertEquals(original, newValue?.toKString())
    }
}
