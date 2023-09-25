plugins {
  id("sdl-commons-lib")
  alias(libs.plugins.kotlinx.serialization)
}

dependencies {
  implementation(libs.logback)
}
