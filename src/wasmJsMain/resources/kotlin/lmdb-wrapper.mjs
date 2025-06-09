// LMDB WASM wrapper module for Kotlin/WASM compatibility
// This module handles async loading of the WASM module and re-exports functions
// for use with @WasmImport annotations

import loadLmdbWASM from './lmdb.mjs';

// Load the WASM module asynchronously
// loadLmdbWASM() returns a Promise that resolves to the actual module
const wasmModule = await loadLmdbWASM();

// Track mounted filesystem paths to avoid duplicate mounts
const mountedPaths = new Set();

// Initialize filesystem once at module load
(function initializeFilesystem() {
    if (isNodeJS()) {
        console.log('Initializing NODEFS for Node.js environment...');
        try {
            // Create /tmp directory if it doesn't exist
            wasmModule.FS.mkdir('/tmp');
        } catch (e) {
            // Directory may already exist, that's fine
        }
        
        try {
            // Mount NODEFS at /tmp for real filesystem access
            // In Node.js environments, we can access the filesystem
            if (typeof globalThis.process !== 'undefined' && globalThis.process.versions && globalThis.process.versions.node) {
                // We're in Node.js - fs, os, and path are available globally in Node.js WASM environment
                // Mount current directory as /tmp
                wasmModule.FS.mount(wasmModule.FS.filesystems.NODEFS, { root: '.' }, '/tmp');
                console.log('NODEFS mounted at /tmp -> . successfully');
            }
        } catch (err) {
            console.warn('Failed to mount NODEFS:', err);
        }
    }
})();

// Detect environment type
function isNodeJS() {
    return typeof process !== 'undefined' && process.versions && process.versions.node;
}

function isBrowser() {
    return typeof window !== 'undefined' && typeof indexedDB !== 'undefined';
}

// Mount IDBFS for persistence (browser environment)
function mountIDBFSAtPath(pathPtr) {
    const path = wasmModule.UTF8ToString(pathPtr);
    
    if (mountedPaths.has(path)) {
        return 1; // Already mounted
    }
    
    // Check if IndexedDB is available (browser environment)
    if (!isBrowser()) {
        console.log('IndexedDB not available (likely Node.js environment)');
        return 0; // Failed
    }
    
    try {
        // Create the directory if it doesn't exist
        wasmModule.FS.mkdirTree(path);
        
        // Mount IDBFS at the path
        wasmModule.FS.mount(wasmModule.FS.filesystems.IDBFS, {}, path);
        
        // Sync from IndexedDB to memory (populate=true)
        wasmModule.FS.syncfs(true, (err) => {
            if (err) {
                console.warn('IDBFS sync from IndexedDB failed:', err);
            } else {
                console.log('IDBFS synced from IndexedDB at:', path);
            }
        });
        
        mountedPaths.add(path);
        console.log('IDBFS mounted at:', path);
        return 1; // Success
    } catch (err) {
        console.warn('Failed to mount IDBFS at', path, ':', err);
        return 0; // Failed
    }
}

// Mount NODEFS for real filesystem access (Node.js environment)
function mountNODEFSAtPath(pathPtr) {
    const path = wasmModule.UTF8ToString(pathPtr);
    
    if (mountedPaths.has(path)) {
        return 1; // Already mounted
    }
    
    // Check if we're in Node.js environment
    if (!isNodeJS()) {
        console.log('NODEFS only available in Node.js environment');
        return 0; // Failed
    }
    
    try {
        // Create the mount point in WASM filesystem
        wasmModule.FS.mkdirTree(path);
        
        // In Node.js WASM environment, we can mount NODEFS
        // Just mount the current directory at the requested path
        wasmModule.FS.mount(wasmModule.FS.filesystems.NODEFS, { root: '.' }, path);
        
        mountedPaths.add(path);
        console.log(`NODEFS mounted at: ${path} -> .`);
        return 1; // Success
    } catch (err) {
        console.warn('Failed to mount NODEFS at', path, ':', err);
        return 0; // Failed
    }
}

// Smart filesystem mounting: try NODEFS first in Node.js, fallback to IDBFS in browser
function mountBestFilesystem(pathPtr) {
    if (isNodeJS()) {
        return mountNODEFSAtPath(pathPtr);
    } else if (isBrowser()) {
        return mountIDBFSAtPath(pathPtr);
    } else {
        console.log('Unknown environment, no persistent filesystem available');
        return 0; // Failed
    }
}

// Export mount functions
export const mountIDBFS = mountIDBFSAtPath;
export const mountNODEFS = mountNODEFSAtPath;
export const mountFilesystem = mountBestFilesystem;

