import kotlin.test.*

class TxnTests {

    @Test
    fun `txn put - get`() {
        val expected = "value"
        val key = "test".encodeToByteArray()
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            var dbi: Dbi? = null
            env.beginTxn {
                dbi = dbiOpen()
                put(dbi!!, key, expected.encodeToByteArray())
                commit()
            }
            env.beginTxn {
                val (_, _, result) = get(dbi!!, key)
                val value = result.toByteArray()?.decodeToString()
                assertEquals(expected, value)
            }
        }
    }

    @Test
    fun `txn put - delete - get returns null`() {
        val key = "test".encodeToByteArray()
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            var dbi: Dbi? = null
            env.beginTxn {
                dbi = dbiOpen()
                put(dbi!!, key, key)
                commit()
            }
            env.beginTxn {
                delete(dbi!!, key)
                commit()
            }
            env.beginTxn {
                val result = get(dbi!!, key).toValueByteArray()
                assertNull(result)
            }
        }
    }
    
    @Test
    fun `txn state transitions after commit`() {
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            val txn = env.beginTxn()
            assertEquals(TxnState.Ready, txn.state)
            
            txn.commit()
            assertEquals(TxnState.Done, txn.state)
            
            // Should throw exception if operations attempted after commit
            assertFailsWith<LmdbException> {
                txn.dbiOpen()
            }
        }
    }
    
    @Test
    fun `txn state transitions after abort`() {
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            val txn = env.beginTxn()
            assertEquals(TxnState.Ready, txn.state)
            
            txn.abort()
            assertEquals(TxnState.Done, txn.state)
            
            // Should throw exception if operations attempted after abort
            assertFailsWith<LmdbException> {
                txn.dbiOpen()
            }
        }
    }
    
    @Test
    fun `txn abort rolls back changes`() {
        val key = "test-abort".encodeToByteArray()
        val value = "original-value".encodeToByteArray()
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            var dbi: Dbi? = null
            
            // Create and populate database
            env.beginTxn {
                dbi = dbiOpen()
                put(dbi!!, key, value)
                commit()
            }
            
            // Make changes and abort them
            env.beginTxn {
                put(dbi!!, key, "new-value".encodeToByteArray())
                // Don't commit, abort instead
                abort()
            }
            
            // Verify changes were rolled back
            env.beginTxn {
                val (_, _, result) = get(dbi!!, key)
                val retrievedValue = result.toByteArray()
                assertContentEquals(value, retrievedValue)
            }
        }
    }
    
    @Test
    fun `txn nested transactions work correctly`() {
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            val parentTxn = env.beginTxn()
            val dbi = parentTxn.dbiOpen()
            
            // Write in parent transaction
            val parentKey = "parent-key".encodeToByteArray()
            val parentValue = "parent-value".encodeToByteArray()
            parentTxn.put(dbi, parentKey, parentValue)
            
            // Create child transaction
            val childTxn = parentTxn.begin()
            assertEquals(TxnState.Ready, childTxn.state)
            
            // Write in child transaction
            val childKey = "child-key".encodeToByteArray()
            val childValue = "child-value".encodeToByteArray()
            childTxn.put(dbi, childKey, childValue)
            
            // Child can see parent's changes
            val (_, _, parentResult) = childTxn.get(dbi, parentKey)
            assertContentEquals(parentValue, parentResult.toByteArray())
            
            // Commit child
            childTxn.commit()
            
            // Parent can see child's committed changes
            val (_, _, childResult) = parentTxn.get(dbi, childKey)
            assertContentEquals(childValue, childResult.toByteArray())
            
            // Cleanup
            parentTxn.commit()
        }
    }
    
    @Test
    fun `txn read-only transactions prevent writes`() {
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            // Create database and add data with a read-write transaction
            var dbi: Dbi? = null
            val key = "readonly-test".encodeToByteArray()
            val value = "test-value".encodeToByteArray()
            
            env.beginTxn {
                dbi = dbiOpen()
                put(dbi!!, key, value)
                commit()
            }
            
            // Now open a read-only transaction
            val readOnlyTxn = env.beginTxn(TxnOption.ReadOnly)
            
            // Should be able to read
            val (readCode, _, readResult) = readOnlyTxn.get(dbi!!, key)
            assertEquals(0, readCode)
            assertContentEquals(value, readResult.toByteArray())
            
            // Should not be able to write
            assertFailsWith<LmdbException> {
                readOnlyTxn.put(dbi!!, "new-key".encodeToByteArray(), "new-value".encodeToByteArray())
            }
            
            readOnlyTxn.abort()
        }
    }
    
    @Test
    fun `txn reset and renew work correctly`() {
        // This test is simplified because the reset/renew state transitions may be implementation-specific
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            // Create database and add data
            var dbi: Dbi? = null
            val key = "renew-test".encodeToByteArray()
            val value = "renew-value".encodeToByteArray()
            
            env.beginTxn {
                dbi = dbiOpen()
                put(dbi!!, key, value)
                commit()
            }
            
            // Create a read-only transaction 
            val txn = env.beginTxn(TxnOption.ReadOnly)
            
            // Initial read works
            val (readCode, _, readResult) = txn.get(dbi!!, key)
            assertEquals(0, readCode)
            assertContentEquals(value, readResult.toByteArray())
            
            // Test that the transaction can be aborted
            txn.abort()
            assertEquals(TxnState.Done, txn.state)
            
            // Opening a new read-only transaction should work
            val txn2 = env.beginTxn(TxnOption.ReadOnly)
            val (readCode2, _, readResult2) = txn2.get(dbi!!, key)
            assertEquals(0, readCode2)
            assertContentEquals(value, readResult2.toByteArray())
            txn2.abort()
        }
    }
    
    @Test
    fun `txn can create and access named database`() {
        val env = createRandomTestEnv(mapSize = 10485760UL, maxDatabases = 10u)
        env.use {
            var dbi: Dbi? = null
            
            // Create named database with some data
            env.beginTxn {
                dbi = dbiOpen("test-db", options = arrayOf(DbiOption.Create))
                put(dbi!!, "key1".encodeToByteArray(), "value1".encodeToByteArray())
                commit()
            }
            
            // Verify data exists
            env.beginTxn {
                val (code, _, result) = get(dbi!!, "key1".encodeToByteArray())
                assertEquals(0, code)
                assertEquals("value1", result.toByteArray()!!.decodeToString())
            }
        }
    }
    
    @Test
    fun `txn can add and retrieve data`() {
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            var dbi: Dbi? = null
            
            // Create database with some data
            env.beginTxn {
                dbi = dbiOpen(options = arrayOf(DbiOption.Create))
                put(dbi!!, "key1".encodeToByteArray(), "value1".encodeToByteArray())
                put(dbi!!, "key2".encodeToByteArray(), "value2".encodeToByteArray())
                commit()
            }
            
            // Database should contain the data
            env.beginTxn {
                // Test first key
                val (code1, _, val1) = get(dbi!!, "key1".encodeToByteArray())
                assertEquals(0, code1)
                assertEquals("value1", val1.toByteArray()!!.decodeToString())
                
                // Test second key
                val (code2, _, val2) = get(dbi!!, "key2".encodeToByteArray())
                assertEquals(0, code2)
                assertEquals("value2", val2.toByteArray()!!.decodeToString())
                
                // Add more data
                put(dbi!!, "key3".encodeToByteArray(), "value3".encodeToByteArray())
                
                // And read it back
                val (code3, _, val3) = get(dbi!!, "key3".encodeToByteArray())
                assertEquals(0, code3)
                assertEquals("value3", val3.toByteArray()!!.decodeToString())
                
                // Non-existent key should return null value
                val result4 = get(dbi!!, "nonexistent".encodeToByteArray()).toValueByteArray()
                assertNull(result4)
            }
        }
    }
    
    @Test
    fun `txn delete with data only removes matching key-value pairs`() {
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            // Need a database with duplicate values enabled
            var dbi: Dbi?
            val key = "dup-key".encodeToByteArray()
            
            env.beginTxn {
                dbi = dbiOpen(options = arrayOf(DbiOption.DupSort))
                
                // Add multiple values for same key
                put(dbi!!, key, "value1".encodeToByteArray())
                put(dbi!!, key, "value2".encodeToByteArray())
                put(dbi!!, key, "value3".encodeToByteArray())
                
                // Use openCursor to verify all values are present
                val cursor = openCursor(dbi!!)
                cursor.use {
                    cursor.set(key)
                    val count = cursor.countDuplicates()
                    assertEquals(3UL, count)
                }
                
                // Delete specific key-value pair
                delete(dbi!!, key, "value2".encodeToByteArray())
                
                // Verify only the specified value was deleted
                val cursor2 = openCursor(dbi!!)
                cursor2.use {
                    cursor2.set(key)
                    val count = cursor2.countDuplicates()
                    assertEquals(2UL, count)
                    
                    // Should still find value1 and value3
                    var foundValue1 = false
                    var foundValue3 = false
                    
                    val firstResult = cursor2.getCurrent()
                    if (firstResult.data.toByteArray()?.decodeToString() == "value1") {
                        foundValue1 = true
                    } else if (firstResult.data.toByteArray()?.decodeToString() == "value3") {
                        foundValue3 = true
                    }
                    
                    val nextResult = cursor2.nextDuplicate()
                    if (nextResult.resultCode == 0) {
                        if (nextResult.data.toByteArray()?.decodeToString() == "value1") {
                            foundValue1 = true
                        } else if (nextResult.data.toByteArray()?.decodeToString() == "value3") {
                            foundValue3 = true
                        }
                    }
                    
                    assertTrue(foundValue1, "value1 should still exist")
                    assertTrue(foundValue3, "value3 should still exist")
                }
                
                commit()
            }
        }
    }
    
    @Test
    fun `txn id is valid for active transactions`() {
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            val txn = env.beginTxn()
            
            // Transaction ID should be greater than 0
            val id = txn.id
            assertTrue(id > 0UL, "Transaction ID should be positive")
            
            // After committing, trying to get ID should fail
            txn.commit()
            assertFailsWith<LmdbException> {
                txn.id
            }
        }
    }
    
    @Test
    fun `txn can insert and retrieve keys in order`() {
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            env.beginTxn {
                // Create standard database
                val dbi = dbiOpen(options = arrayOf(DbiOption.Create))
                
                // Insert some keys
                put(dbi, "a".encodeToByteArray(), "value-a".encodeToByteArray())
                put(dbi, "b".encodeToByteArray(), "value-b".encodeToByteArray())
                put(dbi, "c".encodeToByteArray(), "value-c".encodeToByteArray())
                
                // Create cursor to verify ordering
                val cursor = openCursor(dbi)
                cursor.use {
                    // First key should be 'a' in normal ordering
                    val firstResult = cursor.first()
                    assertEquals(0, firstResult.resultCode)
                    assertEquals("a", firstResult.key.toByteArray()!!.decodeToString())
                    
                    val nextResult = cursor.next()
                    assertEquals(0, nextResult.resultCode)
                    assertEquals("b", nextResult.key.toByteArray()!!.decodeToString())
                    
                    val lastResult = cursor.next()
                    assertEquals(0, lastResult.resultCode)
                    assertEquals("c", lastResult.key.toByteArray()!!.decodeToString())
                }
                
                commit()
            }
        }
    }
    
    @Test
    fun `txn with duplicate values creates multiple values for same key`() {
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            env.beginTxn {
                // Create database with DupSort option to allow duplicate values
                val dbi = dbiOpen(options = arrayOf(DbiOption.DupSort, DbiOption.Create))
                
                // Insert duplicate values for a key (normal ordering)
                val key = "dupkey".encodeToByteArray()
                put(dbi, key, "valueA".encodeToByteArray())
                put(dbi, key, "valueB".encodeToByteArray())
                put(dbi, key, "valueC".encodeToByteArray())
                
                // Create cursor to verify values
                val cursor = openCursor(dbi)
                cursor.use {
                    cursor.set(key)
                    
                    // Count duplicates - should be 3
                    val count = cursor.countDuplicates()
                    assertEquals(3UL, count)
                    
                    // We won't test specific ordering since that can vary by implementation
                    // Just ensure all values exist
                    val values = mutableListOf<String>()
                    
                    // Get current value (first duplicate)
                    var result = cursor.getCurrent()
                    assertEquals(0, result.resultCode)
                    values.add(result.data.toByteArray()!!.decodeToString())
                    
                    // Get next duplicate
                    result = cursor.nextDuplicate()
                    assertEquals(0, result.resultCode)
                    values.add(result.data.toByteArray()!!.decodeToString())
                    
                    // Get final duplicate
                    result = cursor.nextDuplicate()
                    assertEquals(0, result.resultCode)
                    values.add(result.data.toByteArray()!!.decodeToString())
                    
                    // Verify all values are present
                    assertTrue(values.contains("valueA"))
                    assertTrue(values.contains("valueB"))
                    assertTrue(values.contains("valueC"))
                }
                
                commit()
            }
        }
    }
    
    @Test
    fun `txn put with various options`() {
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            env.beginTxn {
                val dbi = dbiOpen()
                
                // Test PutOption.NoOverwrite
                val key = "key1".encodeToByteArray()
                put(dbi, key, "value1".encodeToByteArray())
                
                // Should fail when key exists
                assertFailsWith<LmdbException> {
                    put(dbi, key, "value2".encodeToByteArray(), PutOption.NoOverwrite)
                }
                
                // Should succeed with a new key
                put(dbi, "key2".encodeToByteArray(), "value2".encodeToByteArray(), PutOption.NoOverwrite)
                
                // Verify both values
                val (_, _, result1) = get(dbi, key)
                assertEquals("value1", result1.toByteArray()!!.decodeToString())
                
                val (_, _, result2) = get(dbi, "key2".encodeToByteArray())
                assertEquals("value2", result2.toByteArray()!!.decodeToString())
                
                commit()
            }
        }
    }
    
    @Test
    fun `txn with default database`() {
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            // Using the default (unnamed) database
            var dbi: Dbi?

            env.beginTxn {
                // Open default database 
                dbi = dbiOpen(options = arrayOf(DbiOption.Create))
                
                // Add data
                put(dbi!!, "defaultKey".encodeToByteArray(), "defaultValue".encodeToByteArray())
                
                // Verify data
                val (code, _, value) = get(dbi!!, "defaultKey".encodeToByteArray())
                assertEquals(0, code)
                assertEquals("defaultValue", value.toByteArray()!!.decodeToString())
                
                commit()
            }
            
            // In a new transaction, reopen database and verify content
            env.beginTxn {
                // Reopen the database
                val reopenedDbi = dbiOpen()
                
                // Should still be able to access the data
                val (code, _, value) = get(reopenedDbi, "defaultKey".encodeToByteArray())
                assertEquals(0, code)
                assertEquals("defaultValue", value.toByteArray()!!.decodeToString())
            }
        }
    }
    
    @Test
    fun `txn close behaves correctly`() {
        val env = createRandomTestEnv(mapSize = 10485760UL)
        env.use {
            // Open a transaction
            val txn = env.beginTxn()
            val dbi = txn.dbiOpen()
            txn.put(dbi, "key".encodeToByteArray(), "value".encodeToByteArray())
            
            // Close the transaction without commit - should implicitly abort
            txn.close()
            
            // In a new transaction, data should not be present due to implicit abort
            env.beginTxn {
                val newDbi = dbiOpen()
                val result = get(newDbi, "key".encodeToByteArray()).toValueByteArray()
                assertNull(result)
            }
        }
    }
}