package lmdb

import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.Structure.FieldOrder

/**
 * JNA structure representing MDB_val
 * 
 * Generic structure used for passing keys and data in and out of the database.
 */
@FieldOrder("mv_size", "mv_data")
open class MDB_val : Structure {
    @JvmField var mv_size: Long = 0  // size_t
    @JvmField var mv_data: Pointer? = null

    constructor() : super()
    
    constructor(size: Long, data: Pointer?) : super() {
        this.mv_size = size
        this.mv_data = data
    }
    
    constructor(p: Pointer?) : super(p)
    
    override fun getFieldOrder(): List<String> {
        return listOf("mv_size", "mv_data")
    }
    
    open class ByReference : MDB_val(), Structure.ByReference
    open class ByValue : MDB_val(), Structure.ByValue
}

/**
 * JNA structure representing MDB_stat
 * 
 * Statistics for a database in the environment
 */
@FieldOrder("ms_psize", "ms_depth", "ms_branch_pages", "ms_leaf_pages", "ms_overflow_pages", "ms_entries")
open class MDB_stat : Structure {
    @JvmField var ms_psize: Int = 0           // unsigned int - Size of a database page
    @JvmField var ms_depth: Int = 0           // unsigned int - Depth (height) of the B-tree
    @JvmField var ms_branch_pages: Long = 0   // size_t - Number of internal (non-leaf) pages
    @JvmField var ms_leaf_pages: Long = 0     // size_t - Number of leaf pages
    @JvmField var ms_overflow_pages: Long = 0 // size_t - Number of overflow pages
    @JvmField var ms_entries: Long = 0        // size_t - Number of data items
    
    constructor() : super()
    constructor(p: Pointer?) : super(p)
    
    override fun getFieldOrder(): List<String> {
        return listOf("ms_psize", "ms_depth", "ms_branch_pages", "ms_leaf_pages", "ms_overflow_pages", "ms_entries")
    }
    
    open class ByReference : MDB_stat(), Structure.ByReference
    open class ByValue : MDB_stat(), Structure.ByValue
}

/**
 * JNA structure representing MDB_envinfo
 * 
 * Information about the environment
 */
@FieldOrder("me_mapaddr", "me_mapsize", "me_last_pgno", "me_last_txnid", "me_maxreaders", "me_numreaders")
open class MDB_envinfo : Structure {
    @JvmField var me_mapaddr: Pointer? = null  // void* - Address of map, if fixed
    @JvmField var me_mapsize: Long = 0         // size_t - Size of the data memory map
    @JvmField var me_last_pgno: Long = 0       // size_t - ID of the last used page
    @JvmField var me_last_txnid: Long = 0      // size_t - ID of the last committed transaction
    @JvmField var me_maxreaders: Int = 0       // unsigned int - max reader slots in the environment
    @JvmField var me_numreaders: Int = 0       // unsigned int - max reader slots used in the environment
    
    constructor() : super()
    constructor(p: Pointer?) : super(p)
    
    override fun getFieldOrder(): List<String> {
        return listOf("me_mapaddr", "me_mapsize", "me_last_pgno", "me_last_txnid", "me_maxreaders", "me_numreaders")
    }
    
    open class ByReference : MDB_envinfo(), Structure.ByReference
    open class ByValue : MDB_envinfo(), Structure.ByValue
}

// Error codes
object LmdbErrorCodes {
    const val MDB_SUCCESS = 0
    const val MDB_KEYEXIST = -30799
    const val MDB_NOTFOUND = -30798
    const val MDB_PAGE_NOTFOUND = -30797
    const val MDB_CORRUPTED = -30796
    const val MDB_PANIC = -30795
    const val MDB_VERSION_MISMATCH = -30794
    const val MDB_INVALID = -30793
    const val MDB_MAP_FULL = -30792
    const val MDB_DBS_FULL = -30791
    const val MDB_READERS_FULL = -30790
    const val MDB_TLS_FULL = -30789
    const val MDB_TXN_FULL = -30788
    const val MDB_CURSOR_FULL = -30787
    const val MDB_PAGE_FULL = -30786
    const val MDB_MAP_RESIZED = -30785
    const val MDB_INCOMPATIBLE = -30784
    const val MDB_BAD_RSLOT = -30783
    const val MDB_BAD_TXN = -30782
    const val MDB_BAD_VALSIZE = -30781
    const val MDB_BAD_DBI = -30780
}

// Environment flags
object LmdbEnvFlags {
    const val MDB_FIXEDMAP = 0x01
    const val MDB_NOSUBDIR = 0x4000
    const val MDB_NOSYNC = 0x10000
    const val MDB_RDONLY = 0x20000
    const val MDB_NOMETASYNC = 0x40000
    const val MDB_WRITEMAP = 0x80000
    const val MDB_MAPASYNC = 0x100000
    const val MDB_NOTLS = 0x200000
    const val MDB_NOLOCK = 0x400000
    const val MDB_NORDAHEAD = 0x800000
    const val MDB_NOMEMINIT = 0x1000000
}

// Database flags
object LmdbDbiFlags {
    const val MDB_REVERSEKEY = 0x02
    const val MDB_DUPSORT = 0x04
    const val MDB_INTEGERKEY = 0x08
    const val MDB_DUPFIXED = 0x10
    const val MDB_INTEGERDUP = 0x20
    const val MDB_REVERSEDUP = 0x40
    const val MDB_CREATE = 0x40000
}

// Write flags
object LmdbWriteFlags {
    const val MDB_NOOVERWRITE = 0x10
    const val MDB_NODUPDATA = 0x20
    const val MDB_CURRENT = 0x40
    const val MDB_RESERVE = 0x10000
    const val MDB_APPEND = 0x20000
    const val MDB_APPENDDUP = 0x40000
    const val MDB_MULTIPLE = 0x80000
}

// Copy flags
object LmdbCopyFlags {
    const val MDB_CP_COMPACT = 0x01
}

// Cursor operations
object LmdbCursorOp {
    const val MDB_FIRST = 0
    const val MDB_FIRST_DUP = 1
    const val MDB_GET_BOTH = 2
    const val MDB_GET_BOTH_RANGE = 3
    const val MDB_GET_CURRENT = 4
    const val MDB_GET_MULTIPLE = 5
    const val MDB_LAST = 6
    const val MDB_LAST_DUP = 7
    const val MDB_NEXT = 8
    const val MDB_NEXT_DUP = 9
    const val MDB_NEXT_MULTIPLE = 10
    const val MDB_NEXT_NODUP = 11
    const val MDB_PREV = 12
    const val MDB_PREV_DUP = 13
    const val MDB_PREV_NODUP = 14
    const val MDB_SET = 15
    const val MDB_SET_KEY = 16
    const val MDB_SET_RANGE = 17
    const val MDB_PREV_MULTIPLE = 18
}