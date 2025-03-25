# Publishing to Maven Central

This document explains how to publish the Kotlin-LMDB library to Maven Central.

## Prerequisites

1. **Sonatype Account**: Create an account at [Sonatype JIRA](https://issues.sonatype.org/). This account will be used to publish artifacts to Maven Central.

2. **GPG Key**: Generate a GPG key pair for signing the artifacts.

3. **Configure Gradle Properties**: Add the following credentials to your `~/.gradle/gradle.properties` file:

```
ossrhUsername=your-sonatype-username
ossrhPassword=your-sonatype-password
signing.key=your-gpg-private-key
signing.password=your-gpg-private-key-password
```

Alternatively, you can set these as environment variables:
- `OSSRH_USERNAME`
- `OSSRH_PASSWORD`
- `SIGNING_KEY`
- `SIGNING_PASSWORD`

## Publishing Process

### 1. Prepare the Release

Update the version in `build.gradle.kts` to a non-SNAPSHOT version:

```kotlin
version = "0.1.0" // For a release version
```

### 2. Build and Publish

Run the following Gradle task to build, sign, and publish the artifacts:

```bash
./gradlew publishAllPublicationsToSonatypeRepository
```

### 3. Release to Maven Central

After publishing to Sonatype's staging repository, you need to go to the [Sonatype Nexus Repository Manager](https://s01.oss.sonatype.org/) and:

1. Log in with your Sonatype credentials
2. Go to "Staging Repositories"
3. Find your repository (usually with a name containing your group ID)
4. Verify the content
5. Click "Close" to prepare it for release
6. After checks pass, click "Release" to promote it to Maven Central

### 4. Verify Publication

Once published, your artifact should be accessible at:
- Maven Central: https://repo1.maven.org/maven2/com/github/crowded-libs/
- Sonatype: https://s01.oss.sonatype.org/content/repositories/releases/com/github/crowded-libs/

## Setting up a Development Version

For ongoing development, set the version to a SNAPSHOT version:

```kotlin
version = "0.2.0-SNAPSHOT"
```

This will publish to Sonatype's snapshot repository instead of the release repository.