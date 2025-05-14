package lmdb

import kotlinx.io.files.*
import kotlin.uuid.Uuid

/**
 * JVM implementation of pathCreateTestDir
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
 * JVM implementation of createRandomTestEnv
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
