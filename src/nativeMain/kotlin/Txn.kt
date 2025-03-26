import TxnState.*
import kotlinx.cinterop.*
import lmdb.*

actual class Txn internal actual constructor(val env: Env, parent: Txn?, vararg options: TxnOption) : AutoCloseable {
    private val arena = Arena()
    private val parentTx: CPointer<MDB_txn>?
    internal val ptr: CPointer<MDB_txn>
    internal actual var state: TxnState
    private var isClosed = false
    
    actual val id: ULong
        get() {
            checkReady()
            return mdb_txn_id(ptr)
        }

    internal actual constructor(env: Env, vararg options: TxnOption) : this(env, null, *options)

    init {
        if(!env.isOpened) throw LmdbException("Env is not open")
        parentTx = parent?.ptr
        val ptrVar = arena.allocPointerTo<MDB_txn>()
        check(mdb_txn_begin(env.ptr, parent?.ptr, options.asIterable().toFlags(), ptrVar.ptr))
        ptr = checkNotNull(ptrVar.value)
        state = Ready
    }

    actual fun begin(vararg options: TxnOption) : Txn {
        return Txn(env, this)
    }

    actual fun abort() {
        checkReady()
        state = Done
        mdb_txn_abort(ptr)
    }

    actual fun reset() {
        when(state) {
            Ready, Done -> throw LmdbException("Transaction is in an invalid state for reset.")
            Reset, Released -> state = Reset
        }
        mdb_txn_reset(ptr)
    }

    actual fun renew() {
        if (state != Reset) {
            throw LmdbException("Transaction is in an invalid state for renew, must be reset.")
        }
        state = Done
        check(mdb_txn_renew(ptr))
        state = Ready
    }

    actual fun commit() {
        checkReady()
        state = Done
        check(mdb_txn_commit(ptr))
    }

    actual fun dbiOpen(name: String?, vararg options: DbiOption) : Dbi {
        return Dbi(name, this, *options)
    }
    
    actual fun dbiOpen(name: String?, config: DbiConfig, vararg options: DbiOption) : Dbi {
        val db = Dbi(name, this, *options)
        
        // Set key comparer if provided
        config.keyComparer?.let { comparer ->
            val keyCompareFn = ValComparerImpl.getComparerCallback(comparer)
            check(mdb_set_compare(ptr, db.dbi, keyCompareFn))
        }
        
        // Set duplicate data comparer if provided
        config.dupComparer?.let { comparer ->
            val dupCompareFn = ValComparerImpl.getComparerCallback(comparer)
            check(mdb_set_dupsort(ptr, db.dbi, dupCompareFn))
        }
        
        return db
    }

    actual fun get(dbi: Dbi, key: Val) : ValResult {
        val mdbData = Val.output()
        val code = mdb_get(ptr, dbi.dbi, key.mdbVal.ptr, mdbData.mdbVal.ptr)
        return buildReadResult(code, key, mdbData)
    }

    actual fun put(dbi: Dbi, key: Val, data: Val, vararg options: PutOption) {
        check(mdb_put(ptr, dbi.dbi, key.mdbVal.ptr, data.mdbVal.ptr, options.asIterable().toFlags()))
    }

    actual fun openCursor(dbi: Dbi): Cursor {
        return Cursor(this, dbi)
    }

    actual override fun close() {
        if(isClosed)
            return
        isClosed = true
        if(state == Released) {
            return
        }
        if (state == Ready) {
            mdb_txn_abort(ptr)
        }
        arena.clear()
        state = Released
    }

    actual fun drop(dbi: Dbi) {
        check(mdb_drop(ptr, dbi.dbi, 1))
    }

    actual fun empty(dbi: Dbi) {
        check(mdb_drop(ptr, dbi.dbi, 0))
    }

    actual fun delete(dbi: Dbi, key: Val) {
        check(mdb_del(ptr, dbi.dbi, key.mdbVal.ptr, null))
    }

    actual fun delete(dbi: Dbi, key: Val, data: Val) {
        check(mdb_del(ptr, dbi.dbi, key.mdbVal.ptr, data.mdbVal.ptr))
    }
}
