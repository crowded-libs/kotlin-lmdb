package lmdb

import com.sun.jna.Pointer
import lmdb.TxnState.*

actual class Txn internal actual constructor(env: Env, parent: Txn?, vararg options: TxnOption) : AutoCloseable {
    val env: Env
    internal val ptr: Pointer
    private val parentPtr: Pointer?
    internal actual var state: TxnState

    // Reusable native staging for get/put/delete; created on first use, freed on close().
    private var scratchOrNull: NativeScratch? = null
    private val scratch: NativeScratch
        get() = scratchOrNull ?: NativeScratch().also { scratchOrNull = it }
    
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
        val s = scratch
        s.stageKey(key.stagingBytes())
        s.clearData()
        val resultCode = LmdbJna.mdb_get(ptr, dbi.dbiHandle, s.keyVal, s.dataVal)
        return if (resultCode == 0) {
            // Zero-copy view into LMDB pages; valid until the next operation on this txn.
            buildReadResult(resultCode, key, Val.fromMDBVal(MDBVal.fromMdbVal(s.dataVal)))
        } else {
            buildReadResult(resultCode, key, Val.fromMDBVal(MDBVal.EMPTY))
        }
    }

    actual fun put(dbi: Dbi, key: Val, data: Val, vararg options: PutOption) {
        checkReady()
        val s = scratch
        s.stageKey(key.stagingBytes())
        s.stageData(data.stagingBytes())
        check(LmdbJna.mdb_put(ptr, dbi.dbiHandle, s.keyVal, s.dataVal,
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
        scratchOrNull?.close()
        scratchOrNull = null
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
        val s = scratch
        s.stageKey(key.stagingBytes())
        check(LmdbJna.mdb_del(ptr, dbi.dbiHandle, s.keyVal, null))
    }

    actual fun delete(dbi: Dbi, key: Val, data: Val) {
        checkReady()
        val s = scratch
        s.stageKey(key.stagingBytes())
        s.stageData(data.stagingBytes())
        check(LmdbJna.mdb_del(ptr, dbi.dbiHandle, s.keyVal, s.dataVal))
    }

    private fun checkReady() {
        if (state != Ready) {
            throw LmdbException("Transaction is not in Ready state (current state: $state)")
        }
    }
}