package lmdb

import kotlin.test.Test
import kotlin.test.assertEquals

class VersionTests {
    @Test
    fun `uses LMDB 0_9_35`() {
        assertEquals(LmdbVersion(0, 9, 35), lmdbVersion())
    }
}
