plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
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
