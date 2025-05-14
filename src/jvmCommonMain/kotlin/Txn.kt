package lmdb

import lmdb.Library.Companion.LMDB
import lmdb.Library.Companion.RUNTIME
import lmdb.TxnState.*
import jnr.ffi.Memory.allocateDirect
import jnr.ffi.NativeType
import jnr.ffi.Pointer

actual class Txn internal actual constructor(env: Env, parent: Txn?, vararg options: TxnOption) : AutoCloseable {
    val env: Env
    private val txnPtr = allocateDirect(RUNTIME, NativeType.ADDRESS)
    internal val ptr: Pointer
    private val parentTx: Pointer?
    internal actual var state: TxnState
    
    actual val id: ULong
        get() {
            checkReady()
            return LMDB.mdb_txn_id(ptr).toULong()
        }

    internal actual constructor(env: Env, vararg options: TxnOption) : this(env, null, *options)

    init {
        if(!env.isOpened) throw LmdbException("Env is not open")
        this.env = env
        parentTx = parent?.ptr
        check(LMDB.mdb_txn_begin(env.ptr, parentTx, options.asIterable().toFlags().toInt(), txnPtr))
        ptr = txnPtr.getPointer(0)
        state = Ready
    }

    actual fun begin(vararg options: TxnOption) : Txn {
        return Txn(env, this)
    }

    actual fun abort() {
        checkReady()
        LMDB.mdb_txn_abort(ptr)
        state = Done
    }

    actual fun reset() {
        when(state) {
            Ready, Done -> throw LmdbException("Transaction is in an invalid state for reset.")
            Reset, Released -> state = Reset
        }
        LMDB.mdb_txn_reset(ptr)
    }

    actual fun renew() {
       if (state != Reset) {
           throw LmdbException("Transaction is in an invalid state for renew, must be reset.")
       }
        state = Done
        check(LMDB.mdb_txn_renew(ptr))
        state = Ready
    }

    actual fun commit() {
        checkReady()
        state = Done
        check(LMDB.mdb_txn_commit(ptr))
    }

    actual fun dbiOpen(name: String?, vararg options: DbiOption) : Dbi {
        return Dbi(name, this, *options)
    }
    
    actual fun dbiOpen(name: String?, config: DbiConfig, vararg options: DbiOption) : Dbi {
        val dbi = Dbi(name, this, *options)
        
        // Set key comparer if provided
        config.keyComparer?.let { comparer ->
            val keyComparatorCallback = ValComparerImpl.getComparerCallback(comparer)
            check(LMDB.mdb_set_compare(ptr, dbi.ptr, keyComparatorCallback))
        }
        
        // Set duplicate data comparer if provided
        config.dupComparer?.let { comparer ->
            val dupComparatorCallback = ValComparerImpl.getComparerCallback(comparer)
            check(LMDB.mdb_set_dupsort(ptr, dbi.ptr, dupComparatorCallback))
        }
        
        return dbi
    }
    
    actual fun get(dbi: Dbi, key: Val) : ValResult {
        val data = MDBVal.output()
        val resultCode = LMDB.mdb_get(ptr, dbi.ptr, key.mdbVal.ptr, data.ptr)
        return buildReadResult(resultCode, key, Val.fromMDBVal(data))
    }

    actual fun put(dbi: Dbi, key: Val, data: Val, vararg options: PutOption) {
        check(LMDB.mdb_put(ptr, dbi.ptr, key.mdbVal.ptr, data.mdbVal.ptr,
            options.asIterable().toFlags().toInt()))
    }

    actual fun openCursor(dbi: Dbi): Cursor {
        return Cursor(this, dbi)
    }

    actual override fun close() {
        if(state == Released) {
            return
        }
        if (state == Ready) {
            LMDB.mdb_txn_abort(ptr)
        }
        state = Released
    }

    actual fun drop(dbi: Dbi) {
        check(LMDB.mdb_drop(ptr, dbi.ptr, 1))
    }

    actual fun empty(dbi: Dbi) {
        check(LMDB.mdb_drop(ptr, dbi.ptr, 0))
    }

    actual fun delete(dbi: Dbi, key: Val) {
        check(LMDB.mdb_del(ptr, dbi.ptr, key.mdbVal.ptr, null))
    }

    actual fun delete(dbi: Dbi, key: Val, data: Val) {
        check(LMDB.mdb_del(ptr, dbi.ptr, key.mdbVal.ptr, data.mdbVal.ptr))
    }
}
