package lmdb

/**
 * Annotation to ignore tests on wasmJs platform.
 * Uses expect/actual pattern - acts as @Ignore on wasmJs, no effect on other platforms.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
expect annotation class IgnoreWasmJs()