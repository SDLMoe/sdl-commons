plugins {
  `kotlin-dsl`
}

repositories {
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  // workaround, see: https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
  implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
  implementation(libs.gradle.catalog.update)
  implementation(libs.gradle.kotlin)
  implementation(libs.gradle.publish)
  implementation(libs.gradle.versions)
  implementation(libs.gradle.licensee)
}
