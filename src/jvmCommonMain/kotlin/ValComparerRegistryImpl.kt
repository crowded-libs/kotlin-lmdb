package lmdb

/**
 * JVM implementation of ValComparerRegistry
 */
actual object ValComparerRegistry {
    // Map to store registered custom comparers
    private val customComparers = mutableMapOf<ValComparer, ValCompare>()
    
    /**
     * Register a custom comparison function for a custom slot.
     */
    actual fun registerCustomComparer(slot: ValComparer, compareFunction: ValCompare) {
        when (slot) {
            ValComparer.CUSTOM_1, ValComparer.CUSTOM_2, 
            ValComparer.CUSTOM_3, ValComparer.CUSTOM_4 -> {
                customComparers[slot] = compareFunction
            }
            else -> throw IllegalArgumentException("Only CUSTOM_1, CUSTOM_2, CUSTOM_3, or CUSTOM_4 slots can be registered")
        }
    }
    
    /**
     * Get a registered custom comparison function.
     */
    actual fun getCustomComparer(slot: ValComparer): ValCompare? {
        return customComparers[slot]
    }
    
    /**
     * Check if a custom slot has been registered.
     */
    actual fun isCustomComparerRegistered(slot: ValComparer): Boolean {
        return customComparers.containsKey(slot)
    }
    
    /**
     * Clear all registered custom comparers.
     */
    actual fun clearCustomComparers() {
        customComparers.clear()
    }
}