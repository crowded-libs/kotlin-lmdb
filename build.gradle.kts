plugins {
    kotlin("multiplatform") version "2.1.10"
    id("org.jetbrains.dokka") version "1.9.10"
    id("maven-publish")
    id("signing")
}

group = "com.github.crowded-libs"
version = "0.1.0-SNAPSHOT"
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
        withJava()
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
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.5.4")
            }
            languageSettings.optIn("kotlinx.io.core.ExperimentalIO")
        }
        val jvmMain by getting {
            dependencies {
                implementation("com.github.jnr:jnr-constants:0.10.4")
                implementation("com.github.jnr:jnr-ffi:2.2.13")
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
    
    // Create javadoc jar for Maven Central requirements
    val javadocJar by creating(Jar::class) {
        group = "documentation"
        archiveClassifier.set("javadoc")
        from(layout.buildDirectory.dir("dokka/html"))
        dependsOn("dokkaHtml")
    }
}

// Configure publishing
publishing {
    publications {
        withType<MavenPublication> {
            // Javadoc is required by Maven Central
            artifact(tasks.named("javadocJar"))
            
            // Configure POM required by Maven Central
            pom {
                name.set("kotlin-lmdb")
                // The artifactId is automatically set by Kotlin MPP plugin
                description.set(project.description)
                url.set("https://github.com/crowded-libs/kotlin-lmdb")
                
                licenses {
                    license {
                        name.set("OpenLDAP Public License")
                        url.set("https://www.openldap.org/software/release/license.html")
                        distribution.set("repo")
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
                    connection.set("scm:git:git://github.com/crowded-libs/kotlin-lmdb.git")
                    developerConnection.set("scm:git:ssh://github.com/crowded-libs/kotlin-lmdb.git")
                    url.set("https://github.com/crowded-libs/kotlin-lmdb")
                }
            }
        }
    }
    
    repositories {
        maven {
            name = "sonatype"
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            
            credentials {
                username = findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME") 
                password = findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD")
            }
        }
    }
}

// Configure signing
signing {
    val signingKey = findProperty("signing.key") as String? ?: System.getenv("SIGNING_KEY")
    val signingPassword = findProperty("signing.password") as String? ?: System.getenv("SIGNING_PASSWORD")
    
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    } else {
        isRequired = false // Don't require signing for local builds
    }
}