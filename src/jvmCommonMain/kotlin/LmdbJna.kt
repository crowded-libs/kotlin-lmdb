package lmdb

import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.LongByReference
import com.sun.jna.ptr.PointerByReference
import java.io.File
import java.nio.ByteBuffer

/**
 * JNA interface for LMDB native library.
 * This class provides JNA bindings to LMDB functions, replacing the JNI implementation.
 */
internal object LmdbJna {
    private val lib: LmdbLibrary by lazy {
        NativeLibraryLoader.loadLibrary()
    }

    // Environment management
    fun mdb_env_create(): Pointer {
        val envRef = PointerByReference()
        check(lib.mdb_env_create(envRef))
        return envRef.value ?: throw LmdbException("Failed to create environment")
    }

    fun mdb_env_open(env: Pointer, path: String, flags: Int, mode: Int): Int {
        return lib.mdb_env_open(env, path, flags, mode)
    }

    fun mdb_env_close(env: Pointer) {
        lib.mdb_env_close(env)
    }

    fun mdb_env_set_flags(env: Pointer, flags: Int, onoff: Int): Int {
        return lib.mdb_env_set_flags(env, flags, onoff)
    }

    fun mdb_env_get_flags(env: Pointer): Int {
        val flags = IntByReference()
        val rc = lib.mdb_env_get_flags(env, flags)
        if (rc != 0) return -rc // Return negative on error for compatibility
        return flags.value
    }

    fun mdb_env_set_mapsize(env: Pointer, size: Long): Int {
        return lib.mdb_env_set_mapsize(env, size)
    }

    fun mdb_env_set_maxreaders(env: Pointer, readers: Int): Int {
        return lib.mdb_env_set_maxreaders(env, readers)
    }

    fun mdb_env_set_maxdbs(env: Pointer, dbs: Int): Int {
        return lib.mdb_env_set_maxdbs(env, dbs)
    }

    fun mdb_env_get_maxreaders(env: Pointer): Int {
        val readers = IntByReference()
        val rc = lib.mdb_env_get_maxreaders(env, readers)
        if (rc != 0) return -rc // Return negative on error for compatibility
        return readers.value
    }

    fun mdb_env_get_maxkeysize(env: Pointer): Int {
        return lib.mdb_env_get_maxkeysize(env)
    }

    fun mdb_env_sync(env: Pointer, force: Int): Int {
        return lib.mdb_env_sync(env, force)
    }

    fun mdb_env_copy(env: Pointer, path: String): Int {
        return lib.mdb_env_copy(env, path)
    }

    fun mdb_env_copy2(env: Pointer, path: String, flags: Int): Int {
        return lib.mdb_env_copy2(env, path, flags)
    }

    fun mdb_env_info(env: Pointer, info: EnvInfo): Int {
        val mdbInfo = MDB_envinfo()
        val rc = lib.mdb_env_info(env, mdbInfo)
        if (rc == 0) {
            info.mapaddr = Pointer.nativeValue(mdbInfo.me_mapaddr)
            info.mapsize = mdbInfo.me_mapsize
            info.lastPgno = mdbInfo.me_last_pgno
            info.lastTxnid = mdbInfo.me_last_txnid
            info.maxReaders = mdbInfo.me_maxreaders
            info.numReaders = mdbInfo.me_numreaders
        }
        return rc
    }

    fun mdb_env_stat(env: Pointer, stat: Stat): Int {
        val mdbStat = MDB_stat()
        val rc = lib.mdb_env_stat(env, mdbStat)
        if (rc == 0) {
            stat.pageSize = mdbStat.ms_psize
            stat.depth = mdbStat.ms_depth
            stat.branchPages = mdbStat.ms_branch_pages
            stat.leafPages = mdbStat.ms_leaf_pages
            stat.overflowPages = mdbStat.ms_overflow_pages
            stat.entries = mdbStat.ms_entries
        }
        return rc
    }

    fun mdb_reader_check(env: Pointer): Int {
        val dead = IntByReference()
        val rc = lib.mdb_reader_check(env, dead)
        if (rc != 0) return -rc // Return negative on error for compatibility
        return dead.value
    }

    // Transaction management
    fun mdb_txn_begin(env: Pointer, parent: Pointer?, flags: Int): Pointer {
        val txnRef = PointerByReference()
        check(lib.mdb_txn_begin(env, parent, flags, txnRef))
        return txnRef.value ?: throw LmdbException("Failed to begin transaction")
    }

