package lmdb

import kotlin.test.*

/**
 * Tests for custom comparers that work across both JVM and Native platforms
 * using the expect/actual pattern for platform-specific registration
 */
class CustomComparerTests {
    
    @Test
    fun `can register and use custom comparers with database`() {
        val env = createRandomTestEnv(maxDatabases = 10u)
        env.use {
            // Clear any existing custom comparers before test
            clearCustomComparers()
            
            try {
                // Register a custom comparer that sorts by first byte only
                val firstByteComparer: ValCompare = { a, b -> 
                    val aBytes = a.toByteArray() ?: byteArrayOf()
                    val bBytes = b.toByteArray() ?: byteArrayOf()
                    
                    val aFirstByte = if (aBytes.isNotEmpty()) aBytes[0].toInt() else 0
                    val bFirstByte = if (bBytes.isNotEmpty()) bBytes[0].toInt() else 0
                    
                    aFirstByte - bFirstByte
                }

                registerCustomComparer(ValComparer.CUSTOM_1, firstByteComparer)
                
                env.beginTxn {
                    // Use the custom comparer in a database config
                    val config = DbiConfig(keyComparer = ValComparer.CUSTOM_1)
                    val dbi = dbiOpen("custom-comparer-db", config, DbiOption.Create)
                    
                    // Add keys with different first bytes but same length
                    val keyA = "abc".encodeToByteArray() // First byte is 'a' (97)
                    val keyB = "bcd".encodeToByteArray() // First byte is 'b' (98)
                    val keyZ = "zde".encodeToByteArray() // First byte is 'z' (122)
                    val keySpace = " ef".encodeToByteArray() // First byte is space (32)
                    
                    // Add keys in non-sorted order
                    put(dbi, keyZ, "value-z".encodeToByteArray())
                    put(dbi, keyB, "value-b".encodeToByteArray())
                    put(dbi, keyA, "value-a".encodeToByteArray())
                    put(dbi, keySpace, "value-space".encodeToByteArray())
                    
                    // When we iterate, they should be sorted by first byte only
                    val cursor = openCursor(dbi)
                    cursor.use {
                        // Space comes first in ASCII
                        val firstResult = cursor.first()
                        assertEquals(0, firstResult.resultCode)
                        assertContentEquals(keySpace, firstResult.key.toByteArray()!!)
                        
                        // Then 'a'
                        val secondResult = cursor.next()
                        assertEquals(0, secondResult.resultCode)
                        assertContentEquals(keyA, secondResult.key.toByteArray()!!)
                        
                        // Then 'b'
                        val thirdResult = cursor.next()
                        assertEquals(0, thirdResult.resultCode)
                        assertContentEquals(keyB, thirdResult.key.toByteArray()!!)
                        
                        // Then 'z'
                        val fourthResult = cursor.next()
                        assertEquals(0, fourthResult.resultCode)
                        assertContentEquals(keyZ, fourthResult.key.toByteArray()!!)
                    }
                    
                    dbi.close()
                }
            } finally {
                // Clean up custom comparers
                clearCustomComparers()
            }
        }
    }
}