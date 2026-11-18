# Publishing

The library (`:richlinkpreview`) is published to **Maven Central** as
`io.github.sb-android-dev:richlinkpreview`, using the
[`com.vanniktech.maven.publish`](https://github.com/vanniktech/gradle-maven-publish-plugin) Gradle plugin.
Once it is on Maven Central it is also listed on mvnrepository.com, usually within a few days.

The coordinates live in `gradle.properties` (`GROUP`, `POM_ARTIFACT_ID`, `VERSION_NAME`) and the
POM details (license, developer, SCM) in `richlinkpreview/build.gradle.kts`.
**Credentials and signing keys never go in this repository.**

## One-time setup

1. **Central Portal account and namespace**
   - Sign in at <https://central.sonatype.com> with the `sb-android-dev` GitHub account.
   - Under *Namespaces*, add `io.github.sb-android-dev` and complete the verification.
   - Under *Account > Generate User Token*, create a token. Its username and password are
     `MAVEN_CENTRAL_USERNAME` / `MAVEN_CENTRAL_PASSWORD` below.

2. **GPG signing key**
   ```bash
   gpg --full-generate-key                      # RSA 4096, with a passphrase
   gpg --list-secret-keys --keyid-format=long   # note the key ID
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
   gpg --armor --export-secret-keys <KEY_ID>    # the whole output is SIGNING_KEY
   ```
   The passphrase is `SIGNING_KEY_PASSWORD`.

3. **GitHub secrets** (*Settings > Secrets and variables > Actions*): add `MAVEN_CENTRAL_USERNAME`,
   `MAVEN_CENTRAL_PASSWORD`, `SIGNING_KEY` and `SIGNING_KEY_PASSWORD`.

## Releasing

1. Set `VERSION_NAME` in `gradle.properties` (for example `1.0.1`) and update the README.
2. Merge to `master`, then tag that commit and push the tag:
   ```bash
   git tag v1.0.1
   git push origin v1.0.1
   ```
3. The **Publish to Maven Central** workflow (`.github/workflows/publish.yml`) checks that the tag
   matches `VERSION_NAME`, runs the unit tests, signs and uploads the library, and releases it.
   It is usually available on Maven Central about 30 minutes later.

## Publishing from your own machine

Put the credentials in `~/.gradle/gradle.properties` (your home directory, not this repo):

```properties
mavenCentralUsername=<token username>
mavenCentralPassword=<token password>
signing.keyId=<last 8 characters of the key ID>
signing.password=<key passphrase>
signing.secretKeyRingFile=<path to an exported secring.gpg>
```

or use `signingInMemoryKey` / `signingInMemoryKeyPassword` with the ASCII-armored key. Then run:

```bash
./gradlew :richlinkpreview:publishAndReleaseToMavenCentral --no-configuration-cache
```

## Testing locally

```bash
./gradlew :richlinkpreview:publishToMavenLocal
```

This writes the AAR, sources, Javadoc, Gradle module file and POM to
`~/.m2/repository/io/github/sb-android-dev/richlinkpreview/<version>/`. Signing is skipped when no
key is configured, so this works without any credentials. Maven Central itself rejects unsigned uploads.
