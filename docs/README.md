# sdl-commons

[![Kotlin](https://img.shields.io/badge/kotlin-2.1.10-%237F52FF.svg?style=flat-square&logo=kotlin&logoColor=white)](http://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/Gradle-8.13-02303A.svg?style=flat-square&logo=Gradle&logoColor=white)](https://gradle.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square)](http://www.apache.org/licenses/LICENSE-2.0)
[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/SDLMoe/sdl-commons/ci.yml?event=push&style=flat-square&logo=github)](https://github.com/SDLMoe/sdl-commons/actions)

SDL's common libraries for Kotlin development.

This library requires Java 21+ and Kotlin 2.1+.

## Usage

[![Maven Central](https://img.shields.io/maven-central/v/moe.sdl.commons/commons-all.svg?style=flat-square)](https://central.sonatype.com/search?q=g%3Amoe.sdl.commons&smo=true)

Edit `build.gradle.kts` like so, replace `<latest-version>` with the latest version (see the badge above):

```kotlin
dependencies {
  // BOM for aligning versions to prevent potential problems
  implementation(platform("moe.sdl.commons:bom:<latest-version>"))
  //             ^ this is important, please don't forget to call
  // see: https://docs.gradle.org/current/userguide/platforms.html

  // Add a certain module, see modules below 
  implementation("moe.sdl.commons:event")

  // Or simply, add all modules as dependencies
  implementation(platform("moe.sdl.commons:all"))
}
```

## Modules

| Module           | Description                                                  |
|------------------|--------------------------------------------------------------|
| `bom`            | Special module, BOM, apply version constraints               |
| `all`            | Special module, add all modules as dependencies              |
| `coroutines`     | Extensions of `kotlinx.coroutines`, e.g. `ModuleScope`       |
| `event`          | Event System and State Machine based on `kotlinx.coroutines` |
| `logger-logback` | Extensions of Logback, e.g. `PatternLayoutNoLambda`          |

## License

Licensed under <a href="LICENSE">Apache License, Version
2.0</a>.

Unless you explicitly state otherwise, any contribution intentionally submitted
for inclusion in this library by you, as defined in the Apache-2.0 license, shall
be licensed as above, without any additional terms or conditions.
