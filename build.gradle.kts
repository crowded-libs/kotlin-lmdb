@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

group = "io.github.crowded-libs"
version = libs.versions.kotlin.lmdb.get()
description = "Kotlin Multiplatform library for LMDB key-value store"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlinx/dev")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.RequiresOptIn")
    }
    jvmToolchain(21)
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    androidTarget {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
    }
    mingwX64()
    linuxX64()
    linuxArm64()
    macosX64()
    macosArm64()
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    watchosArm32()
    watchosArm64()
    watchosSimulatorArm64()
    tvosArm64()
    tvosSimulatorArm64()
    wasmJs {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        nodejs()
    }

    listOf(mingwX64(), linuxX64(), linuxArm64(), macosX64(), macosArm64(),
        iosArm64(), iosSimulatorArm64(), iosX64(), watchosArm32(),
        watchosArm64(), watchosSimulatorArm64(), tvosArm64(), tvosSimulatorArm64())
        .forEach { target ->
        target.compilations.getByName("main") {
            cinterops {
                val liblmdb by creating {
                    defFile(project.file("src/nativeInterop/cinterop/liblmdb.def"))
                    includeDirs("src/nativeInterop/cinterop/c/")
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.io.core)
            }
            languageSettings.optIn("kotlinx.io.core.ExperimentalIO")
        }
        val jvmCommonMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.jna)
                implementation(libs.jna.platform)
            }
        }
        val jvmMain by getting {
            dependsOn(jvmCommonMain)
        }
        val nativeMain by creating {
            dependsOn(commonMain)
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
        val nativeTest by creating { dependsOn(commonTest) }


        val wasmJsMain by getting {
            languageSettings.apply {
                optIn("kotlin.js.ExperimentalWasmJsInterop")
                optIn("kotlin.wasm.ExperimentalWasmInterop")
            }
            dependsOn(commonMain)
            resources.srcDirs("src/wasmJsMain/resources")
        }
        val wasmJsTest by getting {
            dependsOn(commonTest)
        }

        val iosMain by creating { dependsOn(nativeMain) }
        val iosX64Main by getting { dependsOn(iosMain) }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }

        val iosTest by creating { dependsOn(nativeTest) }
        val iosX64Test by getting { dependsOn(iosTest) }
        val iosArm64Test by getting { dependsOn(iosTest) }
        val iosSimulatorArm64Test by getting { dependsOn(iosTest) }

        val macosTest by creating { dependsOn(nativeTest) }
        val macosX64Test by getting { dependsOn(macosTest) }
        val macosArm64Test by getting { dependsOn(macosTest) }

        val linuxTest by creating { dependsOn(nativeTest) }
        val linuxX64Test by getting { dependsOn(linuxTest) }
        val linuxArm64Test by getting { dependsOn(linuxTest) }

        val mingwX64Test by getting { dependsOn(nativeTest) }

        val watchosTest by creating { dependsOn(nativeTest) }
        val watchosArm32Test by getting { dependsOn(watchosTest) }
        val watchosArm64Test by getting { dependsOn(watchosTest) }
        val watchosSimulatorArm64Test by getting { dependsOn(watchosTest) }

        val tvosTest by creating { dependsOn(nativeTest) }
        val tvosArm64Test by getting { dependsOn(tvosTest) }
        val tvosSimulatorArm64Test by getting { dependsOn(tvosTest) }

        val jvmCommonTest by creating {
            dependsOn(commonTest)
        }

        val jvmTest by getting {
            dependsOn(jvmCommonTest)
        }

        val androidMain by getting {
            dependsOn(jvmCommonMain)
        }
        val androidUnitTest by getting {
            dependsOn(jvmCommonTest)
            resources.srcDirs(jvmCommonMain.resources.srcDirs)
        }

        val macosMain by creating { dependsOn(nativeMain) }
        val macosX64Main by getting { dependsOn(macosMain) }
        val macosArm64Main by getting { dependsOn(macosMain) }

        val linuxMain by creating { dependsOn(nativeMain) }
        val linuxX64Main by getting { dependsOn(linuxMain) }
        val linuxArm64Main by getting { dependsOn(linuxMain) }

        val mingwX64Main by getting { dependsOn(nativeMain) }

        val watchosMain by creating { dependsOn(nativeMain) }
        val watchosArm32Main by getting { dependsOn(watchosMain) }
        val watchosArm64Main by getting { dependsOn(watchosMain) }
        val watchosSimulatorArm64Main by getting { dependsOn(watchosMain) }

        val tvosMain by creating { dependsOn(nativeMain) }
        val tvosArm64Main by getting { dependsOn(tvosMain) }
        val tvosSimulatorArm64Main by getting { dependsOn(tvosMain) }

        all {
            languageSettings.apply {
                optIn("kotlin.uuid.ExperimentalUuidApi")
                optIn("kotlin.experimental.ExperimentalNativeApi")
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }
    }
}
tasks {
    withType<Test> {
        testLogging {
            showStandardStreams = true
        }
    }

    // Set duplicates strategy for all copy tasks to handle duplicate resources
    withType<Copy> {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

android {
    namespace = "lmdb"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/jvmCommonMain/resources/jniLibs")
            resources.srcDirs("src/jvmCommonMain/resources")
        }
    }

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dokka {
    moduleName = project.name
}

val dokkaHtmlJar by tasks.registering(Jar::class) {
    description = "A HTML Documentation JAR containing Dokka HTML"
    from(tasks.dokkaGeneratePublicationHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("html-doc")
}

// Configure publishing
mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "kotlin-lmdb", version.toString())

    pom {
        name = "kotlin-lmdb"
        description = project.description
        inceptionYear = "2025"
        url = "https://github.com/crowded-libs/kotlin-lmdb"

        licenses {
            license {
                name = "OpenLDAP Public License"
                url = "https://www.openldap.org/software/release/license.html"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id.set("coreykaylor")
                name.set("Corey Kaylor")
                email.set("corey@kaylors.net")
            }
        }

        scm {
            url = "https://github.com/crowded-libs/kotlin-lmdb"
            connection = "scm:git:git://github.com/crowded-libs/kotlin-lmdb.git"
            developerConnection = "scm:git:ssh://github.com/crowded-libs/kotlin-lmdb.git"
        }
    }
}

