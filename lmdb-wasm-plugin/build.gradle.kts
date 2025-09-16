plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    alias(libs.plugins.maven.publish)
}

group = "io.github.crowded-libs"
version = libs.versions.kotlin.lmdb.get()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

gradlePlugin {
    website.set("https://github.com/crowded-libs/kotlin-lmdb")
    vcsUrl.set("https://github.com/crowded-libs/kotlin-lmdb.git")
    plugins {
        create("kotlinLmdbWasm") {
            id = "io.github.crowded-libs.kotlin-lmdb-wasm"
            displayName = "Kotlin LMDB Wasm Resource Plugin"
            description = "Ensures LMDB wasm/JS resources from kotlin-lmdb are available to wasmJs targets"
            implementationClass = "io.github.crowdedlibs.lmdb.wasm.plugin.KotlinLmdbWasmPlugin"
        }
    }
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Use Gradle embedded Kotlin for plugin implementation to avoid version mismatch warning
    implementation(kotlin("gradle-plugin"))
}

// Configure publishing
mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group.toString(), "kotlin-lmdb-wasm-plugin", version.toString())

    pom {
        name = "kotlin-lmdb-wasm-plugin"
        description = "Gradle plugin for managing WASM resources in kotlin-lmdb wasmJs targets"
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
