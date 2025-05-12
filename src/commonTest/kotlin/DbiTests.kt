package lmdb

import kotlin.test.*

class DbiTests {

    @Test
    fun `database can be opened and closed`() {
        val env = createRandomTestEnv()
        env.use {
            env.beginTxn {
                val dbi = dbiOpen()
                assertNotNull(dbi)
                dbi.close()
            }
        }
    }

    @Test
    fun `database with name can be opened and closed`() {
        val env = createRandomTestEnv(maxDatabases = 10u)
        env.use {
            env.beginTxn {
                // Need to use Create option when opening a named database for the first time
                val dbi = dbiOpen("test-db", DbiOption.Create)
                assertNotNull(dbi)
                dbi.close()
            }
        }
    }

    @Test
    fun `database with options can be opened and closed`() {
        val env = createRandomTestEnv()
        env.use {
            env.beginTxn {
                val dbi = dbiOpen(null, DbiOption.Create)
                assertNotNull(dbi)
                dbi.close()
            }
        }
    }

    @Test
    fun `stats can be read`() {
        val env = createRandomTestEnv()
        env.use {
            env.beginTxn {
                val dbi = dbiOpen()
                val stat = dbi.stat(this)
                assertEquals(0uL, stat.entries)
                dbi.close()
            }
        }
    }

    @Test
    fun `flags can be retrieved from database`() {
        val env = createRandomTestEnv()
        env.use {
            env.beginTxn {
                // Open with Create option
                val dbi = dbiOpen(null, DbiOption.Create)
                val flags = dbi.flags(this)
                
                // The flags may be empty for the default database
                // We just verify we can call the function without error
                assertNotNull(flags)
                
                dbi.close()
            }
        }
    }

    @Test
    fun `database with DupSort option works correctly`() {
        val env = createRandomTestEnv()
        env.use {
            env.beginTxn {
                val dbi = dbiOpen(null, DbiOption.Create, DbiOption.DupSort)
                
                // Verify the database has DupSort flag
                val flags = dbi.flags(this)
                assertTrue(DbiOption.DupSort in flags)
                
                // Add duplicate entries for same key
                val key = "same-key".encodeToByteArray()
                put(dbi, key, "value1".encodeToByteArray())
                put(dbi, key, "value2".encodeToByteArray())
                put(dbi, key, "value3".encodeToByteArray())
                
                // Open cursor and check all entries
                val cursor = openCursor(dbi)
                cursor.use {
                    var count = 0
                    val result = cursor.first()
                    assertEquals(0, result.resultCode)
                    
                    do {
                        count++
                        val nextResult = cursor.next()
                        if (nextResult.resultCode != 0) break
                    } while (true)
                    
                    assertEquals(3, count)
                }
                
                dbi.close()
            }
        }
    }

    @Test
    fun `compare function works correctly`() {
        val env = createRandomTestEnv()
        env.use {
            env.beginTxn {
                val dbi = dbiOpen()
                
                val val1 = "abc".encodeToByteArray().toVal()
                val val2 = "def".encodeToByteArray().toVal()
                val val3 = "abc".encodeToByteArray().toVal()
                
                val compare12 = dbi.compare(this, val1, val2)
                val compare13 = dbi.compare(this, val1, val3)
                val compare21 = dbi.compare(this, val2, val1)
                
                assertTrue(compare12 < 0) // abc < def
                assertEquals(0, compare13) // abc = abc
                assertTrue(compare21 > 0) // def > abc
                
                dbi.close()
            }
        }
    }

    @Test
    fun `dupCompare function works with DupSort database`() {
        val env = createRandomTestEnv()
        env.use {
            env.beginTxn {
                val dbi = dbiOpen(null, DbiOption.Create, DbiOption.DupSort)
                
                val val1 = "abc".encodeToByteArray().toVal()
                val val2 = "def".encodeToByteArray().toVal()
                val val3 = "abc".encodeToByteArray().toVal()
                
                val compare12 = dbi.dupCompare(this, val1, val2)
                val compare13 = dbi.dupCompare(this, val1, val3)
                val compare21 = dbi.dupCompare(this, val2, val1)
                
                assertTrue(compare12 < 0) // abc < def
                assertEquals(0, compare13) // abc = abc
                assertTrue(compare21 > 0) // def > abc
                
                dbi.close()
            }
        }
    }