    fun mdb_txn_commit(txn: Pointer): Int {
        return lib.mdb_txn_commit(txn)
    }

    fun mdb_txn_abort(txn: Pointer) {
        lib.mdb_txn_abort(txn)
    }

    fun mdb_txn_reset(txn: Pointer) {
        lib.mdb_txn_reset(txn)
    }

    fun mdb_txn_renew(txn: Pointer): Int {
        return lib.mdb_txn_renew(txn)
    }

    fun mdb_txn_id(txn: Pointer): Long {
        return lib.mdb_txn_id(txn)
    }

    // Database operations
    fun mdb_dbi_open(txn: Pointer, name: String?, flags: Int): Int {
        val dbiRef = IntByReference()
        val rc = lib.mdb_dbi_open(txn, name, flags, dbiRef)
        if (rc != 0) return -rc // Return negative on error for compatibility
        return dbiRef.value
    }

    fun mdb_dbi_close(env: Pointer, dbi: Int) {
        lib.mdb_dbi_close(env, dbi)
    }

    fun mdb_dbi_flags(txn: Pointer, dbi: Int): Int {
        val flags = IntByReference()
        val rc = lib.mdb_dbi_flags(txn, dbi, flags)
        if (rc != 0) return -rc // Return negative on error for compatibility
        return flags.value
    }

    fun mdb_drop(txn: Pointer, dbi: Int, del: Int): Int {
        return lib.mdb_drop(txn, dbi, del)
    }

    fun mdb_stat(txn: Pointer, dbi: Int, stat: Stat): Int {
        val mdbStat = MDB_stat()
        val rc = lib.mdb_stat(txn, dbi, mdbStat)
        if (rc == 0) {
            stat.pageSize = mdbStat.ms_psize
            stat.depth = mdbStat.ms_depth
            stat.branchPages = mdbStat.ms_branch_pages
            stat.leafPages = mdbStat.ms_leaf_pages
            stat.overflowPages = mdbStat.ms_overflow_pages
            stat.entries = mdbStat.ms_entries
        }
        return rc
    }

    // Alternative get operation that returns the MDB_val directly
    fun mdb_get_direct(txn: Pointer, dbi: Int, key: ByteBuffer): Pair<Int, MDB_val?> {
        val keyVal = byteBufferToMdbVal(key)
        val dataVal = MDB_val()
        
        val rc = lib.mdb_get(txn, dbi, keyVal, dataVal)
        
        return if (rc == 0) {
            rc to dataVal
        } else {
            rc to null
        }
    }

    fun mdb_put(txn: Pointer, dbi: Int, key: ByteBuffer, data: ByteBuffer, flags: Int): Int {
        val keyVal = byteBufferToMdbVal(key)
        val dataVal = byteBufferToMdbVal(data)
        return lib.mdb_put(txn, dbi, keyVal, dataVal, flags)
    }

    fun mdb_del(txn: Pointer, dbi: Int, key: ByteBuffer, data: ByteBuffer?): Int {
        val keyVal = byteBufferToMdbVal(key)
        val dataVal = data?.let { byteBufferToMdbVal(it) }
        return lib.mdb_del(txn, dbi, keyVal, dataVal)
    }

    // Cursor operations
    fun mdb_cursor_open(txn: Pointer, dbi: Int): Pointer {
        val cursorRef = PointerByReference()
        check(lib.mdb_cursor_open(txn, dbi, cursorRef))
        return cursorRef.value ?: throw LmdbException("Failed to open cursor")
    }

    fun mdb_cursor_close(cursor: Pointer) {
        lib.mdb_cursor_close(cursor)
    }

    fun mdb_cursor_renew(txn: Pointer, cursor: Pointer): Int {
        return lib.mdb_cursor_renew(txn, cursor)
    }

    fun mdb_cursor_get(cursor: Pointer, key: ByteBuffer, data: ByteBuffer, op: Int): Int {
        val keyVal = byteBufferToMdbVal(key)
        val dataVal = byteBufferToMdbVal(data)
        
        val rc = lib.mdb_cursor_get(cursor, keyVal, dataVal, op)
        
        if (rc == 0) {
            // Update ByteBuffer positions based on returned data
            if (keyVal.mv_data != null) {
                key.position(0)
                key.limit(keyVal.mv_size.toInt())
                key.put(keyVal.mv_data!!.getByteArray(0, keyVal.mv_size.toInt()))
                key.position(0)
            }
            
            if (dataVal.mv_data != null) {
                data.position(0)
                data.limit(dataVal.mv_size.toInt())
                data.put(dataVal.mv_data!!.getByteArray(0, dataVal.mv_size.toInt()))
                data.position(0)
            }
        }
        
        return rc
    }

