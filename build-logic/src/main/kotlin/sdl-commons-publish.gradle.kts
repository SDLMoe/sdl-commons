import com.vanniktech.maven.publish.SonatypeHost

plugins {
  id("com.vanniktech.maven.publish")
}

mavenPublishing {
  publishToMavenCentral(SonatypeHost.S01)
  signAllPublications()

  val projectName = if (project.name.startsWith("commons-")) {
    project.name
  } else {
    "commons-" + project.name
  }

  pom {
    name.set(projectName)
    description.set("SDL's common libraries for Kotlin development.")
    url.set("https://github.com/SDLMoe/sdl-commons")

    licenses {
      license {
        name.set("Apache License, Version 2.0")
        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
      }
    }

    developers {
      developer {
        name = "Colerar"
      }
    }

    scm {
      connection.set("scm:git:git://github.com/SDLMoe/sdl-commons.git")
      developerConnection.set("scm:git:ssh://github.com/SDLMoe/sdl-commons.git")
      url.set("https://github.com/SDLMoe/sdl-commons")
    }
  }
}
