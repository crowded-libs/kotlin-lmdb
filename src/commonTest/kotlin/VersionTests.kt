package lmdb

import kotlin.test.Test
import kotlin.test.assertEquals

class VersionTests {
    @Test
    fun `uses LMDB 1_0_1`() {
        assertEquals(LmdbVersion(1, 0, 1), lmdbVersion())
    }
}