    fun mdb_cursor_put(cursor: Pointer, key: ByteBuffer, data: ByteBuffer, flags: Int): Int {
        val keyVal = byteBufferToMdbVal(key)
        val dataVal = byteBufferToMdbVal(data)
        return lib.mdb_cursor_put(cursor, keyVal, dataVal, flags)
    }

    fun mdb_cursor_del(cursor: Pointer, flags: Int): Int {
        return lib.mdb_cursor_del(cursor, flags)
    }

    fun mdb_cursor_count(cursor: Pointer): Long {
        val count = LongByReference()
        val rc = lib.mdb_cursor_count(cursor, count)
        if (rc != 0) return -rc.toLong() // Return negative on error for compatibility
        return count.value
    }

    // Comparison functions
    fun mdb_cmp(txn: Pointer, dbi: Int, a: ByteBuffer, b: ByteBuffer): Int {
        val aVal = byteBufferToMdbVal(a)
        val bVal = byteBufferToMdbVal(b)
        return lib.mdb_cmp(txn, dbi, aVal, bVal)
    }

    fun mdb_dcmp(txn: Pointer, dbi: Int, a: ByteBuffer, b: ByteBuffer): Int {
        val aVal = byteBufferToMdbVal(a)
        val bVal = byteBufferToMdbVal(b)
        return lib.mdb_dcmp(txn, dbi, aVal, bVal)
    }

    fun mdb_set_compare(txn: Pointer, dbi: Int, callback: Any?): Int {
        val cmpFunc = callback as? LmdbLibrary.MDB_cmp_func
        return lib.mdb_set_compare(txn, dbi, cmpFunc)
    }

    fun mdb_set_dupsort(txn: Pointer, dbi: Int, callback: Any?): Int {
        val cmpFunc = callback as? LmdbLibrary.MDB_cmp_func
        return lib.mdb_set_dupsort(txn, dbi, cmpFunc)
    }

    // Utility functions
    fun mdb_strerror(rc: Int): String {
        return lib.mdb_strerror(rc)
    }

    fun mdb_version(): String {
        val major = IntByReference()
        val minor = IntByReference()
        val patch = IntByReference()
        lib.mdb_version(major, minor, patch)
        return "${major.value}.${minor.value}.${patch.value}"
    }

    // Helper function to convert ByteBuffer to MDB_val
    private fun byteBufferToMdbVal(buffer: ByteBuffer): MDB_val {
        val val_ = MDB_val()
        
        if (buffer.isDirect) {
            // Direct ByteBuffer - get native pointer
            val address = Native.getDirectBufferPointer(buffer)
            val offset = buffer.position()
            val length = buffer.remaining()
            
            if (offset > 0) {
                // If there's an offset, we need to share the pointer at the correct position
                val pointer = address.share(offset.toLong())
                val_.mv_data = pointer
            } else {
                // No offset, use the address directly
                val_.mv_data = address
            }
            val_.mv_size = length.toLong()
        } else {
            // Non-direct ByteBuffer - copy to native memory
            val array = ByteArray(buffer.remaining())
            val savedPosition = buffer.position()
            buffer.get(array)
            buffer.position(savedPosition) // Restore original position
            
            val memory = Memory(array.size.toLong())
            memory.write(0, array, 0, array.size)
            
            val_.mv_data = memory
            val_.mv_size = array.size.toLong()
        }
        
        return val_
    }

    /**
     * Helper class to manage native library loading
     */
    private object NativeLibraryLoader {
        private const val DEBUG = false
        private const val LMDB_NATIVE_LIB_PROP = "lmdb.native.lib"
        private const val LMDB_LIB_NAME = "lmdb"

