package lmdb

/**
 * Contains statistics about a database in the LMDB environment.
 *
 * This class wraps the MDB_stat structure from the native LMDB library,
 * providing details about the database structure and content.
 * These statistics can be useful for monitoring database growth and
 * diagnosing performance issues.
 *
 * @property branchPages Number of internal (non-leaf) B-tree pages
 * @property depth Depth (height) of the B-tree
 * @property entries Number of data items (key-value pairs) in the database
 * @property leafPages Number of leaf pages
 * @property overflowPages Number of overflow pages
 * @property pSize Size of a database page in bytes
 */
class Stat(
    val branchPages: ULong,
    val depth: UInt,
    val entries: ULong,
    val leafPages: ULong,
    val overflowPages: ULong,
    val pSize: UInt
)