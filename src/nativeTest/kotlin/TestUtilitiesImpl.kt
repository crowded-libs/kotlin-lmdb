package lmdb

import kotlinx.cinterop.*
import kotlinx.io.files.*
import kotlin.uuid.Uuid

// Global map to store custom comparers by slot
private val customComparers = mutableMapOf<ValComparer, ValCompare>()

// Global function that can be used with staticCFunction
private fun customCompareFunction(left: CPointer<MDB_val>?, right: CPointer<MDB_val>?, slot: ValComparer): Int {
    val comparer = customComparers[slot] ?: return 0
    val a = Val.forCompare(left)
    val b = Val.forCompare(right)
    return comparer(a, b)
}

// Static C functions for each custom slot
private val custom1CompareFunction = staticCFunction { left: CPointer<MDB_val>?, right: CPointer<MDB_val>? ->
    customCompareFunction(left, right, ValComparer.CUSTOM_1)
}

private val custom2CompareFunction = staticCFunction { left: CPointer<MDB_val>?, right: CPointer<MDB_val>? ->
    customCompareFunction(left, right, ValComparer.CUSTOM_2)
}

private val custom3CompareFunction = staticCFunction { left: CPointer<MDB_val>?, right: CPointer<MDB_val>? ->
    customCompareFunction(left, right, ValComparer.CUSTOM_3)
}

private val custom4CompareFunction = staticCFunction { left: CPointer<MDB_val>?, right: CPointer<MDB_val>? ->
    customCompareFunction(left, right, ValComparer.CUSTOM_4)
}

/**
 * Native implementation of registerCustomComparer
 * 
 * This stores the Kotlin compareFunction in a global map and registers
 * a pre-defined static C function with ValComparerRegistry.registerNativeCustomComparer
 */
actual fun registerCustomComparer(slot: ValComparer, compareFunction: ValCompare) {
    // Store the Kotlin function in our global map
    customComparers[slot] = compareFunction

    // Also register with ValComparerRegistry for compatibility
    ValComparerRegistry.registerCustomComparer(slot, compareFunction)

    // Register the appropriate static C function based on the slot
    val nativeFunction = when (slot) {
        ValComparer.CUSTOM_1 -> custom1CompareFunction
        ValComparer.CUSTOM_2 -> custom2CompareFunction
        ValComparer.CUSTOM_3 -> custom3CompareFunction
        ValComparer.CUSTOM_4 -> custom4CompareFunction
        else -> throw IllegalArgumentException("Only CUSTOM_1, CUSTOM_2, CUSTOM_3, or CUSTOM_4 slots can be registered")
    }

    // Register the native function
    ValComparerRegistry.registerNativeCustomComparer(slot, nativeFunction)
}

/**
 * Native implementation of clearCustomComparers
 */
actual fun clearCustomComparers() {
    // Clear our global map
    customComparers.clear()

    // Also clear the ValComparerRegistry
    ValComparerRegistry.clearCustomComparers()
}

/**
 * Native implementation of pathCreateTestDir
 */
actual fun pathCreateTestDir(): String {
    val fs = SystemFileSystem
    val testPath = Path(SystemTemporaryDirectory.toString(), Uuid.random().toString())
    if (!fs.exists(testPath)) {
        fs.createDirectories(testPath, true)
    }
    return testPath.toString()
}

/**
 * Native implementation of createRandomTestEnv
 */
actual fun createRandomTestEnv(open: Boolean, mapSize: ULong?, maxDatabases: UInt?): Env {
    val path = pathCreateTestDir()
    val env = Env()
    if (mapSize != null) {
        env.mapSize = mapSize
    }
    if (maxDatabases != null) {
        env.maxDatabases = maxDatabases
    }
    if (open) {
        env.open(path)
    }
    return env
}