        fun loadLibrary(): LmdbLibrary {
            // Check for explicitly specified library path
            val explicitPath = System.getProperty(LMDB_NATIVE_LIB_PROP)
            if (explicitPath != null) {
                return loadExplicitLibrary(explicitPath)
            }

            // Try to load from resources first
            val extractedLib = extractLibraryFromResources()
            if (extractedLib != null) {
                if (DEBUG) println("[LMDB-JNA] Extracted library to: ${extractedLib.absolutePath}")
                // Load using the full path to the extracted library
                try {
                    return Native.load(extractedLib.absolutePath, LmdbLibrary::class.java)
                } catch (e: UnsatisfiedLinkError) {
                    if (DEBUG) {
                        println("[LMDB-JNA] Failed to load from extracted path, trying with library name")
                        e.printStackTrace()
                    }
                    // Fall back to loading by name
                    System.setProperty("jna.library.path", extractedLib.parent)
                    if (DEBUG) println("[LMDB-JNA] Set JNA library path to: ${extractedLib.parent}")
                }
            }

            // Try to load the library through JNA
            try {
                return Native.load(LMDB_LIB_NAME, LmdbLibrary::class.java)
            } catch (e: UnsatisfiedLinkError) {
                // Log the error details
                if (DEBUG) {
                    println("[LMDB-JNA] Failed to load library: ${e.message}")
                    e.printStackTrace()
                }
                
                if (extractedLib == null) {
                    // If extraction failed, provide helpful error message
                    val platform = detectPlatform()
                    val libName = getPlatformLibraryName()
                    throw UnsatisfiedLinkError(
                        "Failed to load LMDB library. Tried to find $libName for platform $platform. " +
                        "You can specify a custom library path using -D$LMDB_NATIVE_LIB_PROP=/path/to/library " +
                        "or ensure the library is in your system library path. " +
                        "Original error: ${e.message}"
                    )
                } else {
                    // Extraction succeeded but loading still failed
                    throw UnsatisfiedLinkError(
                        "Failed to load LMDB library from extracted file: ${extractedLib.absolutePath}. " +
                        "Original error: ${e.message}"
                    )
                }
            }
        }

        private fun loadExplicitLibrary(path: String): LmdbLibrary {
            if (DEBUG) println("[LMDB-JNA] Loading explicit library: $path")
            val file = File(path)
            if (file.exists()) {
                System.setProperty("jna.library.path", file.parent)
                return Native.load(file.nameWithoutExtension, LmdbLibrary::class.java)
            } else {
                throw UnsatisfiedLinkError("Specified library not found: $path")
            }
        }

        private fun extractLibraryFromResources(): File? {
            val platform = detectPlatform()
            if (DEBUG) println("[LMDB-JNA] Detected platform: $platform")

            val libFileName = getPlatformLibraryName()
            val jnaPlatform = getJnaPlatformName()
            
            // Map our platform names to Android's jniLibs architecture names
            val androidArch = when (platform) {
                "androidNativeArm64" -> "arm64-v8a"
                "androidNativeArm32" -> "armeabi-v7a"
                "androidNativeX64" -> "x86_64"
                "androidNativeX86" -> "x86"
                else -> null
            }
            
            // Try multiple resource paths for better compatibility
            val resourcePaths = mutableListOf(
                "native-libs/$platform/$libFileName",
                "jniLibs/$platform/$libFileName",
                "lib/$platform/$libFileName",
                "$platform/$libFileName",
                // JNA's expected format
                "$jnaPlatform/$libFileName",
                "native/$jnaPlatform/$libFileName"
            )
            
            // Add Android-specific paths if applicable
            if (androidArch != null) {
                resourcePaths.add("jniLibs/$androidArch/$libFileName")
                resourcePaths.add("lib/$androidArch/$libFileName")
            }

            // Try multiple classloaders for better dependency loading
            val classLoaders = listOfNotNull(
                Thread.currentThread().contextClassLoader,
                LmdbJna::class.java.classLoader,
                ClassLoader.getSystemClassLoader()
            ).distinct()

            var inputStream: java.io.InputStream? = null

            // Try each combination of classloader and resource path
            for (loader in classLoaders) {
                for (path in resourcePaths) {
                    if (DEBUG) println("[LMDB-JNA] Trying resource: $path with loader: $loader")
                    inputStream = loader.getResourceAsStream(path)
                    if (inputStream != null) {
                        if (DEBUG) println("[LMDB-JNA] Found resource at: $path")
                        break
                    }
                }
                if (inputStream != null) break
            }

            if (inputStream == null) {
                if (DEBUG) {
                    println("[LMDB-JNA] Resource not found in any of the paths: $resourcePaths")
                    println("[LMDB-JNA] Tried classloaders: $classLoaders")
                    
                    // Try to list available resources for debugging
                    for (loader in classLoaders) {
                        try {
                            val urls = if (loader is java.net.URLClassLoader) {
                                loader.urLs.contentToString()
                            } else {
                                "Not a URLClassLoader"
                            }
                            println("[LMDB-JNA] ClassLoader URLs: $urls")
                        } catch (e: Exception) {
                            println("[LMDB-JNA] Failed to inspect classloader: ${e.message}")
                        }
                    }
                }
                return null
            }

            return try {
                // Create temp directory for extracted libraries
                val tempDir = File(System.getProperty("java.io.tmpdir"), "lmdb-jna-native")
                tempDir.mkdirs()
                
                // Use a more stable filename to avoid re-extraction
                val tempFile = File(tempDir, "$LMDB_LIB_NAME-$platform-${libFileName}")
                
                // Only extract if file doesn't exist or is older than the resource
                if (!tempFile.exists() || tempFile.length() == 0L) {
                    inputStream.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (DEBUG) println("[LMDB-JNA] Extracted library to: ${tempFile.absolutePath}")
                } else {
                    if (DEBUG) println("[LMDB-JNA] Using existing extracted library: ${tempFile.absolutePath}")
                }
                
                // Make sure the file is executable on Unix-like systems
                try {
                    if (!System.getProperty("os.name").lowercase().contains("win")) {
                        tempFile.setExecutable(true)
                    }
                } catch (_: Exception) {
                    // Ignore permission errors
                }
                
                tempFile
            } catch (e: Exception) {
                if (DEBUG) println("[LMDB-JNA] Failed to extract library: ${e.message}")
                null
            } finally {
                inputStream.close()
            }
        }

