package lmdb

/**
 * JavaScript implementation of the ValComparerRegistry for the wasmJs target.
 * This implementation provides a simple registry for custom comparison functions.
 */
actual object ValComparerRegistry {
    private val customComparers = mutableMapOf<ValComparer, ValCompare>()

    /**
     * Register a custom comparison function for a custom slot.
     * 
     * @param slot The custom comparison slot (must be one of CUSTOM_1, CUSTOM_2, CUSTOM_3, or CUSTOM_4)
     * @param compareFunction The custom comparison function to register
     * @throws IllegalArgumentException if the slot is not a custom slot
     */
    actual fun registerCustomComparer(slot: ValComparer, compareFunction: ValCompare) {
        if (slot !in listOf(ValComparer.CUSTOM_1, ValComparer.CUSTOM_2, ValComparer.CUSTOM_3, ValComparer.CUSTOM_4)) {
            throw IllegalArgumentException("Only CUSTOM_1, CUSTOM_2, CUSTOM_3, and CUSTOM_4 slots can be registered")
        }
        customComparers[slot] = compareFunction
    }

    /**
     * Get a registered custom comparison function.
     * 
     * @param slot The custom comparison slot to retrieve
     * @return The registered comparison function, or null if not registered
     */
    actual fun getCustomComparer(slot: ValComparer): ValCompare? {
        return customComparers[slot]
    }

    /**
     * Check if a custom slot has been registered.
     * 
     * @param slot The custom comparison slot to check
     * @return true if the slot has been registered, false otherwise
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