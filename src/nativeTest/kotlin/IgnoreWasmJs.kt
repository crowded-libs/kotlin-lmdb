package lmdb

/**
 * On Native, IgnoreWasmJs has no effect - tests run normally.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
actual annotation class IgnoreWasmJs()