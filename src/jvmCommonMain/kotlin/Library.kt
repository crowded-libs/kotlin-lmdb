package lmdb

/**
 * Internal library wrapper for LMDB JNA interface.
 * This class provides a simplified interface to the LmdbJna methods.
 */
internal object Library {
    const val LMDB_NATIVE_LIB_PROP = "lmdb.native.lib"
    private const val LMDB_DEBUG_PROP = "lmdb.debug"
    
    val DEBUG = System.getProperty(LMDB_DEBUG_PROP) != null
    
    init {
        // Ensure the native library is loaded
        LmdbJna // This will trigger the lazy loading in LmdbJna
        
        if (DEBUG) {
            println("[LMDB] Library initialized using JNA interface")
            println("[LMDB] Version: ${LmdbJna.mdb_version()}")
        }
    }
    
    // Expose the JNA interface for internal use
    val LMDB = LmdbJna
}