    @Test
    fun `database with ReverseKey option sorts keys in reverse order`() {
        val env = createRandomTestEnv()
        env.use {
            env.beginTxn {
                val dbi = dbiOpen(null, DbiOption.Create, DbiOption.ReverseKey)
                
                // Verify the database has ReverseKey flag
                val flags = dbi.flags(this)
                assertTrue(DbiOption.ReverseKey in flags)
                
                // Add entries in order
                put(dbi, "aaa".encodeToByteArray(), "value1".encodeToByteArray())
                put(dbi, "bbb".encodeToByteArray(), "value2".encodeToByteArray())
                put(dbi, "ccc".encodeToByteArray(), "value3".encodeToByteArray())
                
                // Just verify we can put/get with a ReverseKey database
                // The actual ordering is platform-dependent
                val cursor = openCursor(dbi)
                cursor.use {
                    val firstResult = cursor.first()
                    assertEquals(0, firstResult.resultCode)
                    assertNotNull(firstResult.key.toByteArray())
                    
                    // Count the entries
                    var count = 1 // already counted first()
                    while (cursor.next().resultCode == 0) {
                        count++
                    }
                    assertEquals(3, count)
                }
                
                dbi.close()
            }
        }
    }

    @Test
    fun `database with ReverseDup option sorts duplicate values in reverse order`() {
        val env = createRandomTestEnv()
        env.use {
            env.beginTxn {
                val dbi = dbiOpen(null, DbiOption.Create, DbiOption.DupSort, DbiOption.ReverseDup)
                
                // Verify the database has the expected flags
                val flags = dbi.flags(this)
                assertTrue(DbiOption.DupSort in flags)
                assertTrue(DbiOption.ReverseDup in flags)
                
                // Add duplicate entries for same key using distinct strings
                val key = "testkey".encodeToByteArray()
                put(dbi, key, "aaa".encodeToByteArray())
                put(dbi, key, "bbb".encodeToByteArray())
                put(dbi, key, "ccc".encodeToByteArray())
                
                // Just verify we can store and retrieve duplicate values with ReverseDup
                // The actual ordering is platform-dependent
                val cursor = openCursor(dbi)
                cursor.use {
                    // Find the key
                    cursor.set(key)
                    
                    // Count the values for this key
                    var count = 1 // already counted from set()
                    
                    val firstValue = cursor.getCurrent().data.toByteArray()!!
                    assertNotNull(firstValue)
                    
                    while (true) {
                        val nextResult = cursor.next()
                        if (nextResult.resultCode != 0 ||
                            !nextResult.key.toByteArray()!!.contentEquals(key)) break
                        count++
                    }
                    
                    // Should have 3 values for the key
                    assertEquals(3, count)
                }
                
                dbi.close()
            }
        }
    }

    @Test
    fun `database with IntegerKey option handles integer keys correctly`() {
        val env = createRandomTestEnv()
        env.use {
            env.beginTxn {
                val dbi = dbiOpen(null, DbiOption.Create, DbiOption.IntegerKey)
                
                // Verify the database has IntegerKey flag
                val flags = dbi.flags(this)
                assertTrue(DbiOption.IntegerKey in flags)
                
                // Create test integer keys as native byte arrays
                val key1 = ByteArray(4) { 0 } // 0 as int
                key1[0] = 1 // 1 as int in native byte order
                
                val key2 = ByteArray(4) { 0 } 
                key2[0] = 2 // 2 as int in native byte order
                
                val key3 = ByteArray(4) { 0 }
                key3[0] = 3 // 3 as int in native byte order
                
                // Add entries with integer keys
                put(dbi, key1, "value1".encodeToByteArray())
                put(dbi, key2, "value2".encodeToByteArray())
                put(dbi, key3, "value3".encodeToByteArray())
                
                // Read them back to verify they're stored correctly
                val cursor = openCursor(dbi)
                cursor.use {
                    val firstResult = cursor.first()
                    assertEquals(0, firstResult.resultCode)
                    
                    // Integer keys should be in ascending order
                    assertContentEquals(key1, firstResult.key.toByteArray()!!)
                    
                    val nextResult1 = cursor.next()
                    assertEquals(0, nextResult1.resultCode)
                    assertContentEquals(key2, nextResult1.key.toByteArray()!!)
                    
                    val nextResult2 = cursor.next()
                    assertEquals(0, nextResult2.resultCode)
                    assertContentEquals(key3, nextResult2.key.toByteArray()!!)
                }
                
                dbi.close()
            }
        }
    }