// Re-export all LMDB functions for @WasmImport compatibility
// Memory management
export const _malloc = wasmModule._malloc;
export const _free = wasmModule._free;

// Memory utilities - convert integer type constants to string types for Emscripten
export const getValue = (ptr, type) => {
    const typeStr = type === 0 ? "i8" : type === 1 ? "i16" : type === 2 ? "i32" : type === 3 ? "double" : "i32";
    return wasmModule.getValue(ptr, typeStr);
};

export const setValue = (ptr, value, type) => {
    const typeStr = type === 0 ? "i8" : type === 1 ? "i16" : type === 2 ? "i32" : type === 3 ? "double" : "i32";
    wasmModule.setValue(ptr, value, typeStr);
};

// Environment functions
export const _mdb_env_create = wasmModule._mdb_env_create;
export const _mdb_env_open = wasmModule._mdb_env_open;
export const _mdb_env_close = wasmModule._mdb_env_close;
export const _mdb_env_set_maxdbs = wasmModule._mdb_env_set_maxdbs;
export const _mdb_env_set_mapsize = wasmModule._mdb_env_set_mapsize;
export const _mdb_env_get_maxreaders = wasmModule._mdb_env_get_maxreaders;
export const _mdb_env_set_maxreaders = wasmModule._mdb_env_set_maxreaders;
export const _mdb_env_get_maxkeysize = wasmModule._mdb_env_get_maxkeysize;
export const _mdb_reader_check = wasmModule._mdb_reader_check;
export const _mdb_env_get_flags = wasmModule._mdb_env_get_flags;
export const _mdb_env_set_flags = wasmModule._mdb_env_set_flags;
export const _mdb_env_stat = wasmModule._mdb_env_stat;
export const _mdb_env_info = wasmModule._mdb_env_info;
export const _mdb_env_copy = wasmModule._mdb_env_copy;
export const _mdb_env_copy2 = wasmModule._mdb_env_copy2;
export const _mdb_env_sync = wasmModule._mdb_env_sync;

// Transaction functions
export const _mdb_txn_begin = wasmModule._mdb_txn_begin;
export const _mdb_txn_commit = wasmModule._mdb_txn_commit;
export const _mdb_txn_abort = wasmModule._mdb_txn_abort;
export const _mdb_txn_reset = wasmModule._mdb_txn_reset;
export const _mdb_txn_renew = wasmModule._mdb_txn_renew;
export const _mdb_txn_id = wasmModule._mdb_txn_id;

// Database functions
export const _mdb_dbi_open = wasmModule._mdb_dbi_open;
export const _mdb_dbi_close = wasmModule._mdb_dbi_close;
export const _mdb_drop = wasmModule._mdb_drop;
export const _mdb_stat = wasmModule._mdb_stat;
export const _mdb_dbi_flags = wasmModule._mdb_dbi_flags;
export const _mdb_cmp = wasmModule._mdb_cmp;
export const _mdb_dcmp = wasmModule._mdb_dcmp;

// Data operations
export const _mdb_get = wasmModule._mdb_get;
export const _mdb_put = wasmModule._mdb_put;
export const _mdb_del = wasmModule._mdb_del;

// Cursor operations
export const _mdb_cursor_open = wasmModule._mdb_cursor_open;
export const _mdb_cursor_close = wasmModule._mdb_cursor_close;
export const _mdb_cursor_get = wasmModule._mdb_cursor_get;
export const _mdb_cursor_put = wasmModule._mdb_cursor_put;
export const _mdb_cursor_del = wasmModule._mdb_cursor_del;
export const _mdb_cursor_count = wasmModule._mdb_cursor_count;
export const _mdb_cursor_renew = wasmModule._mdb_cursor_renew;

// Error handling
export const _mdb_strerror = wasmModule._mdb_strerror;

// Version information
export const _mdb_version = wasmModule._mdb_version;

// Filesystem functions
export const _mkdir = wasmModule._mkdir;
export const _access = wasmModule._access;
export const _rmdir = wasmModule._rmdir;
export const _unlink = wasmModule._unlink;
export const _opendir = wasmModule._opendir;
export const _readdir = wasmModule._readdir;
export const _closedir = wasmModule._closedir;

// Note: Filesystem sync may not be needed with MDB_WRITEMAP as it bypasses mmap limitations
// Keeping a no-op implementation for compatibility
export function syncFilesystem() {
    // MDB_WRITEMAP handles persistence internally, manual sync likely unnecessary
    return 1; // Always success
}