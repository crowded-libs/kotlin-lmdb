import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

group = "com.github.crowded-libs"
version = "0.1.0"
description = "Kotlin Multiplatform library for LMDB key-value store"

repositories {
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
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val hostArch = System.getProperty("os.arch")
    val isMacosArm64 = hostOs == "Mac OS X" && hostArch == "aarch64"
    val isMacosX64 = hostOs == "Mac OS X" && !isMacosArm64
    
    when {
        isMacosArm64 -> macosArm64("native")
        isMacosX64 -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }.apply {
        compilations.getByName("main") {
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
        val jvmMain by getting {
            dependencies {
                implementation(libs.jnr.constants)
                implementation(libs.jnr.ffi)
            }
        }
        val jvmTest by getting

        val nativeMain by getting {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
        val nativeTest by getting {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }

        all {
            languageSettings.apply {
                optIn("kotlin.uuid.ExperimentalUuidApi")
                optIn("kotlin.experimental.ExperimentalNativeApi")
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
}

dokka {
    moduleName = project.name
    dokkaSourceSets {
        named("commonMain")
        named("jvmMain")
        named("nativeMain")
    }
}

val dokkaHtmlJar by tasks.registering(Jar::class) {
    description = "A HTML Documentation JAR containing Dokka HTML"
    from(tasks.dokkaGeneratePublicationHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("html-doc")
}

// Configure publishing
mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

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