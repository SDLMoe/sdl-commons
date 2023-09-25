plugins {
  id("sdl-commons-lib")
  alias(libs.plugins.kotlinx.serialization)
}

dependencies {
  api(libs.kotlinx.coroutines)
  implementation(libs.slf4j)
}
