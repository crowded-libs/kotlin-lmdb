package lmdb

import kotlinx.cinterop.*

// The actual implementation of ValComparerRegistry is in ValComparerRegistryImpl.kt

/**
 * Implementation of comparison function callbacks for Native platform
 */
internal class ValComparerImpl {
    companion object {
        /**
         * Map of ValComparer enum to the actual implementation
         */
        private val standardComparers = mapOf(
            ValComparer.BITWISE to staticCFunction(::bitwiseCompareNative),
            ValComparer.REVERSE_BITWISE to staticCFunction(::reverseBitwiseCompareNative),
            ValComparer.LEXICOGRAPHIC_STRING to staticCFunction(::lexicographicalStringCompareNative),
            ValComparer.REVERSE_LEXICOGRAPHIC_STRING to staticCFunction(::reverseLexicographicalStringCompareNative),
            ValComparer.INTEGER_KEY to staticCFunction(::integerKeyCompareNative),
            ValComparer.REVERSE_INTEGER_KEY to staticCFunction(::reverseIntegerKeyCompareNative),
            ValComparer.LENGTH to staticCFunction(::lengthCompareNative),
            ValComparer.REVERSE_LENGTH to staticCFunction(::reverseLengthCompareNative),
            ValComparer.LENGTH_ONLY to staticCFunction(::lengthOnlyCompareNative),
            ValComparer.REVERSE_LENGTH_ONLY to staticCFunction(::reverseLengthOnlyCompareNative),
            ValComparer.HASH_CODE to staticCFunction(::hashCodeCompareNative),
            ValComparer.REVERSE_HASH_CODE to staticCFunction(::reverseHashCodeCompareNative)
        )

        /**
         * Get a native CFunction for the LMDB comparison callback
         */
        fun getComparerCallback(comparer: ValComparer): CPointer<CFunction<(CPointer<MDB_val>?, CPointer<MDB_val>?) -> Int>> {
            return when {
                standardComparers.containsKey(comparer) -> standardComparers[comparer]!!
                isCustomComparer(comparer) -> {
                    // Get native comparer from registry
                    ValComparerRegistry.getNativeCustomComparer(comparer)
                        ?: throw IllegalStateException(
                            "Custom comparer $comparer must be registered with " +
                            "ValComparerRegistry.registerNativeCustomComparer() before use"
                        )
                }
                else -> throw IllegalArgumentException("Unsupported comparer: $comparer")
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

fun bitwiseCompareNative(left: CPointer<MDB_val>?, right: CPointer<MDB_val>?): Int {
    val a = Val.forCompare(left)
    val b = Val.forCompare(right)
    return bitwiseCompare(a, b)
}

fun reverseBitwiseCompareNative(left: CPointer<MDB_val>?, right: CPointer<MDB_val>?): Int {
    val a = Val.forCompare(left)
    val b = Val.forCompare(right)
    return reverseBitwiseCompare(a, b)
}

fun lexicographicalStringCompareNative(left: CPointer<MDB_val>?, right: CPointer<MDB_val>?): Int {
    val a = Val.forCompare(left)
    val b = Val.forCompare(right)
    return lexicographicalStringCompare(a, b)
}

fun reverseLexicographicalStringCompareNative(left: CPointer<MDB_val>?, right: CPointer<MDB_val>?): Int {
    val a = Val.forCompare(left)
    val b = Val.forCompare(right)
    return reverseLexicographicalStringCompare(a, b)
}

fun integerKeyCompareNative(left: CPointer<MDB_val>?, right: CPointer<MDB_val>?): Int {
    val a = Val.forCompare(left)
    val b = Val.forCompare(right)
    return integerKeyCompare(a, b)
}

fun reverseIntegerKeyCompareNative(left: CPointer<MDB_val>?, right: CPointer<MDB_val>?): Int {
    val a = Val.forCompare(left)
    val b = Val.forCompare(right)
    return reverseIntegerKeyCompare(a, b)
}

fun lengthCompareNative(left: CPointer<MDB_val>?, right: CPointer<MDB_val>?): Int {
    val a = Val.forCompare(left)
    val b = Val.forCompare(right)
    return lengthCompare(a, b)
}

fun reverseLengthCompareNative(left: CPointer<MDB_val>?, right: CPointer<MDB_val>?): Int {
    val a = Val.forCompare(left)
    val b = Val.forCompare(right)
    return reverseLengthCompare(a, b)
}

fun lengthOnlyCompareNative(left: CPointer<MDB_val>?, right: CPointer<MDB_val>?): Int {
    val a = Val.forCompare(left)
    val b = Val.forCompare(right)
    return lengthOnlyCompare(a, b)
}

fun reverseLengthOnlyCompareNative(left: CPointer<MDB_val>?, right: CPointer<MDB_val>?): Int {
    val a = Val.forCompare(left)
    val b = Val.forCompare(right)
    return reverseLengthOnlyCompare(a, b)
}

fun hashCodeCompareNative(left: CPointer<MDB_val>?, right: CPointer<MDB_val>?): Int {
    val a = Val.forCompare(left)
    val b = Val.forCompare(right)
    return hashCodeCompare(a, b)
}

fun reverseHashCodeCompareNative(left: CPointer<MDB_val>?, right: CPointer<MDB_val>?): Int {
    val a = Val.forCompare(left)
    val b = Val.forCompare(right)
    return reverseHashCodeCompare(a, b)
}