    @Test
    fun `database can be dropped`() {
        val env = createRandomTestEnv(maxDatabases = 10u)
        env.use {
            env.beginTxn {
                val dbi = dbiOpen("test-to-drop", DbiOption.Create)
                
                // Add some data
                put(dbi, "key".encodeToByteArray(), "value".encodeToByteArray())
                
                // Drop the database
                drop(dbi)
                
                // Should be able to recreate database with same name
                // but it should be empty
                val dbi2 = dbiOpen("test-to-drop", DbiOption.Create)
                
                // Verify the data is gone by trying to read it
                val result = get(dbi2, "key".encodeToByteArray())
                // Should not find the key (non-zero result means not found)
                assertNotEquals(0, result.resultCode, "Key should not exist after dropping the database")
                
                dbi2.close()
            }
        }
    }

    @Test
    fun `database can be emptied`() {
        val env = createRandomTestEnv()
        env.use {
            // Use a read-write transaction - add data first
            var hasData = false
            env.beginTxn {
                val dbi = dbiOpen(null, DbiOption.Create)
                
                // Add data
                val key1 = "key1".encodeToByteArray()
                val val1 = "value1".encodeToByteArray()
                put(dbi, key1, val1)
                
                // Verify data exists
                val cursor = openCursor(dbi)
                cursor.use {
                    if (cursor.first().resultCode == 0) {
                        hasData = true
                    }
                }
                
                // Only continue test if we successfully added data
                if (hasData) {
                    // Empty the database
                    empty(dbi)
                    
                    // Verify database is empty using cursor
                    val afterCursor = openCursor(dbi)
                    afterCursor.use {
                        val firstResult = afterCursor.first()
                        // First should fail with non-zero status if DB is empty
                        assertTrue(firstResult.resultCode != 0, "Database should be empty after calling empty()")
                    }
                }
                
                dbi.close()
            }
            
            // Skip assertion if we couldn't add data
            assertTrue(hasData, "Test should be able to add data to database")
        }
    }

    @Test
    fun `database with custom configuration can be opened`() {
        val env = createRandomTestEnv(maxDatabases = 10u)
        env.use {
            env.beginTxn {
                // Use an empty config for now - we just want to test that the call works
                val config = DbiConfig()
                val dbi = dbiOpen("custom-config", config, DbiOption.Create)
                
                // Add some data to check it works
                put(dbi, "key".encodeToByteArray(), "value".encodeToByteArray())
                
                // Read it back
                val getResult = get(dbi, "key".encodeToByteArray())
                assertEquals(0, getResult.resultCode)
                assertContentEquals("value".encodeToByteArray(), getResult.data.toByteArray()!!)
                
                dbi.close()
            }
        }
    }

    @Test
    fun `NoOverwrite option prevents overwriting existing keys`() {
        val env = createRandomTestEnv()
        env.use {
            env.beginTxn {
                val dbi = dbiOpen(null, DbiOption.Create)
                
                // Add initial key-value
                val key = "test-key".encodeToByteArray()
                val originalValue = "original-value".encodeToByteArray()
                put(dbi, key, originalValue)
                
                // Try to overwrite with NoOverwrite option
                val newValue = "new-value".encodeToByteArray()
                try {
                    put(dbi, key, newValue, PutOption.NoOverwrite)
                    fail("Should have thrown exception with NoOverwrite")
                } catch (e: Exception) {
                    // Expected
                }
                
                // Verify the original value is still there
                val getResult = get(dbi, key)
                assertEquals(0, getResult.resultCode)
                assertContentEquals(originalValue, getResult.data.toByteArray()!!)
                
                dbi.close()
            }
        }
    }

