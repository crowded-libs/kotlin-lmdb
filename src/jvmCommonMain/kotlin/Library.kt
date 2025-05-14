package lmdb

import jnr.ffi.LibraryLoader
import jnr.ffi.Platform
import jnr.ffi.Pointer
import jnr.ffi.Runtime
import jnr.ffi.Struct
import jnr.ffi.annotations.Delegate
import jnr.ffi.annotations.In
import jnr.ffi.annotations.Out
import jnr.ffi.byref.IntByReference
import jnr.ffi.byref.NativeLongByReference
import jnr.ffi.byref.PointerByReference
import jnr.ffi.provider.MemoryManager
import jnr.ffi.types.size_t
import java.io.File
import java.util.*

internal class Library {
    companion object {
        const val LMDB_NATIVE_LIB_PROP = "lmdb.native.lib"
        private const val LMDB_DEBUG_PROP = "lmdb.debug"
        private const val LIB_NAME = "lmdb"

        val SHOULD_USE_LIB = Objects.nonNull(
            System.getProperty(LMDB_NATIVE_LIB_PROP)
        )
        val DEBUG = Objects.nonNull(System.getProperty(LMDB_DEBUG_PROP))

        /**
         * Helper object to load native libraries from resources.
         * This ensures that the correct library for the current platform is loaded.
         */
        private object NativeLibraryLoader {
            // Always enable debug logging for now to help diagnose issues
            private val DEBUG = true // System.getProperty("lmdb.debug") != null

            // Track if we've already loaded the library
            private var libraryLoaded = false

            // Store the path to the extracted library for JNR-FFI to use
            private var extractedLibraryPath: String? = null

            /**
             * Helper method to detect if we're running on Android
             */
            private fun isAndroid(): Boolean {
                return System.getProperty("java.vm.vendor")?.contains("Android") == true ||
                       System.getProperty("os.name")?.contains("Android") == true
            }

            /**
             * Attempts to load the LMDB native library from resources.
             * 
             * @return true if the library was successfully loaded from resources, false otherwise
             */
            fun loadFromResources(): Boolean {
                if (libraryLoaded) {
                    if (DEBUG) println("[LMDB] Library already loaded")
                    return true
                }

                println("[LMDB] Attempting to load native library from resources")

                val tempFile = try {
                    extractNativeLibrary()
                } catch (e: Exception) {
                    println("[LMDB] Failed to extract native library: ${e.message}")
                    e.printStackTrace()
                    return false
                }

                if (tempFile == null || !tempFile.exists()) {
                    println("[LMDB] No native library found in resources")
                    return false
                }

                // Create a symbolic link with the expected name in the temp directory
                val platform = Platform.getNativePlatform()

                // Check if we're running on Android
                val expectedName = if (isAndroid()) {
                    "liblmdb.so" // Android always uses .so files
                } else {
                    when (platform.os) {
                        Platform.OS.WINDOWS -> "lmdb.dll"
                        Platform.OS.DARWIN -> "liblmdb.dylib"
                        Platform.OS.LINUX -> "liblmdb.so"
                        else -> return false // Unsupported platform
                    }
                }

                val tempDir = File(System.getProperty("java.io.tmpdir")?:"")
                val linkFile = File(tempDir, expectedName)

                // Delete the link file if it already exists
                if (linkFile.exists()) {
                    println("[LMDB] Deleting existing link file: ${linkFile.absolutePath}")
                    linkFile.delete()
                }

                // Copy the library to the expected location
                println("[LMDB] Copying library to: ${linkFile.absolutePath}")
                try {
                    tempFile.copyTo(linkFile, overwrite = true)
                } catch (e: Exception) {
                    println("[LMDB] Failed to copy library: ${e.message}")
                    e.printStackTrace()
                    return false
                }

                println("[LMDB] Loading extracted library from: ${linkFile.absolutePath}")
                try {
                    System.load(linkFile.absolutePath)
                    extractedLibraryPath = linkFile.absolutePath
                    libraryLoaded = true
                    println("[LMDB] Successfully loaded library from resources")
                    return true
                } catch (e: Exception) {
                    println("[LMDB] Failed to load extracted library: ${e.message}")
                    e.printStackTrace()
                    return false
                }
            }

            /**
             * Returns the path to the extracted library, or null if the library hasn't been extracted.
             */
            fun getExtractedLibraryPath(): String? {
                return extractedLibraryPath
            }

            /**
             * Extracts the appropriate native library from the JAR resources based on the current platform.
             * 
             * @return A temporary file containing the extracted library, or null if extraction failed
             */
            private fun extractNativeLibrary(): File? {
                val platform = Platform.getNativePlatform()
                val os = platform.os
                val cpu = platform.cpu

                println("[LMDB] Platform: $os, CPU: $cpu")

                // Determine the platform directory and library name
                val (platformDir, libraryName) = if (isAndroid()) {
                    // On Android, determine the appropriate ABI based on CPU architecture
                    when (cpu) {
                        Platform.CPU.ARM -> "androidNativeArm32" to "liblmdb.so"
                        Platform.CPU.AARCH64 -> "androidNativeArm64" to "liblmdb.so"
                        Platform.CPU.I386 -> "androidNativeX86" to "liblmdb.so"
                        Platform.CPU.X86_64 -> "androidNativeX64" to "liblmdb.so"
                        else -> {
                            println("[LMDB] Unsupported Android CPU architecture: $cpu")
                            return null // Unsupported architecture
                        }
                    }
                } else {
                    // For non-Android platforms
                    when (os) {
                        Platform.OS.WINDOWS -> "mingwX64" to "lmdb.dll"
                        Platform.OS.DARWIN -> {
                            if (cpu == Platform.CPU.AARCH64) "macosArm64" to "liblmdb.dylib"
                            else "macosX64" to "liblmdb.dylib"
                        }
                        Platform.OS.LINUX -> {
                            if (cpu == Platform.CPU.AARCH64) "linuxArm64" to "liblmdb.so"
                            else "linuxX64" to "liblmdb.so"
                        }
                        else -> {
                            println("[LMDB] Unsupported platform: $os")
                            return null // Unsupported platform
                        }
                    }
                }

                // Define all possible resource paths to search
                val resourcePaths = listOf(
                    "native-libs/$platformDir/$libraryName",                // Main path
                    "$platformDir/$libraryName",                            // Direct platform directory
                    "libs/$platformDir/$libraryName",                       // Original libs directory
                    "native-libs/libs/$platformDir/$libraryName",           // Nested libs directory
                    "native-libs/$platformDir/lib/$libraryName",            // lib subdirectory
                    "$platformDir/lib/$libraryName",                        // Direct platform with lib subdirectory
                    "lib/$platformDir/$libraryName",                        // lib parent directory
                    "native-libs/lib/$platformDir/$libraryName"             // lib in native-libs
                )

                println("[LMDB] Searching for library in multiple paths")

                // Try each resource path
                val classLoader = NativeLibraryLoader::class.java.classLoader!!

                // First try to find the resource in any of the paths
                for (resourcePath in resourcePaths) {
                    println("[LMDB] Looking for resource: $resourcePath")
                    val resourceStream = classLoader.getResourceAsStream(resourcePath)

                    if (resourceStream != null) {
                        println("[LMDB] Found resource at: $resourcePath")

                        // Create a temporary file to store the library
                        val tempFile = File.createTempFile("liblmdb", libraryName.substringAfterLast('.'))
                        tempFile.deleteOnExit()

                        println("[LMDB] Extracting to: ${tempFile.absolutePath}")

                        // Copy the library to the temporary file
                        resourceStream.use { input ->
                            tempFile.outputStream().use { output ->
                                val bytesCopied = input.copyTo(output)
                                println("[LMDB] Copied $bytesCopied bytes to ${tempFile.absolutePath}")
                            }
                        }

                        // Verify the file exists and has content
                        if (!tempFile.exists()) {
                            println("[LMDB] Extracted file does not exist: ${tempFile.absolutePath}")
                            continue
                        }

                        if (tempFile.length() == 0L) {
                            println("[LMDB] Extracted file is empty: ${tempFile.absolutePath}")
                            continue
                        }

                        println("[LMDB] Successfully extracted library to: ${tempFile.absolutePath} (${tempFile.length()} bytes)")
                        return tempFile
                    }
                }

                // If we get here, we couldn't find the resource in any of the paths
                println("[LMDB] Resource not found in any of the expected paths")

                // List all available resources to help diagnose issues
                try {
                    println("[LMDB] Listing resources in classpath:")
                    val resources = classLoader.getResources("")
                    while (resources.hasMoreElements()) {
                        val url = resources.nextElement()
                        println("[LMDB] Resource: $url")

                        // If it's a file URL, try to list its contents
                        if (url.protocol == "file") {
                            val file = File(url.toURI())
                            if (file.isDirectory) {
                                println("[LMDB] Directory contents of $url:")
                                file.listFiles()?.forEach { println("[LMDB]   - $it") }
                            }
                        }
                    }

                    // Try to list the contents of the native-libs directory
                    val nativeLibsUrl = classLoader.getResource("native-libs")
                    println("[LMDB] native-libs URL: $nativeLibsUrl")

                    if (nativeLibsUrl != null && nativeLibsUrl.protocol == "file") {
                        val nativeLibsDir = File(nativeLibsUrl.toURI())
                        if (nativeLibsDir.isDirectory) {
                            println("[LMDB] native-libs directory contents:")
                            nativeLibsDir.listFiles()?.forEach { println("[LMDB]   - $it") }

                            // Try to list the platform directory
                            val platformDirFile = File(nativeLibsDir, platformDir)
                            if (platformDirFile.isDirectory) {
                                println("[LMDB] $platformDir directory contents:")
                                platformDirFile.listFiles()?.forEach { println("[LMDB]   - $it") }
                            } else {
                                println("[LMDB] $platformDir is not a directory or does not exist")
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("[LMDB] Error listing resources: ${e.message}")
                    e.printStackTrace()
                }

                // Try a different approach - look for the file directly in the filesystem
                for (resourcePath in resourcePaths) {
                    val resourceFile = File("src/jvmMain/resources/$resourcePath")
                    println("[LMDB] Checking if file exists at: ${resourceFile.absolutePath}")
                    if (resourceFile.exists()) {
                        println("[LMDB] Found file at: ${resourceFile.absolutePath}")
                        return resourceFile
                    }
                }

                return null
            }
        }

        // Use a helper class to initialize the library
        private class LibraryInitializer {
            fun initialize(): Triple<Lmdb, Runtime, MemoryManager> {
                try {
                    // First check if we should use a specific library path
                    if (SHOULD_USE_LIB) {
                        if (DEBUG) println("[LMDB] Checking for explicitly specified library")

                        // Use explicitly specified library path
                        val libToLoad = System.getProperty(LMDB_NATIVE_LIB_PROP) ?: ""
                        if (DEBUG) println("[LMDB] Using specified library: $libToLoad")

                        try {
                            // Try loading from full path
                            val file = File(libToLoad)
                            if (file.exists()) {
                                if (DEBUG) println("[LMDB] File exists at specified path")
                                // Load file from absolute path
                                System.loadLibrary(file.absolutePath)

                                // Now use LibraryLoader with default search to find the loaded library
                                val loader = LibraryLoader.create(Lmdb::class.java).searchDefault()
                                val lmdb = loader.load(LIB_NAME)
                                val runtime = Runtime.getRuntime(lmdb)
                                val memory = runtime.memoryManager

                                if (DEBUG) println("[LMDB] Successfully loaded specified library")
                                return Triple(lmdb, runtime, memory)
                            } else {
                                if (DEBUG) println("[LMDB] Specified library file not found: ${file.absolutePath}")
                                // Continue to other loading methods
                            }
                        } catch (e: Exception) {
                            if (DEBUG) {
                                println("[LMDB] Loading specified library failed: ${e.message}")
                                println("[LMDB] Will try other methods")
                            }
                            // Continue to other loading methods
                        }
                    }

                    // Try to load the library from resources
                    val loadedFromResources = NativeLibraryLoader.loadFromResources()

                    if (loadedFromResources) {
                        // If we successfully loaded the library from resources, create the JNR-FFI wrapper
                        // Get the path to the extracted library
                        val extractedLibPath = NativeLibraryLoader.getExtractedLibraryPath()
                        if (DEBUG) println("[LMDB] Using extracted library path: $extractedLibPath")

                        // Create a loader that only looks at the extracted library path
                        val loader = LibraryLoader.create(Lmdb::class.java)

                        // Add the directory containing the extracted library to the search path
                        if (extractedLibPath != null) {
                            val libraryDir = File(extractedLibPath).parent
                            if (DEBUG) println("[LMDB] Adding library directory to search path: $libraryDir")

                            // Set the java.library.path system property to include the directory
                            val oldLibraryPath = System.getProperty("java.library.path", "") ?: ""
                            val newLibraryPath = if (oldLibraryPath.isEmpty()) libraryDir else "$oldLibraryPath${File.pathSeparator}$libraryDir"
                            System.setProperty("java.library.path", newLibraryPath)

                            // Force the ClassLoader to reload the library path
                            try {
                                val fieldSysPath = ClassLoader::class.java.getDeclaredField("sys_paths")
                                fieldSysPath.isAccessible = true
                                fieldSysPath.set(null, null)
                                if (DEBUG) println("[LMDB] Reset ClassLoader sys_paths")
                            } catch (e: Exception) {
                                if (DEBUG) println("[LMDB] Failed to reset ClassLoader sys_paths: ${e.message}")
                            }
                        }

                        // Use the default search paths, which now include our directory
                        loader.searchDefault()

                        val lmdb = loader.load(LIB_NAME)
                        val runtime = Runtime.getRuntime(lmdb)
                        val memory = runtime.memoryManager

                        if (DEBUG) println("[LMDB] Successfully initialized library from resources")
                        return Triple(lmdb, runtime, memory)
                    } else {
                        // Fall back to the original loading mechanism
                        if (DEBUG) println("[LMDB] Falling back to system paths")
                        val isWindows = Platform.getNativePlatform().os == Platform.OS.WINDOWS

                        // Auto-detect library name
                        val libToLoad: String = Platform.getNativePlatform().mapLibraryName(LIB_NAME)
                        if (DEBUG) println("[LMDB] Auto-detected library name: $libToLoad")

                        // Create a loader with diagnostic capability
                        val loader = LibraryLoader.create(Lmdb::class.java)
                            .searchDefault()

                        // On Windows, try multiple naming patterns
                        val lmdb = if (isWindows) {
                            try {
                                if (DEBUG) println("[LMDB] Attempting to load: $libToLoad")
                                loader.load(libToLoad)
                            } catch (e: Exception) {
                                if (DEBUG) {
                                    println("[LMDB] Loading failed: ${e.message}")
                                    println("[LMDB] Search paths: ${System.getProperty("java.library.path")}")
                                }
                                throw e
                            }
                        } else {
                            // Regular loading for non-Windows platforms
                            if (DEBUG) println("[LMDB] Loading library: $libToLoad")
                            loader.load(libToLoad)
                        }

                        val runtime = Runtime.getRuntime(lmdb)
                        val memory = runtime.memoryManager

                        if (DEBUG) println("[LMDB] Successfully loaded library from system paths")
                        return Triple(lmdb, runtime, memory)
                    }
                } catch (e: Exception) {
                    val errorMsg = buildString {
                        append("Failed to load LMDB native library. ")
                        append("Please ensure the library is in one of the following locations:\n")
                        append("- Packaged with the application (automatic)\n")
                        append("- In the system library path\n")
                        append("- In the java.library.path (${System.getProperty("java.library.path")})\n")
                        append("- Specified via -Dlmdb.native.lib=/path/to/library\n\n")
                        append("Operating System: ${System.getProperty("os.name")}\n")
                        append("Architecture: ${System.getProperty("os.arch")}\n")
                        append("Error: ${e.message}")
                    }
                    throw UnsatisfiedLinkError(errorMsg)
                }
            }
        }

        // Initialize the library using the helper class
        private val libraryInitResult = LibraryInitializer().initialize()
        val LMDB: Lmdb = libraryInitResult.first
        val RUNTIME: Runtime = libraryInitResult.second
        val MEMORY: MemoryManager = libraryInitResult.third
    }



    class MDB_envinfo internal constructor(runtime: Runtime?) : Struct(runtime) {
        val f0_me_mapaddr: Pointer
        val f1_me_mapsize: size_t
        val f2_me_last_pgno: size_t
        val f3_me_last_txnid: size_t
        val f4_me_maxreaders: u_int32_t
        val f5_me_numreaders: u_int32_t

        init {
            f0_me_mapaddr = Pointer()
            f1_me_mapsize = size_t()
            f2_me_last_pgno = size_t()
            f3_me_last_txnid = size_t()
            f4_me_maxreaders = u_int32_t()
            f5_me_numreaders = u_int32_t()
        }
    }

    class MDB_stat internal constructor(runtime: Runtime?) : Struct(runtime) {
        val f0_ms_psize: u_int32_t
        val f1_ms_depth: u_int32_t
        val f2_ms_branch_pages: size_t
        val f3_ms_leaf_pages: size_t
        val f4_ms_overflow_pages: size_t
        val f5_ms_entries: size_t

        init {
            f0_ms_psize = u_int32_t()
            f1_ms_depth = u_int32_t()
            f2_ms_branch_pages = size_t()
            f3_ms_leaf_pages = size_t()
            f4_ms_overflow_pages = size_t()
            f5_ms_entries = size_t()
        }
    }

    interface ComparatorCallback {
        @Delegate
        fun compare(@In keyA: Pointer?, @In keyB: Pointer?): Int
    }

    interface Lmdb {
        fun mdb_cmp(@In txn: Pointer?, @In dbi: Pointer?, @In a: Pointer?, @In b: Pointer?): Int
        fun mdb_cursor_close(@In cursor: Pointer?)
        fun mdb_cursor_count(@In cursor: Pointer?, countp: NativeLongByReference?): Int
        fun _mdb_cursor_dbi(@In cursor: Pointer?): Pointer?
        fun mdb_cursor_del(@In cursor: Pointer?, flags: Int): Int
        fun mdb_cursor_get(@In cursor: Pointer?, k: Pointer?, @Out v: Pointer?, cursorOp: Int): Int
        fun mdb_cursor_open(@In txn: Pointer?, @In dbi: Pointer?, cursorPtr: PointerByReference?): Int
        fun mdb_cursor_put(@In cursor: Pointer?, @In key: Pointer?, @In data: Pointer?, flags: Int): Int
        fun mdb_cursor_renew(@In txn: Pointer?, @In cursor: Pointer?): Int
        fun _mdb_cursor_txn(@In cursor: Pointer?): Pointer?
        fun mdb_dbi_close(@In env: Pointer?, @In dbi: Pointer?)
        fun mdb_dbi_flags(@In txn: Pointer?, @In dbi: Pointer?, @Out flags: IntByReference?): Int
        fun mdb_dbi_open(@In txn: Pointer?, @In name: ByteArray?, flags: Int, @In dbiPtr: Pointer?): Int
        fun mdb_dcmp(@In txn: Pointer?, @In dbi: Pointer?, @In a: Pointer?, @In b: Pointer?): Int
        fun mdb_del(@In txn: Pointer?, @In dbi: Pointer?, @In key: Pointer?, @In data: Pointer?): Int
        fun mdb_drop(@In txn: Pointer?, @In dbi: Pointer?, del: Int): Int
        fun mdb_env_close(@In env: Pointer?)
        fun mdb_env_copy(@In env: Pointer?, @In path: String?): Int
        fun mdb_env_copy2(@In env: Pointer?, @In path: String?, flags: Int): Int
        fun _mdb_env_copyfd(@In env: Pointer?, @In fd: Pointer?): Int
        fun _mdb_env_copyfd2(@In env: Pointer?, @In fd: Pointer?, flags: Int): Int
        fun mdb_env_create(envPtr: PointerByReference?): Int
        fun _mdb_env_get_fd(@In env: Pointer?, @In fd: Pointer?): Int
        fun mdb_env_get_flags(@In env: Pointer?, @Out flags: IntByReference?): Int
        fun mdb_env_get_maxkeysize(@In env: Pointer?): Int
        fun mdb_env_get_maxreaders(@In env: Pointer?, @Out readers: IntByReference?): Int
        fun _mdb_env_get_path(@In env: Pointer?, path: String?): Int
        fun _mdb_env_get_userctx(@In env: Pointer?): Pointer?
        fun mdb_env_info(@In env: Pointer?, @Out info: MDB_envinfo?): Int
        fun mdb_env_open(@In env: Pointer?, @In path: String?, flags: Int, mode: Int): Int
        fun _mdb_env_set_assert(@In env: Pointer?, @In func: Pointer?): Int
        fun mdb_env_set_flags(@In env: Pointer?, flags: Int, onoff: Int): Int
        fun mdb_env_set_mapsize(@In env: Pointer?, @size_t size: Long): Int
        fun mdb_env_set_maxdbs(@In env: Pointer?, dbs: Int): Int
        fun mdb_env_set_maxreaders(@In env: Pointer?, readers: Int): Int
        fun _mdb_env_set_userctx(@In env: Pointer?, @In ctx: Pointer?): Int
        fun mdb_env_stat(@In env: Pointer?, @Out stat: MDB_stat?): Int
        fun mdb_env_sync(@In env: Pointer?, f: Int): Int
        fun mdb_get(@In txn: Pointer?, @In dbi: Pointer?, @In key: Pointer?, @Out data: Pointer?): Int
        fun mdb_put(@In txn: Pointer?, @In dbi: Pointer?, @In key: Pointer?, @In data: Pointer?, flags: Int): Int
        fun mdb_reader_check(@In env: Pointer?, @Out dead: IntByReference?): Int
        fun _mdb_reader_list(@In env: Pointer?, @In func: Pointer?, @In ctx: Pointer?): Int
        fun mdb_set_compare(@In txn: Pointer?, @In dbi: Pointer?, cb: ComparatorCallback?): Int
        fun mdb_set_dupsort(@In txn: Pointer?, @In dbi: Pointer?, cb: ComparatorCallback?): Int
        fun _mdb_set_relctx(@In txn: Pointer?, @In dbi: Pointer?, @In ctx: Pointer?): Int
        fun _mdb_set_relfunc(@In txn: Pointer?, @In dbi: Pointer?, @In rel: Pointer?): Int
        fun mdb_stat(@In txn: Pointer?, @In dbi: Pointer?, @Out stat: MDB_stat?): Int
        fun mdb_strerror(rc: Int): String?
        fun mdb_txn_abort(@In txn: Pointer?)
        fun mdb_txn_begin(@In env: Pointer?, @In parentTx: Pointer?, flags: Int, txPtr: Pointer?): Int
        fun mdb_txn_commit(@In txn: Pointer?): Int
        fun _mdb_txn_env(@In txn: Pointer?): Pointer?
        fun mdb_txn_id(@In txn: Pointer?): Long
        fun mdb_txn_renew(@In txn: Pointer?): Int
        fun mdb_txn_reset(@In txn: Pointer?)
        fun mdb_version(major: IntByReference?, minor: IntByReference?, patch: IntByReference?): Pointer?
    }
}
