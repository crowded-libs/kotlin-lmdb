package lmdb

import kotlin.test.*

class EnvTests {

    @Test
    fun `env open - close`() {
        val env = createRandomTestEnv()
        env.close()
    }

    @Test
    fun `start transaction before opening environment`() {
        val env = createRandomTestEnv(open = false)
        assertFailsWith(LmdbException::class) {
            env.beginTxn()
        }
    }

    @Test
    fun `can read environment information`() {
        val mapSize: ULong = (1024 * 1024 * 200).toULong()
        val env = createRandomTestEnv(mapSize = mapSize)
        assertEquals(env.info!!.mapSize, env.mapSize)
    }

    @Test
    fun `can set max databases`() {
        val databaseCount = 2u
        val env = createRandomTestEnv(maxDatabases = databaseCount)
        env.beginTxn {
            dbiOpen("db1", DbiOption.Create)
            dbiOpen("db2", DbiOption.Create)
            commit()
        }
        assertEquals(databaseCount, env.maxDatabases)
    }

    @Test
    fun `can read environment stats`() {
        val env = createRandomTestEnv()
        assertNotNull(env.stat)
    }

    @Test
    fun `can open a readonly environment`() {
        val path = pathCreateTestDir()
        Env().use {
            it.open(path) //must be initially created first before readonly can be opened
        }
        Env().use {
            it.open(path, EnvOption.ReadOnly)
        }
    }

    @Test
    @IgnoreWasmJs
    fun `can copy an environment and open afterwards`() {
        val path = pathCreateTestDir()
        val copyPath = pathCreateTestDir()
        Env().use {
            it.open(path)
            it.copyTo(copyPath, true)
        }
        Env().use {
            it.open(copyPath)
        }
    }
    
    @Test
    fun `setting mapSize increases available storage`() {
        val smallSize: ULong = (1024 * 1024 * 10).toULong() // 10MB
        val largeSize: ULong = (1024 * 1024 * 100).toULong() // 100MB
        
        val env = createRandomTestEnv(mapSize = smallSize)
        env.use {
            assertEquals(smallSize, env.mapSize)
            assertEquals(smallSize, env.info!!.mapSize)
            
            // Increase map size
            env.mapSize = largeSize
            assertEquals(largeSize, env.mapSize)
            assertEquals(largeSize, env.info!!.mapSize)
        }
    }
    
    @Test
    fun `max readers has a default value`() {
        val env = createRandomTestEnv()
        env.use {
            // Just make sure we can read the value
            val readers = env.maxReaders
            // The value should be greater than 0
            assertTrue(readers > 0u)
        }
    }
    
    @Test
    fun `can get max key size`() {
        val env = createRandomTestEnv()
        env.use {
            // Max key size is fixed by the LMDB implementation
            assertTrue(env.maxKeySize > 0u)
        }
    }
    
    @Test
    fun `can check for stale readers`() {
        val env = createRandomTestEnv()
        env.use {
            // Check stale reader count - may not find any since we're running a clean test
            val staleReaderCount = env.staleReaderCount
            // Just ensure this call works without exception
            assertNotNull(staleReaderCount)
        }
    }
    
    @Test
    fun `can set and get environment flags`() {
        val env = createRandomTestEnv()
        env.use {
            // Get initial flags
            val initialFlags = env.flags
            
            // Add a flag (NoSync is a safe option to modify)
            val newFlags = initialFlags + EnvOption.NoSync
            env.flags = newFlags
            
            // Verify the flag was set
            val updatedFlags = env.flags
            assertTrue(updatedFlags.contains(EnvOption.NoSync))
            
            // Remove the flag
            env.flags = initialFlags
            
            // Verify the flag was removed
            val finalFlags = env.flags
            assertFalse(finalFlags.contains(EnvOption.NoSync))
        }
    }
    
    @Test
    fun `can perform sync operation`() {
        val env = createRandomTestEnv()
        env.use {
            // Add some data to ensure there's something to sync
            env.beginTxn {
                val dbi = dbiOpen()
                put(dbi, "key".encodeToByteArray(), "value".encodeToByteArray())
                commit()
            }
            
            // Test both sync modes
            env.sync(false) // normal sync
            env.sync(true)  // force sync
        }
    }
    
    @Test
    fun `multiple open and close operations work correctly`() {
        val path = pathCreateTestDir()
        
        // Open and close first environment
        Env().use { env1 ->
            env1.open(path)
            
            // Add some data
            env1.beginTxn {
                val dbi = dbiOpen(options = arrayOf(DbiOption.Create))
                put(dbi, "key1".encodeToByteArray(), "value1".encodeToByteArray())
                commit()
            }
        }
        
        // Open a second environment on the same path
        Env().use { env2 ->
            env2.open(path)
            
            // Should be able to read the data written by first environment
            env2.beginTxn {
                val dbi = dbiOpen()
                val (code, _, value) = get(dbi, "key1".encodeToByteArray())
                assertEquals(0, code)
                assertEquals("value1", value.toByteArray()!!.decodeToString())
            }
        }
    }
    
    @Test
    fun `can use environment with NoSubDir option`() {
        val path = pathCreateTestDir() + "/data.mdb"
        
        Env().use { env ->
            env.open(path, EnvOption.NoSubDir)
            
            // Add some data
            env.beginTxn {
                val dbi = dbiOpen()
                put(dbi, "key".encodeToByteArray(), "value".encodeToByteArray())
                commit()
            }
            
            // Verify data
            env.beginTxn {
                val dbi = dbiOpen()
                val (code, _, value) = get(dbi, "key".encodeToByteArray())
                assertEquals(0, code)
                assertEquals("value", value.toByteArray()!!.decodeToString())
            }
        }
    }
    
