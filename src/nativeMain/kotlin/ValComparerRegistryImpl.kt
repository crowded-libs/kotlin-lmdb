package lmdb

import kotlinx.cinterop.*

/**
 * Native implementation of ValComparerRegistry
 */
actual object ValComparerRegistry {
    // For Kotlin functions
    private val customComparers = mutableMapOf<ValComparer, ValCompare>()
    
    // For native function pointers 
    private val nativeComparers = mutableMapOf<ValComparer, CPointer<CFunction<(CPointer<MDB_val>?, CPointer<MDB_val>?) -> Int>>>()
    
    /**
     * Register a custom comparison function for a custom slot.
     * For Native platform, this is only usable for the JVM compatibility layer.
     * For Native-specific code, use registerNativeCustomComparer instead.
     */
    actual fun registerCustomComparer(slot: ValComparer, compareFunction: ValCompare) {
        validateCustomSlot(slot)
        customComparers[slot] = compareFunction
    }
    
    /**
     * Register a native comparison function for a custom slot.
     * This should be used on the Native platform instead of registerCustomComparer.
     * 
     * @param slot The custom comparison slot (must be one of CUSTOM_1, CUSTOM_2, CUSTOM_3, or CUSTOM_4)
     * @param nativeFunction The C function pointer created with staticCFunction
     * @throws IllegalArgumentException if the slot is not a custom slot
     */
    fun registerNativeCustomComparer(
        slot: ValComparer, 
        nativeFunction: CPointer<CFunction<(CPointer<MDB_val>?, CPointer<MDB_val>?) -> Int>>
    ) {
        validateCustomSlot(slot)
        nativeComparers[slot] = nativeFunction
    }
    
    /**
     * Get a registered native comparison function.
     * 
     * @param slot The custom comparison slot to retrieve
     * @return The registered native comparison function, or null if not registered
     */
    fun getNativeCustomComparer(
        slot: ValComparer) : CPointer<CFunction<(CPointer<MDB_val>?, CPointer<MDB_val>?) -> Int>>? {
        return nativeComparers[slot]
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
        return nativeComparers.containsKey(slot) || customComparers.containsKey(slot)
    }
    
    /**
     * Check if a native custom slot has been registered.
     */
    fun isNativeCustomComparerRegistered(slot: ValComparer): Boolean {
        return nativeComparers.containsKey(slot)
    }
    
    /**
     * Clear all registered custom comparers.
     */
    actual fun clearCustomComparers() {
        nativeComparers.clear()
        customComparers.clear()
    }
    
    private fun validateCustomSlot(slot: ValComparer) {
        when (slot) {
            ValComparer.CUSTOM_1, ValComparer.CUSTOM_2,
            ValComparer.CUSTOM_3, ValComparer.CUSTOM_4 -> { /* Valid */ }
            else -> throw IllegalArgumentException("Only CUSTOM_1, CUSTOM_2, CUSTOM_3, or CUSTOM_4 slots can be registered")
        }
    }
}