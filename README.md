# kotlin-lmdb

<div>
  <img src="kotlin-lmdb.png" alt="kotlin-lmdb logo" width="200" align="left" style="margin-right: 10px"/>
  <p>
    <a href="https://github.com/crowded-libs/kotlin-lmdb/actions/workflows/build.yml"><img src="https://github.com/crowded-libs/kotlin-lmdb/actions/workflows/build.yml/badge.svg" alt="Build and Test"></a>
    <a href="https://search.maven.org/search?q=g:%22io.github.crowded-libs%22"><img src="https://img.shields.io/maven-central/v/io.github.crowded-libs/kotlin-lmdb.svg?label=Maven%20Central" alt="Maven Central"></a>
    <a href="https://www.openldap.org/software/release/license.html"><img src="https://img.shields.io/badge/License-OpenLDAP-blue.svg" alt="License"></a>
  </p>
</div>

<br clear="all" />

A high-performance, cross-platform Kotlin library for OpenLDAP's Lightning Memory-Mapped Database (LMDB).

## Overview

kotlin-lmdb provides a type-safe, idiomatic Kotlin API for LMDB, one of the fastest and most reliable key-value stores available. This library enables direct integration with LMDB across JVM and Native platforms from a single, consistent codebase.

### Key Benefits

- **Cross-Platform Compatibility**: Works seamlessly on JVM, Android, and Native platforms (Linux, macOS, Windows, iOS, watchOS, tvOS).
- **Performance**: Direct bindings to native LMDB without additional layers for maximum performance
- **Memory Efficiency**: Leverages LMDB's memory-mapped architecture for efficient memory usage
- **Transactional**: Fully ACID-compliant with robust transaction support
- **Consistent API**: Unified Kotlin API regardless of platform
- **Type Safety**: Leverages Kotlin's type system for safer database operations

## Installation

### Gradle (Kotlin DSL)

```kotlin
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("io.github.crowded-libs:kotlin-lmdb:0.2.0")
            }
        }
    }
}
```

### Maven

```xml
<dependency>
  <groupId>io.github.crowded-libs</groupId>
  <artifactId>kotlin-lmdb</artifactId>
  <version>0.2.0</version>
</dependency>
```

## Getting Started

### Basic Usage

```kotlin
// Create and open an environment
val env = Env()
env.open("/path/to/database")

// Perform database operations within a transaction
env.beginTxn { txn ->
    val dbi = txn.dbiOpen()

    // Write data
    txn.put(dbi, "key1".encodeToByteArray(), "value1".encodeToByteArray())

    // Read data
    val (resultCode, k, v) = txn.get(dbi, "key1".encodeToByteArray())
    if (resultCode == 0) {
        val value = v.toByteArray()!!.decodeToString()
        println(value)  // Outputs: value1
    }

    txn.commit()
}

// Always close the environment when done
env.close()
```

### Working with Multiple Values (DupSort)

LMDB supports storing multiple values for a single key using DupSort:

```kotlin
// Create environment and open database with dupsort flag
val env = Env()
env.open("/path/to/database")

env.beginTxn { txn ->
    // Open database with DupSort option enabled
    val dbi = txn.dbiOpen(null, DbiOption.DupSort, DbiOption.Create)

    // Store multiple values for the same key
    val key = "category".encodeToByteArray()
    txn.put(dbi, key, "item1".encodeToByteArray())
    txn.put(dbi, key, "item2".encodeToByteArray())
    txn.put(dbi, key, "item3".encodeToByteArray())

    // Retrieve all values for a key using a cursor
    val cursor = txn.openCursor(dbi)
    cursor.use {
        // Position cursor at the first value for this key
        val result = cursor.set(key)

        if (result.first == 0) {
            // Process the first value
            println("Value: ${result.third.toByteArray()!!.decodeToString()}")

            // Iterate through remaining values for this key
            var moreValues = true
            while (moreValues) {
                val nextResult = cursor.nextDuplicate()
                if (nextResult.first == 0) {
                    println("Value: ${nextResult.third.toByteArray()!!.decodeToString()}")
                } else {
                    moreValues = false
                }
            }
        }
    }

    txn.commit()
}

env.close()
```

### Using Built-in Comparers

