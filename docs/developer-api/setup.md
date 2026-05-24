# Setup

## Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.64-stable")
    compileOnly(files("libs/MythicRod-Paper-2026.1.0.jar"))
}
```

Drop the released MythicRod jar into your project's `libs/` folder.
Keep it `compileOnly`. Never shade or relocate it.

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
