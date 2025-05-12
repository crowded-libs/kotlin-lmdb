package lmdb

/**
 * Registry of available Val comparison functions.
 * These enums represent named implementations that can be used
 * for the key comparison functions when opening a database.
 */
enum class ValComparer {
    /**
     * Default bitwise byte array comparison.
     * Compares values byte by byte in ascending order.
     */
    BITWISE,

    /**
     * Reverse bitwise byte array comparison.
     * Compares values byte by byte in descending order.
     */
    REVERSE_BITWISE,
    
    /**
     * Lexicographical string comparison.
     * Treats values as UTF-8 strings and compares them in ascending order.
     */
    LEXICOGRAPHIC_STRING,

    /**
     * Reverse lexicographical string comparison.
     * Treats values as UTF-8 strings and compares them in descending order.
     */
    REVERSE_LEXICOGRAPHIC_STRING,

    /**
     * Integer key comparison.
     * Treats values as integers and compares them in ascending order.
     */
    INTEGER_KEY,

    /**
     * Reverse integer key comparison.
     * Treats values as integers and compares them in descending order.
     */
    REVERSE_INTEGER_KEY,
    
    /**
     * Length-based comparison.
     * Compares values based on their length (shorter first) 
     * and if equal, by bitwise content.
     */
    LENGTH,
    
    /**
     * Reverse length-based comparison.
     * Compares values based on their length (longer first)
     * and if equal, by bitwise content.
     */
    REVERSE_LENGTH,
    
    /**
     * Length-only comparison.
     * Compares values solely based on their length (shorter first),
     * ignoring the actual content.
     */
    LENGTH_ONLY,
    
    /**
     * Reverse length-only comparison.
     * Compares values solely based on their length (longer first),
     * ignoring the actual content.
     */
    REVERSE_LENGTH_ONLY,
    
    /**
     * Hash-based comparison.
     * Compares values based on their hashCode.
     * Note that this may lead to collisions and inconsistent sort order
     * but can be faster for large values.
     */
    HASH_CODE,
    
    /**
     * Reverse hash-based comparison.
     * Compares values based on their hashCode in reverse order.
     */
    REVERSE_HASH_CODE,
    
    /**
     * Custom comparison slot 1.
     * This must be registered with a custom comparison function
     * using the ValComparerRegistry before use.
     */
    CUSTOM_1,
    
    /**
     * Custom comparison slot 2.
     * This must be registered with a custom comparison function
     * using the ValComparerRegistry before use.
     */
    CUSTOM_2,
    
    /**
     * Custom comparison slot 3.
     * This must be registered with a custom comparison function
     * using the ValComparerRegistry before use.
     */
    CUSTOM_3,
    
    /**
     * Custom comparison slot 4.
     * This must be registered with a custom comparison function
     * using the ValComparerRegistry before use.
     */
    CUSTOM_4
}

/**
 * Implementation of bitwise byte comparison
 */
fun bitwiseCompare(a: Val, b: Val): Int {
    val aBytes = a.toByteArray() ?: return -1
    val bBytes = b.toByteArray() ?: return 1
    
    val minLength = minOf(aBytes.size, bBytes.size)
    
    for (i in 0 until minLength) {
        val diff = (aBytes[i].toInt() and 0xFF) - (bBytes[i].toInt() and 0xFF)
        if (diff != 0) {
            return diff
        }
    }
    
    return aBytes.size - bBytes.size
}

/**
 * Implementation of reverse bitwise byte comparison
 */
fun reverseBitwiseCompare(a: Val, b: Val): Int {
    return -bitwiseCompare(a, b)
}

/**
 * Implementation of lexicographical string comparison
 */
fun lexicographicalStringCompare(a: Val, b: Val): Int {
    val aBytes = a.toByteArray() ?: return -1
    val bBytes = b.toByteArray() ?: return 1
    
    // Get String representations - comparing byte by byte for safety
    val minLength = minOf(aBytes.size, bBytes.size)
    
    // First compare byte by byte
    for (i in 0 until minLength) {
        val diff = (aBytes[i].toInt() and 0xFF) - (bBytes[i].toInt() and 0xFF)
        if (diff != 0) {
            return diff
        }
    }
    
    // If all bytes are equal up to the minimum length, the shorter array is "less than"
    return aBytes.size - bBytes.size
}

/**
 * Implementation of reverse lexicographical string comparison
 */
fun reverseLexicographicalStringCompare(a: Val, b: Val): Int {
    return -lexicographicalStringCompare(a, b)
}

