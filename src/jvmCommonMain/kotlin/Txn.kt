package lmdb

import com.sun.jna.Pointer
import lmdb.TxnState.*

actual class Txn internal actual constructor(env: Env, parent: Txn?, vararg options: TxnOption) : AutoCloseable {
    val env: Env
    internal val ptr: Pointer
    private val parentPtr: Pointer?
    internal actual var state: TxnState
    
    actual val id: ULong
        get() {
            checkReady()
            return LmdbJna.mdb_txn_id(ptr).toULong()
        }

    internal actual constructor(env: Env, vararg options: TxnOption) : this(env, null, *options)

    init {
        if(!env.isOpened) throw LmdbException("Env is not open")
        this.env = env
        parentPtr = parent?.ptr
        ptr = LmdbJna.mdb_txn_begin(env.ptr, parentPtr, options.asIterable().toFlags().toInt())
        state = Ready
    }

    actual fun begin(vararg options: TxnOption) : Txn {
        return Txn(env, this)
    }

    actual fun abort() {
        checkReady()
        LmdbJna.mdb_txn_abort(ptr)
        state = Done
    }

    actual fun reset() {
        when(state) {
            Ready, Done -> throw LmdbException("Transaction is in an invalid state for reset.")
            Reset, Released -> state = Reset
        }
        LmdbJna.mdb_txn_reset(ptr)
    }

    actual fun renew() {
       if (state != Reset) {
           throw LmdbException("Transaction is in an invalid state for renew, must be reset.")
       }
        state = Done
        check(LmdbJna.mdb_txn_renew(ptr))
        state = Ready
    }

    actual fun commit() {
        checkReady()
        state = Done
        check(LmdbJna.mdb_txn_commit(ptr))
    }

    actual fun dbiOpen(name: String?, vararg options: DbiOption) : Dbi {
        checkReady()
        return Dbi(name, this, *options)
    }
    
    actual fun dbiOpen(name: String?, config: DbiConfig, vararg options: DbiOption) : Dbi {
        checkReady()
        val dbi = Dbi(name, this, *options)
        
        // Set key comparer if provided
        config.keyComparer?.let { comparer ->
            val keyComparatorCallback = ValComparerImpl.getComparerCallback(comparer)
            check(LmdbJna.mdb_set_compare(ptr, dbi.dbiHandle, keyComparatorCallback))
        }
        
        // Set duplicate data comparer if provided
        config.dupComparer?.let { comparer ->
            val dupComparatorCallback = ValComparerImpl.getComparerCallback(comparer)
            check(LmdbJna.mdb_set_dupsort(ptr, dbi.dbiHandle, dupComparatorCallback))
        }
        
        return dbi
    }
    
    actual fun get(dbi: Dbi, key: Val) : ValResult {
        checkReady()
        // Use the direct get method that returns the MDB_val
        val (resultCode, dataVal) = LmdbJna.mdb_get_direct(ptr, dbi.dbiHandle, key.mdbVal.buffer)
        
        return if (resultCode == 0 && dataVal != null) {
            // Create Val from the returned MDB_val
            val data = Val.fromMDBVal(MDBVal.fromMdbVal(dataVal))
            buildReadResult(resultCode, key, data)
        } else {
            buildReadResult(resultCode, key, Val.fromMDBVal(MDBVal.output()))
        }
    }

    actual fun put(dbi: Dbi, key: Val, data: Val, vararg options: PutOption) {
        checkReady()
        check(LmdbJna.mdb_put(ptr, dbi.dbiHandle, key.mdbVal.buffer, data.mdbVal.buffer,
            options.asIterable().toFlags().toInt()))
    }

    actual fun openCursor(dbi: Dbi): Cursor {
        checkReady()
        return Cursor(this, dbi)
    }

    actual override fun close() {
        if(state == Released) {
            return
        }
        if (state == Ready) {
            LmdbJna.mdb_txn_abort(ptr)
        }
        state = Released
    }

    actual fun drop(dbi: Dbi) {
        checkReady()
        check(LmdbJna.mdb_drop(ptr, dbi.dbiHandle, 1))
    }

    actual fun empty(dbi: Dbi) {
        checkReady()
        check(LmdbJna.mdb_drop(ptr, dbi.dbiHandle, 0))
    }

    actual fun delete(dbi: Dbi, key: Val) {
        checkReady()
        check(LmdbJna.mdb_del(ptr, dbi.dbiHandle, key.mdbVal.buffer, null))
    }

    actual fun delete(dbi: Dbi, key: Val, data: Val) {
        checkReady()
        check(LmdbJna.mdb_del(ptr, dbi.dbiHandle, key.mdbVal.buffer, data.mdbVal.buffer))
    }
    
    private fun checkReady() {
        if (state != Ready) {
            throw LmdbException("Transaction is not in Ready state (current state: $state)")
        }
    }
}