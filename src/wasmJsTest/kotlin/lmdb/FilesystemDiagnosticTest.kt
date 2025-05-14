package lmdb

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FilesystemDiagnosticTest {
    
    @Test
    fun testFilesystemPersistence() {
        println("\n=== Filesystem Persistence Diagnostic ===")
        
        // Create a test directory
        val testDir = "/tmp/fs-diagnostic-${Uuid.random()}"
        println("Creating test directory: $testDir")
        
        // Test 1: Check if directory creation works
        val dirCreated = WasmUtils.createDirectory(testDir)
        println("Directory created: $dirCreated")
        
        // Test 2: Check if directory exists
        val dirExists = WasmUtils.pathExists(testDir)
        println("Directory exists: $dirExists")
        
        // Test 3: Try to create and open an LMDB environment
        try {
            val env = Env()
            env.maxDatabases = 10u
            env.open(testDir)
            println("LMDB environment opened successfully")
            
            // Test 4: Write data in a transaction and read before commit
            val txn = env.beginTxn()
            val dbi = txn.dbiOpen(null)
            txn.put(dbi, "test-key".encodeToByteArray().toVal(), "test-value".encodeToByteArray().toVal())
            println("Data written in transaction")
            
            // Test 4a: Try to read data in same transaction before commit
            val resultBeforeCommit = txn.get(dbi, "test-key".encodeToByteArray().toVal())
            val valueBeforeCommit = if (resultBeforeCommit.resultCode == 0) {
                val bytes = resultBeforeCommit.data.toByteArray()
                val value = bytes?.decodeToString() ?: "NULL_BYTES"
                println("Read before commit - raw bytes: ${bytes?.joinToString(",") { it.toString() } ?: "null"}")
                value
            } else "NOT_FOUND"
            println("Read before commit: code=${resultBeforeCommit.resultCode}, value=$valueBeforeCommit")
            
            txn.commit()
            println("Transaction committed")
            
            // Test 5: Try to read data in a new transaction
            env.beginTxn(TxnOption.ReadOnly).use { txn2 ->
                val dbi2 = txn2.dbiOpen(null)
                println("Original DBI pointer: ${dbi.ptr}, New DBI pointer: ${dbi2.ptr}")
                val result = txn2.get(dbi2, "test-key".encodeToByteArray().toVal())
                val value = if (result.resultCode == 0) result.data.toString() else "NOT_FOUND"
                println("Read result: code=${result.resultCode}, value=$value")
                // Don't fail the test yet, let's see what happens
                if (result.resultCode != 0) {
                    println("WARNING: Data not found in new transaction!")
                } else {
                    assertEquals("test-value", value, "Should get correct value")
                }
            }
            
            // Test 5b: Try using the original DBI handle in a new transaction
            env.beginTxn(TxnOption.ReadOnly).use { txn3 ->
                try {
                    val result = txn3.get(dbi, "test-key".encodeToByteArray().toVal())
                    val value = if (result.resultCode == 0) result.data.toString() else "NOT_FOUND"
                    println("Read with original DBI: code=${result.resultCode}, value=$value")
                } catch (e: Exception) {
                    println("Error using original DBI: ${e.message}")
                }
            }
            
            env.close()
            
            // Test 6: Reopen environment and check persistence
            val env2 = Env()
            env2.maxDatabases = 10u
            env2.open(testDir)
            println("LMDB environment reopened")
            
            env2.beginTxn(TxnOption.ReadOnly).use { txn ->
                val dbi = txn.dbiOpen(null)
                val result = txn.get(dbi, "test-key".encodeToByteArray().toVal())
                val value = if (result.resultCode == 0) result.data.toString() else "NOT_FOUND"
                println("Read after reopen: code=${result.resultCode}, value=$value")
                assertEquals(0, result.resultCode, "Should find the key after reopen")
                assertEquals("test-value", value, "Should get correct value after reopen")
            }
            
            env2.close()
            
        } catch (e: Exception) {
            println("Exception: ${e.message}")
            throw e
        }
        
        println("=== End Filesystem Persistence Diagnostic ===\n")
    }
    
    @Test
    fun testSimpleFileOperations() {
        println("\n=== Simple File Operations Diagnostic ===")
        
        // Test basic file operations to understand the filesystem
        val testPath = "/tmp/test-file-${Uuid.random()}"
        
        println("Testing path: $testPath")
        
        // Check if /tmp exists
        val tmpExists = WasmUtils.pathExists("/tmp")
        println("/tmp exists: $tmpExists")
        
        // Try to create a file using WASM memory operations
        try {
            val content = "Hello NODEFS"
            val contentPtr = WasmUtils.stringToPtr(content)
            
            // Check if we can read it back
            val readBack = WasmUtils.ptrToString(contentPtr)
            println("Memory test - wrote: '$content', read: '$readBack'")
            assertEquals(content, readBack, "Memory operations should work")
            
            WasmUtils.free(contentPtr)
        } catch (e: Exception) {
            println("Memory test failed: ${e.message}")
        }
        
        println("=== End Simple File Operations Diagnostic ===\n")
    }
}