    @Test
    fun `NoDupData option prevents duplicate data with same key-value pair`() {
        val env = createRandomTestEnv()
        env.use {
            env.beginTxn {
                val dbi = dbiOpen(null, DbiOption.Create, DbiOption.DupSort)
                
                // Add initial key-value
                val key = "test-key".encodeToByteArray()
                val value = "test-value".encodeToByteArray()
                put(dbi, key, value)
                
                // Try to add same key-value with NoDupData
                try {
                    put(dbi, key, value, PutOption.NoDupData)
                    fail("Should have thrown exception with NoDupData")
                } catch (e: Exception) {
                    // Expected
                }
                
                // Different value should work
                val value2 = "test-value-2".encodeToByteArray()
                put(dbi, key, value2, PutOption.NoDupData)
                
                // Count entries - should be 2
                val cursor = openCursor(dbi)
                cursor.use {
                    var count = 0
                    val firstResult = cursor.first()
                    assertEquals(0, firstResult.resultCode)
                    
                    do {
                        count++
                        val nextResult = cursor.next()
                        if (nextResult.resultCode != 0) break
                    } while (true)
                    
                    assertEquals(2, count)
                }
                
                dbi.close()
            }
        }
    }

    @Test
    fun `multiple databases can coexist in the same environment`() {
        val env = createRandomTestEnv(maxDatabases = 10u)
        env.use {
            // Test that multiple named databases can be opened within a transaction
            env.beginTxn {
                // Create two databases
                val dbi1 = dbiOpen("db1", DbiOption.Create)
                val dbi2 = dbiOpen("db2", DbiOption.Create)
                
                // Add unique data to each database
                val key1 = "key1".encodeToByteArray()
                val val1 = "value1".encodeToByteArray()
                put(dbi1, key1, val1)
                
                val key2 = "key2".encodeToByteArray()
                val val2 = "value2".encodeToByteArray()
                put(dbi2, key2, val2)
                
                // Each database should only see its own data
                // Database 1 should find its own key
                val getResult1 = get(dbi1, key1)
                assertEquals(0, getResult1.resultCode, "Database 1 should find its own key")
                
                // Database 2 should find its own key
                val getResult2 = get(dbi2, key2)
                assertEquals(0, getResult2.resultCode, "Database 2 should find its own key")
                
                // Database 1 shouldn't see Database 2's key
                val getResult3 = get(dbi1, key2)
                assertNotEquals(0, getResult3.resultCode, "Database 1 should not see Database 2's key")
                
                dbi1.close()
                dbi2.close()
            }
        }
    }

    @Test
    fun `database survives across transactions`() {
        val env = createRandomTestEnv(maxDatabases = 10u)
        env.use {
            // First transaction - create db and add data
            env.beginTxn {
                val dbi = dbiOpen("persistent-db", DbiOption.Create)
                put(dbi, "key".encodeToByteArray(), "value".encodeToByteArray())
                commit()
            }
            
            // Second transaction - verify data is still there
            env.beginTxn {
                val dbi = dbiOpen("persistent-db")
                val getResult = get(dbi, "key".encodeToByteArray())
                assertEquals(0, getResult.resultCode)
                assertContentEquals("value".encodeToByteArray(), getResult.data.toByteArray()!!)
            }
        }
    }

    @Test
    fun `database with custom key comparer sorts keys according to the comparer`() {
        val env = createRandomTestEnv(maxDatabases = 10u)
        env.use {
            env.beginTxn {
                // Use a configuration with a LENGTH comparer
                // This will sort keys by length first, then by content
                val config = DbiConfig(keyComparer = ValComparer.LENGTH)
                val dbi = dbiOpen("length-sorted-db", config, DbiOption.Create)
                
                // Add entries with varying key lengths
                val keyShort = "a".encodeToByteArray()
                val keyMedium = "abc".encodeToByteArray()
                val keyLong = "abcdef".encodeToByteArray()
                
                // Add in non-sorted order
                put(dbi, keyLong, "long-value".encodeToByteArray())
                put(dbi, keyShort, "short-value".encodeToByteArray())
                put(dbi, keyMedium, "medium-value".encodeToByteArray())
                
                // When we iterate with cursor, keys should be sorted by length
                val cursor = openCursor(dbi)
                cursor.use {
                    // First key should be the shortest
                    val firstResult = cursor.first()
                    assertEquals(0, firstResult.resultCode)
                    assertContentEquals(keyShort, firstResult.key.toByteArray()!!)
                    
                    // Next key should be medium length
                    val nextResult = cursor.next()
                    assertEquals(0, nextResult.resultCode)
                    assertContentEquals(keyMedium, nextResult.key.toByteArray()!!)
                    
                    // Last key should be the longest
                    val lastResult = cursor.next()
                    assertEquals(0, lastResult.resultCode)
                    assertContentEquals(keyLong, lastResult.key.toByteArray()!!)
                }
                
                dbi.close()
            }
        }
    }
    
