package lmdb

/**
 * Platform-specific function to create a test directory
 * This handles the differences between platforms (JVM/Native vs JS)
 */
expect fun pathCreateTestDir(): String

/**
 * Platform-specific function to create a random test environment
 * This handles the differences between platforms (JVM/Native vs JS)
 */
expect fun createRandomTestEnv(open: Boolean = true, mapSize: ULong? = null, maxDatabases: UInt? = null): Env

/**
 * Platform-specific function to register a custom comparer
 * This handles the differences between JVM and Native implementations
 */
expect fun registerCustomComparer(slot: ValComparer, compareFunction: ValCompare)

/**
 * Platform-specific function to clear all custom comparers
 * This handles the differences between JVM and Native implementations
 */
expect fun clearCustomComparers()
