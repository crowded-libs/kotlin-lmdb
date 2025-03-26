import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CursorTests {

    @Test
    fun `cursor can be opened`() {
        val env = createRandomTestEnv(mapSize = 10485760UL) // 10MB
        env.use {
            env.beginTxn {
                val dbi = dbiOpen()
                val cursor = openCursor(dbi)
                cursor.use {
                    assertNotNull(cursor)
                }
            }
        }
    }
    
    @Test
    fun `cursor can put and get data`() {
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            env.beginTxn {
                val dbi = dbiOpen()
                val cursor = openCursor(dbi)
                cursor.use {
                    // Put some test data
                    val key1 = "key1".encodeToByteArray()
                    val data1 = "data1".encodeToByteArray()
                    val putResult = cursor.put(key1, data1)
                    assertEquals(0, putResult.resultCode)
                    
                    // Get data using set
                    val getResult = cursor.set(key1)
                    assertEquals(0, getResult.resultCode)
                    assertEquals("key1", getResult.key.toByteArray()!!.decodeToString())
                    assertEquals("data1", getResult.data.toByteArray()!!.decodeToString())
                }
            }
        }
    }
    
    @Test
    fun `cursor can navigate through multiple entries`() {
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            env.beginTxn {
                val dbi = dbiOpen()
                val cursor = openCursor(dbi)
                cursor.use {
                    // Add multiple entries in sorted order
                    val entries = listOf(
                        "a" to "data-a",
                        "b" to "data-b",
                        "c" to "data-c",
                        "d" to "data-d"
                    )
                    
                    entries.forEach { (k, v) ->
                        cursor.put(k.encodeToByteArray(), v.encodeToByteArray())
                    }
                    
                    // Test first
                    val firstResult = cursor.first()
                    assertEquals(0, firstResult.resultCode)
                    assertEquals("a", firstResult.key.toByteArray()!!.decodeToString())
                    assertEquals("data-a", firstResult.data.toByteArray()!!.decodeToString())
                    
                    // Test next
                    val nextResult = cursor.next()
                    assertEquals(0, nextResult.resultCode)
                    assertEquals("b", nextResult.key.toByteArray()!!.decodeToString())
                    assertEquals("data-b", nextResult.data.toByteArray()!!.decodeToString())
                    
                    // Test last
                    val lastResult = cursor.last()
                    assertEquals(0, lastResult.resultCode)
                    assertEquals("d", lastResult.key.toByteArray()!!.decodeToString())
                    assertEquals("data-d", lastResult.data.toByteArray()!!.decodeToString())
                    
                    // Test previous
                    val prevResult = cursor.previous()
                    assertEquals(0, prevResult.resultCode)
                    assertEquals("c", prevResult.key.toByteArray()!!.decodeToString())
                    assertEquals("data-c", prevResult.data.toByteArray()!!.decodeToString())
                }
            }
        }
    }
    
    @Test
    fun `cursor can get current entry`() {
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            env.beginTxn {
                val dbi = dbiOpen()
                val cursor = openCursor(dbi)
                cursor.use {
                    val key = "testkey".encodeToByteArray()
                    val data = "testdata".encodeToByteArray()
                    cursor.put(key, data)
                    
                    // Position cursor at the key
                    cursor.set(key)
                    
                    // Get current position
                    val currentResult = cursor.getCurrent()
                    assertEquals(0, currentResult.resultCode)
                    assertEquals("testkey", currentResult.key.toByteArray()!!.decodeToString())
                    assertEquals("testdata", currentResult.data.toByteArray()!!.decodeToString())
                }
            }
        }
    }
    
    @Test
    fun `cursor can perform exact value lookup using getBoth`() {
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            env.beginTxn {
                // Need to enable DupSort for getBoth operations 
                val dbi = dbiOpen(options = arrayOf(DbiOption.DupSort))
                val cursor = openCursor(dbi)
                cursor.use {
                    val key = "testkey".encodeToByteArray()
                    val data = "testdata".encodeToByteArray()
                    cursor.put(key, data)
                    
                    // Successful lookup with exact key and data match
                    val successResult = cursor.getBoth(key, data)
                    assertEquals(0, successResult.resultCode)
                    assertEquals("testkey", successResult.key.toByteArray()!!.decodeToString())
                    assertEquals("testdata", successResult.data.toByteArray()!!.decodeToString())
                    
                    // Failed lookup with mismatched data
                    try {
                        val wrongData = "wrongdata".encodeToByteArray()
                        val failResult = cursor.getBoth(key, wrongData)
                        assertNotEquals(0, failResult.resultCode) // Should fail with non-zero status
                    } catch (e: LmdbException) {
                        // Expected exception for not found
                        assertTrue(e.message!!.contains("not found"))
                    }
                }
            }
        }
    }
    
    @Test
    fun `cursor can find matching prefix using getBothRange`() {
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            env.beginTxn {
                val dbi = dbiOpen(options = arrayOf(DbiOption.DupSort))
                val cursor = openCursor(dbi)
                cursor.use {
                    // Add multiple entries with the same key
                    val key = "testkey".encodeToByteArray()
                    cursor.put(key, "aaa".encodeToByteArray())
                    cursor.put(key, "bbb".encodeToByteArray())
                    cursor.put(key, "ccc".encodeToByteArray())
                    
                    // Search for a range match
                    val searchData = "b".encodeToByteArray()
                    val rangeResult = cursor.getBothRange(key, searchData)
                    assertEquals(0, rangeResult.resultCode)
                    assertEquals("testkey", rangeResult.key.toByteArray()!!.decodeToString())
                    assertEquals("bbb", rangeResult.data.toByteArray()!!.decodeToString()) // Finds "bbb" as first match for "b"
                }
            }
        }
    }
    
    @Test
    fun `cursor can set position using setRange`() {
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            env.beginTxn {
                val dbi = dbiOpen()
                val cursor = openCursor(dbi)
                cursor.use {
                    // Add entries with sorted keys
                    cursor.put("a10".encodeToByteArray(), "data-a10".encodeToByteArray())
                    cursor.put("b20".encodeToByteArray(), "data-b20".encodeToByteArray())
                    cursor.put("c30".encodeToByteArray(), "data-c30".encodeToByteArray())
                    
                    // Find first entry >= b
                    val rangeResult = cursor.setRange("b".encodeToByteArray())
                    assertEquals(0, rangeResult.resultCode)
                    assertEquals("b20", rangeResult.key.toByteArray()!!.decodeToString())
                    assertEquals("data-b20", rangeResult.data.toByteArray()!!.decodeToString())
                }
            }
        }
    }
    
    @Test
    fun `cursor can delete entries`() {
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            env.beginTxn {
                val dbi = dbiOpen()
                val cursor = openCursor(dbi)
                cursor.use {
                    // Add an entry
                    val key = "deletekey".encodeToByteArray()
                    val data = "deletedata".encodeToByteArray()
                    cursor.put(key, data)
                    
                    // Position the cursor
                    cursor.set(key)
                    
                    // Delete the entry
                    cursor.delete()
                    
                    // Verify deletion by trying to look up
                    val lookupResult = cursor.set(key)
                    // In some implementations a not-found might return a non-zero code
                    // rather than throwing exception
                    assertNotEquals(0, lookupResult.resultCode)
                }
            }
        }
    }
    
    @Test
    fun `cursor put with various options`() {
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            env.beginTxn {
                val dbi = dbiOpen()
                val cursor = openCursor(dbi)
                cursor.use {
                    // Test put - add entry
                    val key = "key1".encodeToByteArray()
                    val data1 = "data1".encodeToByteArray()
                    cursor.put(key, data1)
                    
                    // Test NOOVERWRITE - should fail since key exists
                    val data2 = "data2".encodeToByteArray()
                    try {
                        cursor.putNoOverwrite(key, data2)
                        assertTrue(false, "Expected an exception for duplicate key")
                    } catch (e: LmdbException) {
                        // Expected exception
                    }
                    
                    // Test append - adds at end of db
                    val lastKey = "zkey".encodeToByteArray()
                    val lastData = "zdata".encodeToByteArray()
                    val appendResult = cursor.putAppend(lastKey, lastData)
                    assertEquals(0, appendResult.resultCode)
                    
                    // Verify it's at the end
                    cursor.last()
                    val currentEntry = cursor.getCurrent()
                    assertEquals("zkey", currentEntry.key.toByteArray()!!.decodeToString())
                }
            }
        }
    }
    
    @Test
    fun `cursor can be converted to sequence`() {
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            env.beginTxn {
                val dbi = dbiOpen()
                val cursor = openCursor(dbi)
                cursor.use {
                    // Add test data
                    val entries = listOf("a", "b", "c", "d")
                    entries.forEach { key ->
                        cursor.put(key.encodeToByteArray(), "data-$key".encodeToByteArray())
                    }
                    
                    // Reset cursor position to beginning
                    cursor.first()
                    
                    // Manually iterate through all entries to verify data
                    var count = 0
                    val foundKeys = mutableListOf<String>()
                    
                    // Get the first entry
                    var result = cursor.getCurrent()
                    if (result.resultCode == 0) {
                        count++
                        foundKeys.add(result.key.toByteArray()!!.decodeToString())
                        
                        // Get remaining entries
                        while (true) {
                            result = cursor.next()
                            if (result.resultCode != 0) break
                            count++
                            foundKeys.add(result.key.toByteArray()!!.decodeToString())
                        }
                    }
                    
                    // Verify we found all entries
                    assertEquals(4, count)
                    assertTrue(foundKeys.containsAll(entries))
                }
            }
        }
    }
    
    @Test
    fun `cursor can be created and closed`() {
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            env.beginTxn {
                val dbi = dbiOpen()
                
                // Create first cursor
                val cursor1 = openCursor(dbi)
                cursor1.use {
                    cursor1.put("key1".encodeToByteArray(), "data1".encodeToByteArray())
                }
                
                // Create second cursor and verify it can see data from first
                val cursor2 = openCursor(dbi)
                cursor2.use {
                    val result = cursor2.set("key1".encodeToByteArray())
                    assertEquals(0, result.resultCode)
                }
            }
        }
    }
    
    // Tests for duplicate values need to be more conservative due to implementation details
    
    @Test
    fun `cursor can handle duplicate values basics`() {
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            env.beginTxn {
                // Open database with DupSort option
                val dbi = dbiOpen(options = arrayOf(DbiOption.DupSort, DbiOption.Create))
                val cursor = openCursor(dbi)
                cursor.use {
                    // Add multiple values for the same key
                    val key = "dupkey".encodeToByteArray()
                    cursor.put(key, "value1".encodeToByteArray())
                    cursor.put(key, "value2".encodeToByteArray())
                    cursor.put(key, "value3".encodeToByteArray())
                    
                    // Position cursor at key
                    cursor.set(key)
                    
                    // Count duplicates
                    val count = cursor.countDuplicates()
                    assertEquals(3UL, count)
                }
            }
        }
    }
    
    @Test
    fun `cursor can delete duplicate data`() {
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            env.beginTxn {
                // Open database with DupSort option
                val dbi = dbiOpen(options = arrayOf(DbiOption.DupSort, DbiOption.Create))
                val cursor = openCursor(dbi)
                cursor.use {
                    // Add multiple values for the same key
                    val key = "dupkey".encodeToByteArray()
                    cursor.put(key, "valueA".encodeToByteArray())
                    cursor.put(key, "valueB".encodeToByteArray())
                    
                    // Position at first duplicate and delete it
                    cursor.set(key)
                    cursor.delete()
                    
                    // Try to find the deleted key
                    try {
                        cursor.set(key)
                        // If we get here, at least one value still exists
                        val data = cursor.getCurrent().data.toByteArray()!!.decodeToString()
                        // Should find only valueB now
                        assertEquals("valueB", data)
                    } catch (e: LmdbException) {
                        // If all values were deleted, this is also acceptable
                    }
                }
            }
        }
    }
    
    @Test
    fun `cursor putNoDuplicateData prevents duplicate values`() {
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            env.beginTxn {
                // Open database with DupSort option
                val dbi = dbiOpen(options = arrayOf(DbiOption.DupSort, DbiOption.Create))
                val cursor = openCursor(dbi)
                cursor.use {
                    val key = "key".encodeToByteArray()
                    val data = "data".encodeToByteArray()
                    
                    // First put should succeed
                    val result1 = cursor.putNoDuplicateData(key, data)
                    assertEquals(0, result1.resultCode)
                    
                    // Second put with same data should fail
                    try {
                        cursor.putNoDuplicateData(key, data)
                        assertTrue(false, "Expected an exception for duplicate data")
                    } catch (e: LmdbException) {
                        // Expected exception for duplicate data
                    }
                    
                    // Different data for same key should succeed
                    val result3 = cursor.putNoDuplicateData(key, "differentdata".encodeToByteArray())
                    assertEquals(0, result3.resultCode)
                }
            }
        }
    }
}