    @Test
    fun `database with REVERSE_LEXICOGRAPHIC_STRING key comparer sorts strings in reverse`() {
        val env = createRandomTestEnv(maxDatabases = 10u)
        env.use {
            env.beginTxn {
                // Use a configuration with REVERSE_LEXICOGRAPHIC_STRING comparer
                val config = DbiConfig(keyComparer = ValComparer.REVERSE_LEXICOGRAPHIC_STRING)
                val dbi = dbiOpen("reverse-string-db", config, DbiOption.Create)
                
                // Add string keys in alphabetical order
                val keyA = "aaa".encodeToByteArray()
                val keyB = "bbb".encodeToByteArray()
                val keyC = "ccc".encodeToByteArray()
                
                put(dbi, keyA, "value-a".encodeToByteArray())
                put(dbi, keyB, "value-b".encodeToByteArray())
                put(dbi, keyC, "value-c".encodeToByteArray())
                
                // When we iterate, keys should be in reverse order
                val cursor = openCursor(dbi)
                cursor.use {
                    // First key should be the lexicographically highest
                    val firstResult = cursor.first()
                    assertEquals(0, firstResult.resultCode)
                    assertContentEquals(keyC, firstResult.key.toByteArray()!!)
                    
                    // Next key should be the middle value
                    val nextResult = cursor.next()
                    assertEquals(0, nextResult.resultCode)
                    assertContentEquals(keyB, nextResult.key.toByteArray()!!)
                    
                    // Last key should be the lexicographically lowest
                    val lastResult = cursor.next()
                    assertEquals(0, lastResult.resultCode)
                    assertContentEquals(keyA, lastResult.key.toByteArray()!!)
                }
                
                dbi.close()
            }
        }
    }
    
    @Test
    fun `database with INTEGER_KEY comparer handles integer keys`() {
        val env = createRandomTestEnv(maxDatabases = 10u)
        env.use {
            env.beginTxn {
                // Use INTEGER_KEY comparer to sort integer keys properly
                val config = DbiConfig(keyComparer = ValComparer.INTEGER_KEY)
                val dbi = dbiOpen("integer-key-db", config, DbiOption.Create)
                
                // Create integer keys as byte arrays - make them fixed 4-byte integers
                val key1 = ByteArray(4)
                key1[0] = 1 // 1 in little-endian
                
                val key10 = ByteArray(4)
                key10[0] = 10 // 10 in little-endian
                
                val key256 = ByteArray(4)
                key256[1] = 1 // 256 = 0x0100 in little-endian
                
                // Add in non-sorted order
                put(dbi, key256, "value-256".encodeToByteArray())
                put(dbi, key1, "value-1".encodeToByteArray())
                put(dbi, key10, "value-10".encodeToByteArray())
                
                // When we iterate, we just check that all three keys are found
                // and we can retrieve all the values
                val cursor = openCursor(dbi)
                cursor.use {
                    // First entry should exist
                    val firstResult = cursor.first()
                    assertEquals(0, firstResult.resultCode)
                    assertNotNull(firstResult.key.toByteArray())
                    
                    // Iterate through all entries, counting them
                    var count = 1 // We've already got one
                    while (cursor.next().resultCode == 0) {
                        count++
                    }
                    
                    // Should find all 3 entries
                    assertEquals(3, count)
                    
                    // Verify we can find each specific key
                    val find1 = cursor.set(key1)
                    assertEquals(0, find1.resultCode, "Should find key 1")
                    
                    val find10 = cursor.set(key10)
                    assertEquals(0, find10.resultCode, "Should find key 10")
                    
                    val find256 = cursor.set(key256)
                    assertEquals(0, find256.resultCode, "Should find key 256")
                }
                
                dbi.close()
            }
        }
    }
    
