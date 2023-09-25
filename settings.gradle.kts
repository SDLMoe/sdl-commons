enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "sdl-commons"

include(
  ":all",
  ":bom",
  ":coroutines",
  ":event",
  ":logger-logback",
)

includeBuild("build-logic")
