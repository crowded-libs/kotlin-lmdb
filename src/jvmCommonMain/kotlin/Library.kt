package lmdb

/**
 * Internal library wrapper for LMDB JNA interface.
 * This class provides a simplified interface to the LmdbJna methods.
 */
internal object Library {
    const val LMDB_NATIVE_LIB_PROP = "lmdb.native.lib"
    private const val LMDB_DEBUG_PROP = "lmdb.debug"
    
    val DEBUG = System.getProperty(LMDB_DEBUG_PROP) != null
    val LMDB = LmdbJna
    
    init {
        if (DEBUG) {
            println("[LMDB] Library initialized using JNA interface")
            println("[LMDB] Version: ${LMDB.mdb_version()}")
        }
    }
}
