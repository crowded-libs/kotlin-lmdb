package lmdb

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
         * Returns a JNA callback that can be passed to native code
         */
        fun getComparerCallback(comparer: ValComparer): Any {
            val compareFn = when {
                standardComparers.containsKey(comparer) -> standardComparers[comparer]
                isCustomComparer(comparer) -> {
                    ValComparerRegistry.getCustomComparer(comparer) 
                        ?: throw IllegalStateException("Custom comparer $comparer has not been registered")
                }
                else -> throw IllegalArgumentException("Unsupported comparer: $comparer")
            }
            
            // Return a JNA callback implementation
            return object : LmdbLibrary.MDB_cmp_func {
                override fun compare(a: MDB_val, b: MDB_val): Int {
                    val aVal = Val(MDBVal.fromMdbVal(a))
                    val bVal = Val(MDBVal.fromMdbVal(b))
                    return compareFn!!(aVal, bVal)
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