import org.gradle.accessors.dm.LibrariesForLibs

plugins {
  kotlin("jvm")
  id("sdl-commons-publish")
  id("app.cash.licensee")
  `java-library`
}

val libs = the<LibrariesForLibs>()

dependencies {
  testImplementation(libs.bundles.test.junit)
  testImplementation(libs.logback)
  testImplementation(project(":logger-logback"))
}

configureLicensee()

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
  }
}

tasks.test {
  useJUnitPlatform()
  systemProperty(
    "logback.configurationFile",
    rootProject.rootDir
      .resolve("resources")
      .resolve("logback-test.xml")
      .canonicalPath,
  )
}
