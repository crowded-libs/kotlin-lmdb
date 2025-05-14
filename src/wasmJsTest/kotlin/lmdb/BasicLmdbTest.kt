package lmdb

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BasicLmdbTest {
    @Test
    fun canCreateEnvironment() {
        val envPtrPtr = _malloc(4)
        try {
            val result = _mdb_env_create(envPtrPtr)
            val envPtr = _getValue(envPtrPtr, 2) // TYPE_I32 = 2
            
            println("mdb_env_create result: $result")
            println("envPtr value: $envPtr")
            
            if (result != 0) {
                val errorPtr = _mdb_strerror(result)
                val errorMsg = WasmUtils.ptrToString(errorPtr)
                println("Error message: $errorMsg")
            }
            
            assertEquals(0, result, "mdb_env_create should return 0")
            assertNotEquals(0, envPtr, "Environment pointer should not be null")
            
            if (envPtr != 0) {
                _mdb_env_close(envPtr)
            }
        } finally {
            _free(envPtrPtr)
        }
    }
}