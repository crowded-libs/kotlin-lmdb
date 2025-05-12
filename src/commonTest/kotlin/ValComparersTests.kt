package lmdb

import kotlin.test.*

private fun createVal(vararg bytes: Byte): Val {
    val byteArray = ByteArray(bytes.size)
    bytes.copyInto(byteArray)
    return byteArray.toVal()
}

class ValComparersTests {
    private val empty = createVal()
    private val a = createVal(1)
    private val b = createVal(2)
    private val aa = createVal(1, 1)
    private val ab = createVal(1, 2)
    private val ba = createVal(2, 1)
    private val hello = createVal(104, 101, 108, 108, 111) // "hello" as bytes
    private val world = createVal(119, 111, 114, 108, 100) // "world" as bytes
    private val helloWorld = createVal(104, 101, 108, 108, 111, 119, 111, 114, 108, 100) // "helloworld" as bytes
    private val zero = createVal(0)
    private val int1 = createVal(0, 0, 0, 1)
    private val int2 = createVal(0, 0, 0, 2)
    private val int10 = createVal(0, 0, 0, 10)
    private val int256 = createVal(0, 0, 1, 0)

    @Test
    fun `bitwise compare works correctly`() {
        // Equal values
        assertEquals(0, bitwiseCompare(empty, empty))
        assertEquals(0, bitwiseCompare(a, a))
        assertEquals(0, bitwiseCompare(aa, aa))

        // Different values
        assertTrue(bitwiseCompare(a, b) < 0)
        assertTrue(bitwiseCompare(b, a) > 0)
        assertTrue(bitwiseCompare(a, aa) < 0)  // Shorter is less
        assertTrue(bitwiseCompare(aa, a) > 0)  // Longer is greater
        assertTrue(bitwiseCompare(aa, ab) < 0) // Compare by content when same length
        assertTrue(bitwiseCompare(ab, ba) < 0) // First byte dominates
        assertTrue(bitwiseCompare(hello, world) < 0) // Lexicographical comparison
    }

    @Test
    fun `reverse bitwise compare works correctly`() {
        // Should be the exact opposite of bitwiseCompare
        assertEquals(0, reverseBitwiseCompare(empty, empty))
        assertTrue(reverseBitwiseCompare(a, b) > 0)
        assertTrue(reverseBitwiseCompare(b, a) < 0)
        assertTrue(reverseBitwiseCompare(a, aa) > 0)
        assertTrue(reverseBitwiseCompare(aa, a) < 0)
    }

    @Test
    fun `lexicographical string compare works correctly`() {
        // Equal values
        assertEquals(0, lexicographicalStringCompare(empty, empty))
        assertEquals(0, lexicographicalStringCompare(hello, hello))

        // Different values
        assertTrue(lexicographicalStringCompare(hello, world) < 0) // "hello" < "world"
        assertTrue(lexicographicalStringCompare(world, hello) > 0) // "world" > "hello"
        
        // Non-text values still work by interpreting as strings
        assertEquals(0, lexicographicalStringCompare(a, a))
        assertTrue(lexicographicalStringCompare(a, b) < 0)
    }

    @Test
    fun `reverse lexicographical string compare works correctly`() {
        // Should be the exact opposite of lexicographicalStringCompare
        assertEquals(0, reverseLexicographicalStringCompare(empty, empty))
        assertTrue(reverseLexicographicalStringCompare(hello, world) > 0)
        assertTrue(reverseLexicographicalStringCompare(world, hello) < 0)
    }

    @Test
    fun `integer key compare works correctly`() {
        // Equal values
        assertEquals(0, integerKeyCompare(empty, empty))
        assertEquals(0, integerKeyCompare(int1, int1))

        // Different values - treating as integers
        assertTrue(integerKeyCompare(int1, int2) < 0)
        assertTrue(integerKeyCompare(int2, int10) < 0)
        assertTrue(integerKeyCompare(int10, int256) < 0)
        assertTrue(integerKeyCompare(int256, int10) > 0)
        
        // Different length - longer is greater regardless of content
        assertTrue(integerKeyCompare(a, aa) < 0)
        assertTrue(integerKeyCompare(aa, a) > 0)
    }

    @Test
    fun `reverse integer key compare works correctly`() {
        // Should be the exact opposite of integerKeyCompare
        assertEquals(0, reverseIntegerKeyCompare(int1, int1))
        assertTrue(reverseIntegerKeyCompare(int1, int2) > 0)
        assertTrue(reverseIntegerKeyCompare(int2, int1) < 0)
    }

    @Test
    fun `length compare works correctly`() {
        // Equal length, equal content
        assertEquals(0, lengthCompare(empty, empty))
        assertEquals(0, lengthCompare(a, a))
        assertEquals(0, lengthCompare(aa, aa))

        // Equal length, different content - falls back to bitwise comparison
        assertTrue(lengthCompare(a, b) < 0)
        assertTrue(lengthCompare(b, a) > 0)
        assertTrue(lengthCompare(aa, ab) < 0)
        assertTrue(lengthCompare(ab, aa) > 0)

        // Different length - shorter is less
        assertTrue(lengthCompare(a, aa) < 0)
        assertTrue(lengthCompare(aa, a) > 0)
        assertTrue(lengthCompare(hello, helloWorld) < 0)
        assertTrue(lengthCompare(helloWorld, hello) > 0)
    }