        private fun detectPlatform(): String {
            val os = System.getProperty("os.name").lowercase()
            val arch = System.getProperty("os.arch").lowercase()
            
            if (DEBUG) {
                println("[LMDB-JNA] OS: $os, Arch: $arch")
                println("[LMDB-JNA] java.vendor: ${System.getProperty("java.vendor")}")
                println("[LMDB-JNA] java.vm.vendor: ${System.getProperty("java.vm.vendor")}")
            }

            return when {
                os.contains("win") -> "mingwX64"
                os.contains("mac") -> when {
                    arch.contains("aarch64") || arch.contains("arm64") -> "macosArm64"
                    else -> "macosX64"
                }
                os.contains("linux") -> when {
                    arch.contains("aarch64") || arch.contains("arm64") -> "linuxArm64"
                    else -> "linuxX64"
                }
                os.contains("android") || isAndroid() -> when {
                    arch.contains("arm64") -> "androidNativeArm64"
                    arch.contains("arm") -> "androidNativeArm32"
                    arch.contains("x86_64") -> "androidNativeX64"
                    else -> "androidNativeX86"
                }
                else -> throw UnsupportedOperationException("Unsupported platform: $os/$arch")
            }
        }

        /**
         * Get JNA's expected platform name format (e.g., "darwin-aarch64")
         */
        private fun getJnaPlatformName(): String {
            val os = System.getProperty("os.name").lowercase()
            val arch = System.getProperty("os.arch").lowercase()
            
            val osName = when {
                os.contains("win") -> "win32"
                os.contains("mac") -> "darwin"
                os.contains("linux") -> "linux"
                else -> os
            }
            
            val archName = when {
                arch.contains("aarch64") || arch.contains("arm64") -> "aarch64"
                arch.contains("x86_64") || arch.contains("amd64") -> "x86-64"
                arch.contains("x86") || arch.contains("i386") -> "x86"
                else -> arch
            }
            
            return "$osName-$archName"
        }

        private fun isAndroid(): Boolean {
            return System.getProperty("java.vendor")?.contains("Android") == true ||
                   System.getProperty("java.vm.vendor")?.contains("Android") == true
        }

        private fun getPlatformLibraryName(): String {
            val baseName = LMDB_LIB_NAME
            val os = System.getProperty("os.name").lowercase()
            return when {
                os.contains("win") -> "$baseName.dll"
                os.contains("mac") -> "lib$baseName.dylib"
                else -> "lib$baseName.so"
            }
        }
    }

    /**
     * JNA-compatible structure for environment info
     */
    class EnvInfo {
        var mapaddr: Long = 0
        var mapsize: Long = 0
        var lastPgno: Long = 0
        var lastTxnid: Long = 0
        var maxReaders: Int = 0
        var numReaders: Int = 0
    }

    /**
     * JNA-compatible structure for statistics
     */
    class Stat {
        var pageSize: Int = 0
        var depth: Int = 0
        var branchPages: Long = 0
        var leafPages: Long = 0
        var overflowPages: Long = 0
        var entries: Long = 0
    }
}