import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

group = "io.github.crowded-libs"
version = "0.1.0"
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
            jvmTarget = JvmTarget.JVM_11
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
        val jvmMain by getting {
            dependencies {
                implementation(libs.jnr.constants)
                implementation(libs.jnr.ffi)
            }
        }
        val nativeMain by creating { 
            dependsOn(commonMain)
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
        val nativeTest by creating { dependsOn(commonTest) }

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

        val androidMain by getting { dependsOn(jvmMain) }
        val androidUnitTest by getting { dependsOn(jvmTest.get()) }

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
}

android {
    namespace = "lmdb"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

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
