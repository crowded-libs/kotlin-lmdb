package lmdb

/**
 * Exception thrown when an LMDB operation fails.
 *
 * This exception wraps error codes and messages from the native LMDB library,
 * providing detailed information about what went wrong during an operation.
 *
 * Common error conditions include:
 * - Key not found
 * - Key already exists
 * - Map full (database size limit reached)
 * - Read-only transaction attempted a write
 * - Transaction has too many dirty pages
 * - Database was opened with incompatible flags
 * - Invalid parameters were specified
 *
 * @param message The error message describing what went wrong
 */
class LmdbException(message: String) : Exception(message)
