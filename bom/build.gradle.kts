plugins {
  `java-platform`
  id("sdl-commons-publish")
}

dependencies {
  constraints {
    rootProject.subprojects
      .filter { it.name != project.name }
      .forEach {
        api(it)
      }
  }
}
