plugins {
  `java-platform`
  id("sdl-commons-publish")
}

javaPlatform {
  allowDependencies()
}

dependencies {
  rootProject.subprojects
    .filter { it.name != project.name && it.name != projects.bom.name }
    .forEach {
      api(it)
    }

  api(platform(projects.bom))
}
