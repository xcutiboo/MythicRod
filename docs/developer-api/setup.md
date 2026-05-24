# Setup

The cleanest way to depend on MythicRod is through JitPack, which
builds the `mythicrod-api` artifact straight from the public GitHub
tag. No file copies, no jar wrangling.

## Gradle (Kotlin DSL) - JitPack

```kotlin
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.64-stable")
    compileOnly("com.github.xcutiboo.MythicRod:mythicrod-api:v2026.1.0")
}
```

The first build for a new MythicRod tag triggers a one-time JitPack
build (1-5 minutes). Subsequent consumers get the cached artifact.

## Gradle (Kotlin DSL) - jar drop

If you cannot reach JitPack from your build host, drop the released
MythicRod jar into your project's `libs/` folder and keep it
`compileOnly`. Never shade or relocate it.

```kotlin
dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.64-stable")
    compileOnly(files("libs/MythicRod-Paper-2026.1.0.jar"))
}
```

## Gradle (Groovy)

```groovy
dependencies {
    compileOnly 'io.papermc.paper:paper-api:26.1.2.build.64-stable'
    compileOnly files('libs/MythicRod-Paper-2026.1.0.jar')
}
```

## Maven

```xml
<dependency>
  <groupId>io.papermc.paper</groupId>
  <artifactId>paper-api</artifactId>
  <version>26.1.2.build.64-stable</version>
  <scope>provided</scope>
</dependency>
<dependency>
  <groupId>io.xcutiboo</groupId>
  <artifactId>mythicrod-paper</artifactId>
  <version>2026.1.0</version>
  <scope>system</scope>
  <systemPath>${project.basedir}/libs/MythicRod-Paper-2026.1.0.jar</systemPath>
</dependency>
```

## paper-plugin.yml dependency

Declare MythicRod as an after-load optional dependency so the API
service is registered before your plugin reaches for it:

```yaml
name: YourPlugin
main: example.YourPlugin
api-version: '1.21'
dependencies:
  server:
    MythicRod:
      load: AFTER
      required: false
```

Handle the missing-service case cleanly when MythicRod is optional.

## Plugin version compatibility

| Plugin version | Paper API | Java | Folia |
| --- | --- | --- | --- |
| `2026.1.x` | `26.1.x` (1.21.11) | 25 | supported |

Patch releases never break the API. Minor releases add methods with
default implementations only. Year roll-overs may rename or remove API
only when the changelog calls it out explicitly.

[← Developer API](../developer-api.md)