The library comes with several pre-built comparers that you can use without writing custom code:

```kotlin
val env = Env()
env.open("/path/to/database")

env.beginTxn { txn ->
    // Use pre-built INTEGER_KEY comparer for numeric sorting
    val config = DbiConfig(keyComparer = ValComparer.INTEGER_KEY)
    val intDb = txn.dbiOpen("integer-keys", config, DbiOption.Create)

    // Now keys will be sorted as integers
    txn.put(intDb, byteArrayOf(10), "value-10".encodeToByteArray())
    txn.put(intDb, byteArrayOf(5), "value-5".encodeToByteArray()) 
    txn.put(intDb, byteArrayOf(20), "value-20".encodeToByteArray())

    // When iterating, they will be in order: 5, 10, 20
    // Open a different database with lexicographic string sorting
    val stringConfig = DbiConfig(keyComparer = ValComparer.LEXICOGRAPHIC_STRING)
    val stringDb = txn.dbiOpen("string-keys", stringConfig, DbiOption.Create)

    txn.commit()
}

env.close()
```

### Custom Comparators

You can also define your own custom sorting logic for keys and values:

```kotlin
// Register a custom first-byte comparator
val firstByteComparer: ValCompare = { a, b -> 
    val aBytes = a.toByteArray() ?: byteArrayOf()
    val bBytes = b.toByteArray() ?: byteArrayOf()

    val aFirstByte = if (aBytes.isNotEmpty()) aBytes[0].toInt() else 0
    val bFirstByte = if (bBytes.isNotEmpty()) bBytes[0].toInt() else 0

    aFirstByte - bFirstByte
}

// Clear any existing custom comparers
clearCustomComparers()

// Register the custom comparer
registerCustomComparer(ValComparer.CUSTOM_1, firstByteComparer)

val env = Env()
env.open("/path/to/database")

env.beginTxn { txn ->
    // Create database config that uses the custom comparer
    val config = DbiConfig(keyComparer = ValComparer.CUSTOM_1)
    val dbi = txn.dbiOpen("custom-comparer-db", config, DbiOption.Create)

    // Now keys will be sorted by their first byte
    // Add some test data
    txn.put(dbi, "abc".encodeToByteArray(), "value1".encodeToByteArray())
    txn.put(dbi, "bcd".encodeToByteArray(), "value2".encodeToByteArray())
    txn.put(dbi, "zde".encodeToByteArray(), "value3".encodeToByteArray())

    txn.commit()
}

env.close()
```

### Available Built-in Comparers

The library provides these pre-built comparers through the `ValComparer` enum:

- `BITWISE` - Default byte-by-byte comparison (ascending)
- `REVERSE_BITWISE` - Byte-by-byte comparison in reverse order
- `LEXICOGRAPHIC_STRING` - Compare as UTF-8 strings (ascending)
- `REVERSE_LEXICOGRAPHIC_STRING` - Compare as UTF-8 strings (descending) 
- `INTEGER_KEY` - Compare as integers (ascending)
- `REVERSE_INTEGER_KEY` - Compare as integers (descending)
- `LENGTH` - Compare by length first, then by content
- `REVERSE_LENGTH` - Compare by length (longer first), then by content
- `LENGTH_ONLY` - Compare only by length, ignoring content
- `HASH_CODE` - Compare by hash code (for faster large value comparisons)

## Performance Considerations

- LMDB is memory-mapped, providing exceptional read performance
- Consider using a single transaction for multiple operations when possible
- Memory usage scales with database size as pages are mapped into memory

## Platforms Support

- **JVM**: Linux, macOS, Windows
- **Android**: All supported Android devices
- **Kotlin/Native**:
  - **Desktop**: macOS (x64, Arm64), Linux (x64, Arm64), Windows (x64)
  - **iOS**: iOS (Arm64, x64), iOS Simulator (Arm64)
  - **watchOS**: watchOS (Arm32, Arm64), watchOS Simulator (Arm64)
  - **tvOS**: tvOS (Arm64), tvOS Simulator (Arm64)

## References

- [Official LMDB Documentation](http://lmdb.tech/doc)
- [LMDB GitHub Repository](https://github.com/LMDB/lmdb)