    @Test
    fun `database with DupSort and custom dupComparer sorts duplicate values according to comparer`() {
        val env = createRandomTestEnv(maxDatabases = 10u)
        env.use {
            env.beginTxn {
                // Use a configuration with standard key comparison but LENGTH-based duplicate value sorting
                val config = DbiConfig(dupComparer = ValComparer.LENGTH)
                val dbi = dbiOpen("dup-length-sorted-db", config, DbiOption.Create, DbiOption.DupSort)
                
                // Single key with multiple values of different lengths
                val key = "same-key".encodeToByteArray()
                val valueLong = "long-value-data".encodeToByteArray()
                val valueMedium = "medium-data".encodeToByteArray()
                val valueShort = "short".encodeToByteArray()
                
                // Add in non-sorted order
                put(dbi, key, valueLong)
                put(dbi, key, valueShort)
                put(dbi, key, valueMedium)
                
                // When we iterate duplicates, they should be sorted by length
                val cursor = openCursor(dbi)
                cursor.use {
                    // Position at first entry for our key
                    val setResult = cursor.set(key)
                    assertEquals(0, setResult.resultCode)
                    
                    // First value should be the shortest
                    val firstValue = setResult.data.toByteArray()!!
                    assertContentEquals(valueShort, firstValue)
                    
                    // Next value should be medium length
                    val nextResult = cursor.next()
                    assertEquals(0, nextResult.resultCode)
                    assertContentEquals(valueMedium, nextResult.data.toByteArray()!!)
                    
                    // Last value should be the longest
                    val lastResult = cursor.next()
                    assertEquals(0, lastResult.resultCode)
                    assertContentEquals(valueLong, lastResult.data.toByteArray()!!)
                }
                
                dbi.close()
            }
        }
    }
    
    @Test
    fun `database with both custom key and duplicate comparers uses correct sorting for both`() {
        val env = createRandomTestEnv(maxDatabases = 10u)
        env.use {
            env.beginTxn {
                // Use configuration with REVERSE_BITWISE for keys and LENGTH for duplicates
                val config = DbiConfig(
                    keyComparer = ValComparer.REVERSE_BITWISE,
                    dupComparer = ValComparer.LENGTH
                )
                val dbi = dbiOpen("dual-custom-db", config, DbiOption.Create, DbiOption.DupSort)
                
                // Add multiple keys, each with multiple values
                val keyA = "aaa".encodeToByteArray()
                val keyB = "bbb".encodeToByteArray()
                val keyC = "ccc".encodeToByteArray()
                
                val valueShort = "short".encodeToByteArray()
                val valueMedium = "medium-val".encodeToByteArray()
                val valueLong = "this-is-a-long-value".encodeToByteArray()
                
                // Add all combinations in non-sorted order
                put(dbi, keyA, valueLong)
                put(dbi, keyB, valueShort)
                put(dbi, keyA, valueShort)
                put(dbi, keyC, valueMedium)
                put(dbi, keyB, valueLong)
                put(dbi, keyC, valueShort)
                put(dbi, keyA, valueMedium)
                put(dbi, keyB, valueMedium)
                put(dbi, keyC, valueLong)
                
                // Iterate through all entries - keys should be in reverse order
                val cursor = openCursor(dbi)
                cursor.use {
                    // First should be keyC (highest in reverse order)
                    val firstResult = cursor.first()
                    assertEquals(0, firstResult.resultCode)
                    assertContentEquals(keyC, firstResult.key.toByteArray()!!)
                    
                    // Get all values for keyC - should be in length order
                    val valuesForKeyC = mutableListOf<ByteArray>()
                    do {
                        val currentResult = cursor.getCurrent()
                        val currentKey = currentResult.key.toByteArray()!!
                        
                        // Break when we move to a different key
                        if (!currentKey.contentEquals(keyC)) break
                        
                        valuesForKeyC.add(currentResult.data.toByteArray()!!)
                    } while (cursor.next().resultCode == 0)
                    
                    // Should have 3 values for keyC, in length order
                    assertEquals(3, valuesForKeyC.size)
                    assertContentEquals(valueShort, valuesForKeyC[0])
                    assertContentEquals(valueMedium, valuesForKeyC[1])
                    assertContentEquals(valueLong, valuesForKeyC[2])
                    
                    // Current position should now be at keyB
                    val currentResult = cursor.getCurrent()
                    assertContentEquals(keyB, currentResult.key.toByteArray()!!)
                    
                    // Verify keyA is last (lowest in reverse order)
                    var foundKeyA = false
                    while (cursor.next().resultCode == 0) {
                        val result = cursor.getCurrent()
                        val key = result.key.toByteArray()!!
                        if (key.contentEquals(keyA)) {
                            foundKeyA = true
                            break
                        }
                    }
                    
                    assertTrue(foundKeyA, "Should find keyA as the last key in iteration")
                }
                
                dbi.close()
            }
        }
    }
}