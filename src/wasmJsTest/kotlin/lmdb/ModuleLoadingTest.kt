package lmdb

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModuleLoadingTest {
    @Test
    fun canCallBasicFunctions() {
        // Test that we can call basic WASM functions
        val ptr = _malloc(8)
        assertNotNull(ptr)
        _free(ptr)
    }
    
    @Test
    fun canGetVersion() {
        val major = _malloc(4)
        val minor = _malloc(4)
        val patch = _malloc(4)
        
        try {
            val versionPtr = _mdb_version(major, minor, patch)
            assertNotNull(versionPtr)
        } finally {
            _free(major)
            _free(minor)
            _free(patch)
        }
    }
    
    @Test
    fun basicMemoryOperationsWork() {
        console.log("Testing basic memory operations...")
        
        // Test 1: Direct getValue/setValue with raw functions
        val ptr = _malloc(16)
        assertNotNull(ptr)
        console.log("Allocated memory at pointer: $ptr")
        
        try {
            // Test i32 operations (4 bytes)
            val testValue32 = 0x12345678
            _setValue(ptr, testValue32, 2) // TYPE_I32 = 2
            val readValue32 = _getValue(ptr, 2)
            console.log("Wrote i32: 0x${testValue32.toString(16)}, Read i32: 0x${readValue32.toString(16)}")
            assertEquals(testValue32, readValue32, "i32 value should match")
            
            // Test i16 operations (2 bytes) at offset 4
            val testValue16 = 0x5678
            _setValue(ptr + 4, testValue16, 1) // TYPE_I16 = 1
            val readValue16 = _getValue(ptr + 4, 1)
            console.log("Wrote i16: 0x${testValue16.toString(16)}, Read i16: 0x${readValue16.toString(16)}")
            assertEquals(testValue16, readValue16, "i16 value should match")
            
            // Test i8 operations (1 byte) at offset 8
            val testValue8 = 0x42
            _setValue(ptr + 8, testValue8, 0) // TYPE_I8 = 0
            val readValue8 = _getValue(ptr + 8, 0)
            console.log("Wrote i8: 0x${testValue8.toString(16)}, Read i8: 0x${readValue8.toString(16)}")
            assertEquals(testValue8, readValue8, "i8 value should match")
            
        } finally {
            _free(ptr)
        }
        
        console.log("Basic memory operations test completed successfully!")
    }
    
    @Test
    fun wasmUtilsMemoryOperationsWork() {
        console.log("Testing WasmUtils memory operations...")
        
        // Test 2: WasmUtils wrapper functions
        WasmUtils.withMemory(16) { ptr ->
            console.log("Allocated memory via WasmUtils at pointer: $ptr")
            
            // Test i32 operations using WasmUtils
            val testValue32 = 0xDEADBEEF.toInt()
            WasmUtils.setValue(ptr, testValue32, "i32")
            val readValue32 = WasmUtils.getValue(ptr, "i32")
            console.log("WasmUtils - Wrote i32: 0x${testValue32.toString(16)}, Read i32: 0x${readValue32.toString(16)}")
            assertEquals(testValue32, readValue32, "WasmUtils i32 value should match")
            
            // Test i16 operations using WasmUtils at offset 4
            val testValue16 = 0x1234
            WasmUtils.setValue(ptr + 4, testValue16, "i16")
            val readValue16 = WasmUtils.getValue(ptr + 4, "i16")
            console.log("WasmUtils - Wrote i16: 0x${testValue16.toString(16)}, Read i16: 0x${readValue16.toString(16)}")
            assertEquals(testValue16, readValue16, "WasmUtils i16 value should match")
            
            // Test i8 operations using WasmUtils at offset 8
            val testValue8 = 0x99
            WasmUtils.setValue(ptr + 8, testValue8, "i8")
            val readValue8 = WasmUtils.getValue(ptr + 8, "i8")
            console.log("WasmUtils - Wrote i8: 0x${testValue8.toString(16)}, Read i8: 0x${readValue8.toString(16)}")
            assertEquals(testValue8, readValue8, "WasmUtils i8 value should match")
        }
        
        console.log("WasmUtils memory operations test completed successfully!")
    }
    
    @Test
    fun memoryOverwriteTest() {
        console.log("Testing memory overwrite scenarios...")
        
        // Test 3: Verify that writing to memory actually overwrites previous values
        val ptr = _malloc(8)
        assertNotNull(ptr)
        
        try {
            // Write initial value
            val initialValue = 0x11111111
            _setValue(ptr, initialValue, 2) // i32
            val readInitial = _getValue(ptr, 2)
            assertEquals(initialValue, readInitial, "Initial value should match")
            console.log("Initial value: 0x${readInitial.toString(16)}")
            
            // Overwrite with new value
            val newValue = 0x22222222
            _setValue(ptr, newValue, 2) // i32
            val readNew = _getValue(ptr, 2)
            assertEquals(newValue, readNew, "New value should match")
            console.log("New value: 0x${readNew.toString(16)}")
            
            // Verify old value is completely gone
            assertTrue(readNew != readInitial, "New value should be different from initial")
            
        } finally {
            _free(ptr)
        }
        
        console.log("Memory overwrite test completed successfully!")
    }
}