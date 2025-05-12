package lmdb

/**
 * Contains information about the LMDB environment.
 *
 * This class wraps the MDB_envinfo structure from the native LMDB library,
 * providing details about the memory map, page usage, transaction state,
 * and reader slots.
 *
 * @property lastPgNo ID of the last used page in the database
 * @property lastTxnId ID of the last committed transaction
 * @property mapAddr Address of the memory map (if fixed)
 * @property mapSize Size of the data memory map in bytes
 * @property maxReader Maximum number of reader slots configured for the environment
 * @property numReaders Number of reader slots currently in use
 */
class EnvInfo(
    val lastPgNo: ULong,
    val lastTxnId: ULong,
    val mapAddr: ULong,
    val mapSize: ULong,
    val maxReader: UInt,
    val numReaders: UInt
)