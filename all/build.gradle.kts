plugins {
  `java-library`
  id("sdl-commons-publish")
}

dependencies {
  rootProject.subprojects
    .filter { it.name != project.name && it.name != projects.bom.name }
    .forEach {
      api(it)
    }

  api(platform(projects.bom))
}
