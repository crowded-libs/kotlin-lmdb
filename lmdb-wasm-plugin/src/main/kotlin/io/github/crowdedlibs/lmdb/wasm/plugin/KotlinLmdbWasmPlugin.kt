package io.github.crowdedlibs.lmdb.wasm.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Sync
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.util.zip.ZipFile

/**
 * Extracts LMDB wasm runtime files (lmdb-wrapper.mjs, lmdb.mjs, lmdb.wasm) from the kotlin-lmdb dependency klib
 * into the consumer project's build so relative imports resolve without manual copy tasks.
 */
class KotlinLmdbWasmPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Act only when KMP plugin is present (immediately if already applied)
        project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            // Create a detached configuration to avoid configuration-time resolution
            val scanCfg = project.configurations.detachedConfiguration()

            // Add the kotlin-lmdb-wasm-js dependency directly to this configuration
            project.afterEvaluate {
                // Find kotlin-lmdb dependency from commonMainImplementation and create wasmJs-specific version
                project.configurations.findByName("commonMainImplementation")?.dependencies?.forEach { dep ->
                    if (dep.group == "io.github.crowded-libs" && dep.name == "kotlin-lmdb") {
                        // Add the wasmJs-specific artifact
                        val wasmDep = project.dependencies.create("${dep.group}:kotlin-lmdb-wasm-js:${dep.version}")
                        scanCfg.dependencies.add(wasmDep)
                    }
                }
            }

            val generatedDir = project.layout.buildDirectory.dir("generated/lmdb-wasm-runtime")

            val extractTask =
                project.tasks.register("syncLmdbWasmResources", Sync::class.java) {
                    group = "kotlin-lmdb"
                    description = "Extract LMDB wasm runtime resources from kotlin-lmdb dependency"

                    // Use a provider that defers resolution to task execution time
                    val extractedDirsProvider = project.provider {
                        val tempDir = project.layout.buildDirectory.dir("tmp/lmdb-wasm-extract").get().asFile
                        tempDir.mkdirs()

                        val seen = HashSet<String>()
                        val dirs = mutableListOf<java.io.File>()

                        // Only resolve at task execution time
                        scanCfg.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
                            val file = artifact.file
                            val moduleId = artifact.moduleVersion.id

                            // Look for kotlin-lmdb-wasm-js artifacts (both .klib and .jar)
                            if (moduleId.group == "io.github.crowded-libs" && moduleId.name == "kotlin-lmdb-wasm-js") {
                                if ((file.name.endsWith(".klib") && containsMarker(file)) ||
                                    (file.name.endsWith(".jar") && containsJarMarker(file))) {
                                    if (seen.add(file.absolutePath)) {
                                        dirs += extractToTempDir(tempDir, file)
                                    }
                                }
                            }
                        }

                        if (dirs.isEmpty()) {
                            project.logger.warn("No LMDB WASM resources found - ensure kotlin-lmdb dependency is present")
                        } else {
                            project.logger.info("Extracted LMDB WASM resources from ${dirs.size} artifacts to ${tempDir}")
                        }

                        dirs
                    }

                    from(extractedDirsProvider) {
                        include("lmdb-wrapper.mjs", "lmdb.mjs", "lmdb.wasm")
                    }
                    from(extractedDirsProvider) {
                        include("lmdb-wrapper.mjs", "lmdb.mjs", "lmdb.wasm")
                        into("kotlin")
                    }
                    into(generatedDir)
                }

            // Attach as resources to make them packaged for both source sets
            project.extensions.findByType(KotlinMultiplatformExtension::class.java)?.let { kmp ->
                kmp.sourceSets
                    .findByName("wasmJsMain")
                    ?.resources
                    ?.srcDir(generatedDir)
                kmp.sourceSets
                    .findByName("wasmJsTest")
                    ?.resources
                    ?.srcDir(generatedDir)
            }

            // Wire into wasmJs resource processing tasks (main + test)
            project.tasks.matching { it.name.startsWith("wasmJs") && it.name.endsWith("ProcessResources") }.configureEach {
                dependsOn(extractTask)
            }

            // Create a separate task to copy resources to WASM output directories
            val copyLmdbWasmTask = project.tasks.register("copyLmdbWasmToOutput") {
                group = "kotlin-lmdb"
                description = "Copy LMDB WASM resources to WASM output directories"
                dependsOn(extractTask)

                doLast {
                    val wasmOutputDir = project.rootProject.layout.buildDirectory.dir("wasm").get().asFile
                    val generatedResourcesDir = generatedDir.get().asFile

                    if (!generatedResourcesDir.exists()) {
                        project.logger.warn("Generated resources directory does not exist: ${generatedResourcesDir}")
                        return@doLast
                    }

                    if (!wasmOutputDir.exists()) {
                        project.logger.info("WASM output directory does not exist yet: ${wasmOutputDir}")
                        return@doLast
                    }

                    // Copy to all kotlin subdirectories in wasm output
                    val kotlinDirs = wasmOutputDir.walkTopDown()
                        .filter { it.isDirectory && it.name == "kotlin" }
                        .toList()

                    if (kotlinDirs.isEmpty()) {
                        project.logger.info("No kotlin output directories found yet in ${wasmOutputDir}")
                        return@doLast
                    }

                    kotlinDirs.forEach { kotlinDir ->
                        generatedResourcesDir.walkTopDown()
                            .filter { it.isFile && (it.name.endsWith(".mjs") || it.name.endsWith(".wasm")) }
                            .forEach { sourceFile ->
                                val targetFile = java.io.File(kotlinDir, sourceFile.name)
                                try {
                                    sourceFile.copyTo(targetFile, overwrite = true)
                                    project.logger.info("Copied ${sourceFile.name} to ${kotlinDir.absolutePath}")
                                } catch (e: Exception) {
                                    project.logger.warn("Failed to copy ${sourceFile.name}: ${e.message}")
                                }
                            }
                    }
                }
            }

            // Wire the copy task to run after both main and test compilation sync tasks
            project.tasks.matching {
                it.name.endsWith("wasmJsTestTestDevelopmentExecutableCompileSync") ||
                it.name.endsWith("wasmJsDevelopmentExecutableCompileSync") ||
                it.name.endsWith("wasmJsProductionExecutableCompileSync")
            }.configureEach {
                finalizedBy(copyLmdbWasmTask)
            }

            // Expose directory as an extra property (for potential downstream usage)
            project.extensions.extraProperties.set("kotlin.lmdb.generatedWasmRuntimeDir", generatedDir)
        }
    }

    private fun containsMarker(klib: java.io.File): Boolean =
        try {
            ZipFile(klib).use { zip ->
                zip.getEntry("resources/lmdb-wrapper.mjs") != null ||
                    zip.entries().asSequence().any { it.name.endsWith("lmdb-wrapper.mjs") }
            }
        } catch (_: Exception) {
            false
        }

    private fun containsJarMarker(jar: java.io.File): Boolean =
        try {
            ZipFile(jar).use { zip ->
                zip.entries().asSequence().any { entry ->
                    entry.name.endsWith("lmdb-wrapper.mjs") ||
                    entry.name.endsWith("lmdb.mjs") ||
                    entry.name.endsWith("lmdb.wasm")
                }
            }
        } catch (_: Exception) {
            false
        }

    private fun extractToTempDir(
        tempDir: java.io.File,
        archive: java.io.File,
    ): java.io.File {
        val outDir = java.io.File(tempDir, archive.nameWithoutExtension)
        if (outDir.exists()) return outDir
        outDir.mkdirs()
        ZipFile(archive).use { zip ->
            zip
                .entries()
                .asSequence()
                .filter {
                    !it.isDirectory &&
                        (it.name.endsWith("lmdb-wrapper.mjs") || it.name.endsWith("lmdb.mjs") || it.name.endsWith("lmdb.wasm"))
                }.forEach { entry ->
                    val target = java.io.File(outDir, entry.name.substringAfterLast('/'))
                    zip.getInputStream(entry).use { input -> target.outputStream().use { input.copyTo(it) } }
                }
        }
        return outDir
    }
}