// Custom tasks for compiling and linking LMDB for WASM
abstract class CompileLmdbWasmTask : DefaultTask() {
    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val compiler: Property<String>

    @get:Input
    abstract val flags: ListProperty<String>

    @TaskAction
    fun compile() {
        val outDir = outputDir.get().asFile
        outDir.mkdirs()

        val sourceFiles = sourceDir.get().asFile.listFiles { file ->
                file.name.endsWith(".c")
            } ?: emptyArray()

        sourceFiles.forEach { sourceFile ->
            val outputFile = File(outDir, sourceFile.nameWithoutExtension + ".o")

            project.providers.exec {
                    commandLine(
                        compiler.get(),
                        "-c",
                    "-o", outputFile.absolutePath,
                        *flags.get().toTypedArray(),
                    sourceFile.absolutePath
                    )
            }.result.get().assertNormalExitValue()

            logger.lifecycle("Compiled ${sourceFile.name} to ${outputFile.name}")
        }
    }
}

abstract class LinkLmdbWasmTask : DefaultTask() {
    @get:InputFiles
    abstract val objectFiles: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Input
    abstract val linker: Property<String>

    @get:Input
    abstract val flags: ListProperty<String>

    @get:InputFiles
    @get:Optional
    abstract val jsPrefix: ConfigurableFileCollection

