package lmdb

/**
 * Configuration for database operations that specifies custom comparison functions.
 *
 * This class allows customizing the behavior of a database by providing
 * custom comparers for keys and duplicate data values. These comparers
 * determine how keys and data values are ordered in the database.
 *
 * The comparers can be specified using the [ValComparer] enum, which provides
 * a set of predefined comparison algorithms, or by registering custom comparison
 * functions with the [ValComparerRegistry].
 *
 * Warning: The same comparison functions must be used by every program
 * accessing the database, and must be set before any data access functions
 * are used, otherwise data corruption may occur.
 *
 * Usage example:
 * ```
 * // Use predefined comparers
 * val config = DbiConfig(
 *     keyComparer = ValComparer.LEXICOGRAPHIC_STRING,
 *     dupComparer = ValComparer.INTEGER_KEY
 * )
 *
 * // Register and use a custom comparer
 * ValComparerRegistry.registerCustomComparer(ValComparer.CUSTOM_1) { a, b ->
 *     // Custom comparison logic
 *     val aBytes = a.toByteArray() ?: return@registerCustomComparer -1
 *     val bBytes = b.toByteArray() ?: return@registerCustomComparer 1
 *     // ... comparison logic ...
 * }
 *
 * val config = DbiConfig(keyComparer = ValComparer.CUSTOM_1)
 * ```
 */
class DbiConfig {
    /**
     * Custom comparer for keys in the database.
     *
     * This property specifies which comparison function to use when ordering keys
     * in the database. If null, the default lexicographical comparison is used.
     *
     * The comparer can be one of the predefined [ValComparer] values or a custom
     * comparer registered with [ValComparerRegistry].
     */
    var keyComparer: ValComparer? = null
    
    /**
     * Custom comparer for duplicate values in the database.
     * 
     * This property specifies which comparison function to use when ordering
     * duplicate values for the same key. This is only used with databases
     * opened with [DbiOption.DupSort].
     *
     * If null, the default lexicographical comparison is used.
     */
    var dupComparer: ValComparer? = null
    
    /**
     * Creates a default DbiConfig with no custom comparers.
     */
    constructor()
    
    /**
     * Creates a DbiConfig with the specified custom comparers.
     *
     * @param keyComparer The comparer to use for keys. If null, the default 
     *                    lexicographical comparison is used.
     * @param dupComparer The comparer to use for duplicate values. If null, the default
     *                    lexicographical comparison is used. Only relevant for
     *                    databases opened with [DbiOption.DupSort].
     */
    constructor(keyComparer: ValComparer? = null, dupComparer: ValComparer? = null) {
        this.keyComparer = keyComparer
        this.dupComparer = dupComparer
    }
}