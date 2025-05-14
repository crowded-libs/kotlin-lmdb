package lmdb

import jnr.ffi.Pointer

// The actual implementation of ValComparerRegistry is in ValComparerRegistryImpl.kt

/**
 * Implementation of comparison function callbacks for JVM platform
 */
internal class ValComparerImpl {
    companion object {
        /**
         * Map of ValComparer enum to the actual implementation
         */
        private val standardComparers = mapOf(
            ValComparer.BITWISE to ::bitwiseCompare,
            ValComparer.REVERSE_BITWISE to ::reverseBitwiseCompare,
            ValComparer.LEXICOGRAPHIC_STRING to ::lexicographicalStringCompare,
            ValComparer.REVERSE_LEXICOGRAPHIC_STRING to ::reverseLexicographicalStringCompare,
            ValComparer.INTEGER_KEY to ::integerKeyCompare,
            ValComparer.REVERSE_INTEGER_KEY to ::reverseIntegerKeyCompare,
            ValComparer.LENGTH to ::lengthCompare,
            ValComparer.REVERSE_LENGTH to ::reverseLengthCompare,
            ValComparer.LENGTH_ONLY to ::lengthOnlyCompare,
            ValComparer.REVERSE_LENGTH_ONLY to ::reverseLengthOnlyCompare,
            ValComparer.HASH_CODE to ::hashCodeCompare,
            ValComparer.REVERSE_HASH_CODE to ::reverseHashCodeCompare
        )

        /**
         * Get the JVM-specific comparator callback for given ValComparer
         */
        fun getComparerCallback(comparer: ValComparer): Library.ComparatorCallback {
            val compareFn = when {
                standardComparers.containsKey(comparer) -> standardComparers[comparer]
                isCustomComparer(comparer) -> {
                    ValComparerRegistry.getCustomComparer(comparer) 
                        ?: throw IllegalStateException("Custom comparer $comparer has not been registered")
                }
                else -> throw IllegalArgumentException("Unsupported comparer: $comparer")
            }
            
            return object : Library.ComparatorCallback {
                override fun compare(keyA: Pointer?, keyB: Pointer?): Int {
                    val a = Val.fromMDBVal(MDBVal.fromJNRPointer(keyA!!))
                    val b = Val.fromMDBVal(MDBVal.fromJNRPointer(keyB!!))
                    return compareFn!!(a, b)
                }
            }
        }
        
        /**
         * Check if the given comparer is a custom comparer slot
         */
        private fun isCustomComparer(comparer: ValComparer): Boolean {
            return comparer == ValComparer.CUSTOM_1 ||
                   comparer == ValComparer.CUSTOM_2 ||
                   comparer == ValComparer.CUSTOM_3 ||
                   comparer == ValComparer.CUSTOM_4
        }
    }
}