    @TaskAction
    fun link() {
        val output = outputFile.get().asFile
        output.parentFile.mkdirs()

        val linkArgs = mutableListOf<String>().apply {
                add(linker.get())
                add("-o")
                add(output.absolutePath)
                addAll(objectFiles.files.map { it.absolutePath })
                addAll(flags.get())

                // Add JS prefix files if provided
                jsPrefix.files.forEach { prefixFile ->
                    add("--extern-post-js")
                    add(prefixFile.absolutePath)
                }
            }

        project.providers.exec {
                commandLine(linkArgs)
        }.result.get().assertNormalExitValue()

        logger.lifecycle("Linked LMDB WASM to ${output.name}")
    }
}

// Configure LMDB compilation and linking for wasmJs
val compileLmdbWasm by tasks.registering(CompileLmdbWasmTask::class) {
    val emscriptenPath = System.getenv("EMCC_PATH") ?: "/opt/homebrew/bin/emcc"
    sourceDir.set(file("src/nativeInterop/cinterop/c"))
    outputDir.set(layout.buildDirectory.dir("lmdb-wasm/obj"))
    compiler.set(emscriptenPath)
    flags.set(listOf(
            "-O2",
            "-g",
            "-DMDB_USE_POSIX_MUTEX=0",
            "-DMDB_USE_ROBUST=0",
    ))
}

val linkLmdbWasm by tasks.registering(LinkLmdbWasmTask::class) {
    dependsOn(compileLmdbWasm)

    objectFiles.from(compileLmdbWasm.map { 
            fileTree(it.outputDir) { include("*.o") }
    })

    val emscriptenPath = System.getenv("EMCC_PATH") ?: "/opt/homebrew/bin/emcc"
    outputFile.set(layout.buildDirectory.file("lmdb-wasm/lmdb.js"))
    linker.set(emscriptenPath)

    flags.set(listOf(
        "-s", "WASM=1",
        "-s", "EXPORT_ES6=1",
        "-s", "MODULARIZE=1",
        "-s", "EXPORT_NAME='loadLmdbWASM'",
        "-s", "EXPORTED_FUNCTIONS=[" +
                "'_mdb_env_create','_mdb_env_open','_mdb_env_close'," +
                "'_mdb_env_set_maxdbs','_mdb_env_set_mapsize'," +
                "'_mdb_env_get_maxreaders','_mdb_env_set_maxreaders'," +
                "'_mdb_env_get_maxkeysize','_mdb_reader_check'," +
                "'_mdb_env_get_flags','_mdb_env_set_flags'," +
                "'_mdb_env_stat','_mdb_env_info'," +
                "'_mdb_env_copy','_mdb_env_copy2','_mdb_env_sync'," +
                "'_mdb_txn_begin','_mdb_txn_commit','_mdb_txn_abort'," +
                "'_mdb_txn_reset','_mdb_txn_renew','_mdb_txn_id'," +
                "'_mdb_dbi_open','_mdb_dbi_close','_mdb_drop'," +
                "'_mdb_stat','_mdb_dbi_flags'," +
                "'_mdb_cmp','_mdb_dcmp'," +
                "'_mdb_get','_mdb_put','_mdb_del'," +
                "'_mdb_cursor_open','_mdb_cursor_close'," +
                "'_mdb_cursor_get','_mdb_cursor_put'," +
                "'_mdb_cursor_del','_mdb_cursor_count'," +
                "'_mdb_cursor_renew'," +
                "'_mdb_strerror','_mdb_version'," +
                "'_malloc','_free'," +
                "'_mkdir','_access','_rmdir','_unlink'," +
                "'_opendir','_readdir','_closedir'" +
                "]",
        "-s", "EXPORTED_RUNTIME_METHODS=['getValue','setValue','UTF8ToString','FS']",
        "-s", "IMPORTED_MEMORY=1",
        "-s", "ALLOW_MEMORY_GROWTH=1",
        "-s", "MAXIMUM_MEMORY=2GB",
        "-s", "INITIAL_MEMORY=16MB",
        "-s", "FORCE_FILESYSTEM=1",
        "-s", "FILESYSTEM=1",
            "-lidbfs.js",
            "-lnodefs.js",
            "-s",
            "ERROR_ON_UNDEFINED_SYMBOLS=0",
            "--no-entry",
        ),
    )
}
