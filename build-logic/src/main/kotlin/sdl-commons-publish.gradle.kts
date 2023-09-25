plugins {
  `maven-publish`
  signing
}

val secretPropsFile: File = project.rootProject.file("local.properties")
if (secretPropsFile.exists()) {
  val props = getRootProjectLocalProps()
  props.forEach { (t, u) -> ext[t] = u }
} else {
  ext["signing.keyId"] = System.getenv("SIGNING_KEY_ID")
  ext["signing.password"] = System.getenv("SIGNING_PASSWORD")
  ext["signing.secretKeyRingFile"] = System.getenv("SIGNING_SECRET_KEY_RING_FILE")
  ext["ossrhUsername"] = System.getenv("OSSRH_USERNAME")
  ext["ossrhPassword"] = System.getenv("OSSRH_PASSWORD")
}

fun SoftwareComponentContainer.getOrNull(key: String): SoftwareComponent? {
  return try {
    getByName(key)
  } catch (e: UnknownDomainObjectException) {
    // ignore
    null
  }
}

publishing {
  publications {
    create<MavenPublication>("mavenKotlin") {
      val projectName = if (project.name.startsWith("commons-")) {
        project.name
      } else {
        "commons-" + project.name
      }
      artifactId = projectName

      val javaComponent = components.getOrNull("java")
      val javaPlatformComponent = components.getOrNull("javaPlatform")
      from(
        javaComponent ?: javaPlatformComponent
          ?: error("No `java` or `javaPlatform` components for publishing"),
      )
      versionMapping {
        if (javaComponent != null) {
          usage("java-api") {
            fromResolutionOf("runtimeClasspath")
          }
          usage("java-runtime") {
            fromResolutionResult()
          }
        }
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

    repositories {
      maven {
        name = "sonatype"
        val releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
        val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
        val url = if (version.toString().contains("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
        setUrl(url)
        credentials {
          username = getExtraString("ossrhUsername")
          password = getExtraString("ossrhPassword")
        }
      }
      maven {
        name = "test"
        url = rootProject.layout.buildDirectory.dir("testRepo").get().asFile.toURI()
      }
    }
  }
}

signing {
  sign(publishing.publications["mavenKotlin"])
}
