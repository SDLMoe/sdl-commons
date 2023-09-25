plugins {
  id("sdl-commons-lib")
  alias(libs.plugins.kotlinx.serialization)
}

dependencies {
  api(projects.sdlCommons.coroutines)
  api(libs.kotlinx.coroutines)
  implementation(libs.slf4j)
}
