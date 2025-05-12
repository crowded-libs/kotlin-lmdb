package lmdb

import kotlin.test.*

class ValComparerRegistryTests {
    
    @BeforeTest
    fun setup() {
        // Clear any registered comparers before each test
        ValComparerRegistry.clearCustomComparers()
    }
    
    @Test
    fun `can register and retrieve custom comparer`() {
        // Create a simple comparison function
        val compareFunc: ValCompare = { a, b ->
            val aBytes = a.toByteArray() ?: byteArrayOf()
            val bBytes = b.toByteArray() ?: byteArrayOf()
            aBytes.size - bBytes.size  // Compare by length only
        }
        
        // Register the function
        ValComparerRegistry.registerCustomComparer(ValComparer.CUSTOM_1, compareFunc)
        
        // Verify it's registered
        assertTrue(ValComparerRegistry.isCustomComparerRegistered(ValComparer.CUSTOM_1))
        assertFalse(ValComparerRegistry.isCustomComparerRegistered(ValComparer.CUSTOM_2))
        
        // Retrieve and test the function
        val retrieved = ValComparerRegistry.getCustomComparer(ValComparer.CUSTOM_1)
        assertNotNull(retrieved)
        
        // Test the function works as expected
        val a = byteArrayOf(1).toVal()
        val aa = byteArrayOf(1, 1).toVal()
        
        assertEquals(-1, retrieved(a, aa))
        assertEquals(1, retrieved(aa, a))
        assertEquals(0, retrieved(a, byteArrayOf(2).toVal())) // Same length
    }
    
    @Test
    fun `can register multiple custom comparers`() {
        // Create two different comparison functions
        val lengthCompareFunc: ValCompare = { a, b ->
            val aBytes = a.toByteArray() ?: byteArrayOf()
            val bBytes = b.toByteArray() ?: byteArrayOf()
            aBytes.size - bBytes.size
        }
        
        val firstByteCompareFunc: ValCompare = { a, b ->
            val aBytes = a.toByteArray() ?: byteArrayOf()
            val bBytes = b.toByteArray() ?: byteArrayOf()
            
            if (aBytes.isEmpty() && bBytes.isEmpty()) 0
            else if (aBytes.isEmpty()) -1
            else if (bBytes.isEmpty()) 1
            else aBytes[0] - bBytes[0]
        }
        
        // Register both functions
        ValComparerRegistry.registerCustomComparer(ValComparer.CUSTOM_1, lengthCompareFunc)
        ValComparerRegistry.registerCustomComparer(ValComparer.CUSTOM_2, firstByteCompareFunc)
        
        // Verify both are registered
        assertTrue(ValComparerRegistry.isCustomComparerRegistered(ValComparer.CUSTOM_1))
        assertTrue(ValComparerRegistry.isCustomComparerRegistered(ValComparer.CUSTOM_2))
        
        // Test they work as expected
        val compare1 = ValComparerRegistry.getCustomComparer(ValComparer.CUSTOM_1)
        val compare2 = ValComparerRegistry.getCustomComparer(ValComparer.CUSTOM_2)
        
        assertNotNull(compare1)
        assertNotNull(compare2)
        
        val a = byteArrayOf(1).toVal()
        val b = byteArrayOf(2).toVal()
        val aa = byteArrayOf(1, 1).toVal()
        
        // Length comparison
        assertEquals(0, compare1(a, b))  // Same length
        assertEquals(-1, compare1(a, aa)) // Different length
        
        // First byte comparison
        assertEquals(-1, compare2(a, b)) // 1 vs 2
        assertEquals(0, compare2(a, aa)) // Both start with 1
    }
    
    @Test
    fun `can clear custom comparers`() {
        // Register a comparer
        ValComparerRegistry.registerCustomComparer(ValComparer.CUSTOM_1) { a, b -> 0 }
        
        // Verify it's registered
        assertTrue(ValComparerRegistry.isCustomComparerRegistered(ValComparer.CUSTOM_1))
        
        // Clear all comparers
        ValComparerRegistry.clearCustomComparers()
        
        // Verify it's no longer registered
        assertFalse(ValComparerRegistry.isCustomComparerRegistered(ValComparer.CUSTOM_1))
        assertNull(ValComparerRegistry.getCustomComparer(ValComparer.CUSTOM_1))
    }
    
    @Test
    fun `fails when registering to invalid slot`() {
        // Try to register with a non-custom slot
        assertFailsWith<IllegalArgumentException> {
            ValComparerRegistry.registerCustomComparer(ValComparer.BITWISE) { a, b -> 0 }
        }
    }
    
    @Test
    fun `can overwrite custom comparer`() {
        // Register a comparer
        ValComparerRegistry.registerCustomComparer(ValComparer.CUSTOM_1) { a, b -> 1 }
        
        // Get the initial comparer
        val compare1 = ValComparerRegistry.getCustomComparer(ValComparer.CUSTOM_1)
        assertNotNull(compare1)
        
        val a = byteArrayOf(1).toVal()
        val b = byteArrayOf(2).toVal()
        
        // Test initial behavior
        assertEquals(1, compare1(a, b))
        
        // Override with a new implementation
        ValComparerRegistry.registerCustomComparer(ValComparer.CUSTOM_1) { a, b -> -1 }
        
        // Get the new comparer
        val compare2 = ValComparerRegistry.getCustomComparer(ValComparer.CUSTOM_1)
        assertNotNull(compare2)
        
        // Test new behavior
        assertEquals(-1, compare2(a, b))
    }
}