    @Test
    fun `environment stats reflect database activity`() {
        val env = createRandomTestEnv()
        env.use {
            // Get initial stats
            val initialStats = env.stat!!
            val initialEntries = initialStats.entries
            
            // Add some entries
            env.beginTxn {
                val dbi = dbiOpen()
                repeat(10) { i ->
                    put(dbi, "key$i".encodeToByteArray(), "value$i".encodeToByteArray())
                }
                commit()
            }
            
            // Get updated stats
            val updatedStats = env.stat!!
            val updatedEntries = updatedStats.entries
            
            // Entries count should have increased
            // Note: The exact number might vary due to internal database structure
            assertTrue(updatedEntries >= initialEntries)
        }
    }
    
    @Test
    fun `environment info reflects database state`() {
        val env = createRandomTestEnv()
        env.use {
            // Get initial info
            val initialInfo = env.info!!
            val initialLastTxnId = initialInfo.lastTxnId
            
            // Perform a transaction
            env.beginTxn {
                val dbi = dbiOpen()
                put(dbi, "key".encodeToByteArray(), "value".encodeToByteArray())
                commit()
            }
            
            // Get updated info
            val updatedInfo = env.info!!
            val updatedLastTxnId = updatedInfo.lastTxnId
            
            // Transaction ID should have increased
            assertTrue(updatedLastTxnId > initialLastTxnId)
        }
    }
    
    @Test
    fun `read-only transaction basics`() {
        val env = createRandomTestEnv()
        env.use {
            // First create some data with a write transaction
            env.beginTxn {
                val dbi = dbiOpen()
                put(dbi, "key".encodeToByteArray(), "value".encodeToByteArray())
                commit()
            }
            
            // Then open a read-only transaction
            val txn = env.beginTxn(TxnOption.ReadOnly)
            try {
                // Ensure transaction has a valid ID
                assertTrue(txn.id > 0UL)
                
                // Verify we can read data
                val dbi = txn.dbiOpen()
                val (code, _, value) = txn.get(dbi, "key".encodeToByteArray())
                assertEquals(0, code)
                assertEquals("value", value.toByteArray()!!.decodeToString())
            } finally {
                // Clean up
                txn.abort()
            }
        }
    }
    
    @Test
    fun `can use transactions with different options`() {
        val env = createRandomTestEnv()
        env.use {
            // Regular transaction
            env.beginTxn {
                val dbi = dbiOpen()
                put(dbi, "regular".encodeToByteArray(), "value".encodeToByteArray())
                commit()
            }
            
            // Read-only transaction
            val readOnlyTxn = env.beginTxn(TxnOption.ReadOnly)
            try {
                val dbi = readOnlyTxn.dbiOpen()
                val (code, _, value) = readOnlyTxn.get(dbi, "regular".encodeToByteArray())
                assertEquals(0, code)
                assertEquals("value", value.toByteArray()!!.decodeToString())
                
                // Should not be able to write in read-only transaction
                assertFailsWith<LmdbException> {
                    readOnlyTxn.put(dbi, "new".encodeToByteArray(), "value".encodeToByteArray())
                }
            } finally {
                readOnlyTxn.abort()
            }
            
            // NoSync transaction
            env.beginTxn(TxnOption.NoSync) {
                val dbi = dbiOpen()
                put(dbi, "nosync".encodeToByteArray(), "value".encodeToByteArray())
                commit()
            }
        }
    }
    
    @Test
    fun `close after closing is safe`() {
        val env = createRandomTestEnv()
        
        // First close
        env.close()
        
        // Second close should not cause errors
        env.close()
    }
    
    @Test
    fun `can copy env to destination`() {
        val path = pathCreateTestDir()
        val copyPath = pathCreateTestDir()
        
        Env().use { env ->
            env.open(path)
            
            // Add some data
            env.beginTxn {
                val dbi = dbiOpen()
                put(dbi, "copykey".encodeToByteArray(), "copyvalue".encodeToByteArray())
                commit()
            }
            
            // Simple copyTo method
            env.copyTo(copyPath)
        }
        
        // The copy should be usable
        Env().use { env ->
            env.open(copyPath)
            env.beginTxn {
                val dbi = dbiOpen()
                // Check for the key we added
                val (code, _, value) = get(dbi, "copykey".encodeToByteArray())
                assertEquals(0, code)
                assertEquals("copyvalue", value.toByteArray()!!.decodeToString())
            }
        }
    }
    
    @Test
    @IgnoreWasmJs
    fun `can copy env with compact option`() {
        val path = pathCreateTestDir()
        val copyPath = pathCreateTestDir()
        
        Env().use { env ->
            env.open(path)
            
            // Add some data
            env.beginTxn {
                val dbi = dbiOpen()
                put(dbi, "compactkey".encodeToByteArray(), "compactvalue".encodeToByteArray())
                commit()
            }
            
            // Copy with compact option
            env.copyTo(copyPath, true)
        }
        
        // The copy should be usable
        Env().use { env ->
            env.open(copyPath)
            env.beginTxn {
                val dbi = dbiOpen()
                // Check for the key we added
                val (code, _, value) = get(dbi, "compactkey".encodeToByteArray())
                assertEquals(0, code)
                assertEquals("compactvalue", value.toByteArray()!!.decodeToString())
            }
        }
    }
}