    @Test
    fun `reverse length compare works correctly`() {
        // Should be the exact opposite of lengthCompare
        assertEquals(0, reverseLengthCompare(empty, empty))
        assertTrue(reverseLengthCompare(a, aa) > 0) // Shorter is greater
        assertTrue(reverseLengthCompare(aa, a) < 0) // Longer is less
        
        // Equal length - falls back to reverse bitwise comparison
        assertTrue(reverseLengthCompare(a, b) > 0)
        assertTrue(reverseLengthCompare(b, a) < 0)
    }

    @Test
    fun `length only compare works correctly`() {
        // Equal length
        assertEquals(0, lengthOnlyCompare(empty, empty))
        assertEquals(0, lengthOnlyCompare(a, b)) // Different content but same length
        assertEquals(0, lengthOnlyCompare(aa, ab)) // Different content but same length

        // Different length - shorter is less
        assertTrue(lengthOnlyCompare(a, aa) < 0)
        assertTrue(lengthOnlyCompare(aa, a) > 0)
        assertTrue(lengthOnlyCompare(hello, helloWorld) < 0)
        assertTrue(lengthOnlyCompare(helloWorld, hello) > 0)
    }

    @Test
    fun `reverse length only compare works correctly`() {
        // Should be the exact opposite of lengthOnlyCompare
        assertEquals(0, reverseLengthOnlyCompare(empty, empty))
        assertEquals(0, reverseLengthOnlyCompare(a, b)) // Same length
        
        // Different length - longer is less
        assertTrue(reverseLengthOnlyCompare(a, aa) > 0)
        assertTrue(reverseLengthOnlyCompare(aa, a) < 0)
    }

    @Test
    fun `hash code compare works correctly`() {
        // Equal values should return 0
        assertEquals(0, hashCodeCompare(empty, empty))
        assertEquals(0, hashCodeCompare(a, a))
        assertEquals(0, hashCodeCompare(aa, aa))

        // Different values
        val a_simple = createVal(1)
        val b_simple = createVal(2)
        
        // Check consistency of the comparison
        val result1 = hashCodeCompare(a_simple, b_simple)
        val result2 = hashCodeCompare(b_simple, a_simple)
        
        // The results should be of opposite sign
        if (result1 != 0) { // Not equal
            assertTrue(result2 != 0, "Hash comparison should not return 0 for different values")
            assertTrue(result1 * result2 <= 0, "Hash comparisons should have opposite signs or one should be zero")
        }
        
        // Test with more complex values
        val hello_simple = createVal(1, 2, 3)  // Use simple bytes instead of strings
        val world_simple = createVal(3, 2, 1)  // Use simple bytes instead of strings
        
        val hash_hello = hashCodeCompare(hello_simple, world_simple)
        val hash_world = hashCodeCompare(world_simple, hello_simple)
        
        if (hash_hello != 0) { // Skip if they happen to have the same hash
            assertTrue(hash_world != 0, "Hash comparison should not return 0 for different values")
            assertTrue(hash_hello * hash_world <= 0, "Hash comparisons should have opposite signs or one should be zero")
        }
        
        // Check consistency of repeated calls with same values
        assertEquals(result1, hashCodeCompare(a_simple, b_simple), "Hash comparison should be deterministic")
        assertEquals(result2, hashCodeCompare(b_simple, a_simple), "Hash comparison should be deterministic")
    }

    @Test
    fun `reverse hash code compare works correctly`() {
        // Should be the exact opposite of hashCodeCompare
        assertEquals(0, reverseHashCodeCompare(empty, empty))
        
        val a_simple = createVal(1) 
        val b_simple = createVal(2)
        
        val direct = hashCodeCompare(a_simple, b_simple)
        val reverse = reverseHashCodeCompare(a_simple, b_simple)
        
        // Equal values should still return zero in reverse
        assertEquals(0, reverseHashCodeCompare(a_simple, a_simple))
        
        if (direct != 0) {  // Not equal, check opposites
            assertTrue(reverse != 0, "Reverse hash comparison should not return 0 for different values")
            
            // Signs should be opposite (unless one is zero which we already checked)
            assertTrue(direct * reverse <= 0, "Direct and reverse hash comparison should have opposite signs or one should be zero")
            
            // Check if same magnitude or at least same classification
            if (direct > 0) {
                assertTrue(reverse <= 0, "Reverse should be negative or zero when direct is positive")
            } else if (direct < 0) {
                assertTrue(reverse >= 0, "Reverse should be positive or zero when direct is negative")
            }
        }
    }
}