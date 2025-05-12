package lmdb

/**
 * JVM implementation of registerCustomComparer
 */
actual fun registerCustomComparer(slot: ValComparer, compareFunction: ValCompare) {
    ValComparerRegistry.registerCustomComparer(slot, compareFunction)
}

/**
 * JVM implementation of clearCustomComparers
 */
actual fun clearCustomComparers() {
    ValComparerRegistry.clearCustomComparers()
}