/**
 * Implementation of integer key comparison
 */
fun integerKeyCompare(a: Val, b: Val): Int {
    val aBytes = a.toByteArray() ?: return -1
    val bBytes = b.toByteArray() ?: return 1
    
    // Compare by size first
    if (aBytes.size != bBytes.size) {
        return aBytes.size - bBytes.size
    }
    
    // Compare byte by byte, most significant byte first
    for (i in 0 until aBytes.size) {
        val diff = (aBytes[i].toInt() and 0xFF) - (bBytes[i].toInt() and 0xFF)
        if (diff != 0) {
            return diff
        }
    }
    
    return 0
}

/**
 * Implementation of reverse integer key comparison
 */
fun reverseIntegerKeyCompare(a: Val, b: Val): Int {
    return -integerKeyCompare(a, b)
}

/**
 * Implementation of length-based comparison.
 * Compares values based on their length first, then by content.
 */
fun lengthCompare(a: Val, b: Val): Int {
    val aBytes = a.toByteArray() ?: return -1
    val bBytes = b.toByteArray() ?: return 1
    
    // First compare by length
    val lengthComparison = aBytes.size - bBytes.size
    if (lengthComparison != 0) {
        return lengthComparison
    }
    
    // If lengths are equal, compare content
    return bitwiseCompare(a, b)
}

/**
 * Implementation of reverse length-based comparison.
 * Compares values based on their length first (longer first), then by content.
 */
fun reverseLengthCompare(a: Val, b: Val): Int {
    return -lengthCompare(a, b)
}

/**
 * Implementation of length-only comparison.
 * Compares values solely based on their length, ignoring content.
 */
fun lengthOnlyCompare(a: Val, b: Val): Int {
    val aBytes = a.toByteArray() ?: return -1
    val bBytes = b.toByteArray() ?: return 1
    
    return aBytes.size - bBytes.size
}

/**
 * Implementation of reverse length-only comparison.
 * Compares values solely based on their length (longer first), ignoring content.
 */
fun reverseLengthOnlyCompare(a: Val, b: Val): Int {
    return -lengthOnlyCompare(a, b)
}

/**
 * Implementation of hash-based comparison.
 * Compares values based on their hashCode.
 */
fun hashCodeCompare(a: Val, b: Val): Int {
    val aBytes = a.toByteArray() ?: return -1
    val bBytes = b.toByteArray() ?: return 1
    
    // If byte arrays are identical, return 0 immediately
    if (aBytes.contentEquals(bBytes)) {
        return 0
    }
    
    val aHash = aBytes.contentHashCode()
    val bHash = bBytes.contentHashCode()
    
    // If hashes are different, use them to determine order
    if (aHash != bHash) {
        // Avoid integer overflow by using sign test rather than subtraction
        return if (aHash < bHash) -1 else 1
    }
    
    // If hashes are the same but values are different (collision),
    // fall back to bitwise comparison for consistency
    return bitwiseCompare(a, b)
}

/**
 * Implementation of reverse hash-based comparison.
 */
fun reverseHashCodeCompare(a: Val, b: Val): Int {
    return -hashCodeCompare(a, b)
}

/**
 * Registry for registering custom comparison functions.
 * This allows users to register their own comparison logic for the CUSTOM_x slots.
 * 
 * Note: For native platform, you need to register platform-specific native functions
 * using registerNativeCustomComparer() instead of this common version.
 */
expect object ValComparerRegistry {
    /**
     * Register a custom comparison function for a custom slot.
     * 
     * @param slot The custom comparison slot (must be one of CUSTOM_1, CUSTOM_2, CUSTOM_3, or CUSTOM_4)
     * @param compareFunction The custom comparison function to register
     * @throws IllegalArgumentException if the slot is not a custom slot
     */
    fun registerCustomComparer(slot: ValComparer, compareFunction: ValCompare)
    
    /**
     * Get a registered custom comparison function.
     * 
     * @param slot The custom comparison slot to retrieve
     * @return The registered comparison function, or null if not registered
     */
    fun getCustomComparer(slot: ValComparer): ValCompare?
    
    /**
     * Check if a custom slot has been registered.
     * 
     * @param slot The custom comparison slot to check
     * @return true if the slot has been registered, false otherwise
     */
    fun isCustomComparerRegistered(slot: ValComparer): Boolean
    
    /**
     * Clear all registered custom comparers.
     */
    fun clearCustomComparers()
}