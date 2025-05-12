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
import java.util.*

internal class Library {
    companion object {
        const val LMDB_NATIVE_LIB_PROP = "lmdb.native.lib"
        private const val LMDB_DEBUG_PROP = "lmdb.debug"
        private const val LIB_NAME = "lmdb"

        val LMDB: Lmdb
        val RUNTIME: Runtime
        val MEMORY: MemoryManager

        val SHOULD_USE_LIB = Objects.nonNull(
            System.getProperty(LMDB_NATIVE_LIB_PROP)
        )
        val DEBUG = Objects.nonNull(System.getProperty(LMDB_DEBUG_PROP))

        init {
            val libToLoad: String
            val isWindows = Platform.getNativePlatform().os == Platform.OS.WINDOWS
            
            try {
                if (SHOULD_USE_LIB) {
                    // Use explicitly specified library path
                    libToLoad = System.getProperty(LMDB_NATIVE_LIB_PROP) ?: ""
                    if (DEBUG) println("[LMDB] Using specified library: $libToLoad")
                } else {
                    // Auto-detect library name
                    libToLoad = Platform.getNativePlatform().mapLibraryName(LIB_NAME)
                    if (DEBUG) println("[LMDB] Auto-detected library name: $libToLoad")
                }
                
                // Create a loader with diagnostic capability
                val loader = LibraryLoader.create(Lmdb::class.java)
                    .searchDefault()
                
                // On Windows, try multiple naming patterns
                if (isWindows) {
                    try {
                        if (DEBUG) println("[LMDB] Attempting to load: $libToLoad")
                        if (SHOULD_USE_LIB) {
                            // Try loading from full path
                            val file = java.io.File(libToLoad)
                            if (file.exists()) {
                                if (DEBUG) println("[LMDB] File exists at specified path")
                                // Load file from absolute path
                                System.loadLibrary(file.absolutePath)
                                // Now use LibraryLoader with default search to find the loaded library
                                LMDB = loader.load(LIB_NAME)
                            } else {
                                throw UnsatisfiedLinkError("Library file not found at: ${file.absolutePath}")
                            }
                        } else {
                            LMDB = loader.load(libToLoad)
                        }
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
                    LMDB = loader.load(libToLoad)
                }
                
                RUNTIME = Runtime.getRuntime(LMDB)
                MEMORY = RUNTIME.memoryManager
                
                if (DEBUG) println("[LMDB] Successfully loaded library")
            } catch (e: Exception) {
                val errorMsg = buildString {
                    append("Failed to load LMDB native library. ")
                    append("Please ensure the library is in one of the following locations:\n")
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