plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.dokka)
    alias(libs.plugins.dokka.javadoc)
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
    dokkaSourceSets.configureEach {
        includes.from("README.md")
        sourceLink {
            localDirectory.set(file("src/commonMain/kotlin"))
            remoteUrl("https://github.com/crowded-libs/kotlin-lmdb/blob/main/src/commonMain/kotlin")
        }
    }
}

val dokkaJavadocJar by tasks.registering(Jar::class) {
    description = "A Javadoc JAR containing Dokka Javadoc"
    from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

val dokkaHtmlJar by tasks.registering(Jar::class) {
    description = "A HTML Documentation JAR containing Dokka HTML"
    from(tasks.dokkaGeneratePublicationHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("html-doc")
}

// Configure publishing
publishing {
    publications {
        register<MavenPublication>("kotlin-lmdb") {
            groupId = group.toString()
            version = version.toString()
            description = project.description
            pom {
                name = project.name
                groupId = group.toString()
                version = version.toString()
                description = project.description
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
            artifact(dokkaJavadocJar)
            artifact(dokkaHtmlJar)
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