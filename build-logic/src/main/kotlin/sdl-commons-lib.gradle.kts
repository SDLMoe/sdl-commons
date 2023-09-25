import org.gradle.accessors.dm.LibrariesForLibs

plugins {
  kotlin("jvm")
  id("app.cash.licensee")
  `java-library`
  `maven-publish`
  signing
}

val libs = the<LibrariesForLibs>()

dependencies {
  testImplementation(libs.bundles.test.junit)
  testImplementation(project(":logger-logback"))
}

configureLicensee()

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

tasks.test {
  useJUnitPlatform()
  systemProperty(
    "logback.configurationFile",
    rootProject.rootDir
      .resolve("resources")
      .resolve("logback-test.xml")
      .canonicalPath,
  )
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

publishing {
  publications {
    create<MavenPublication>("mavenKotlin") {
      artifactId = if (project.name.startsWith("sdl-commons-")) {
        project.name
      } else {
        "sdl-commons-" + project.name
      }

      from(components["java"])
      versionMapping {
        usage("java-api") {
          fromResolutionOf("runtimeClasspath")
        }
        usage("java-runtime") {
          fromResolutionResult()
        }
      }

      pom {
        name.set(project.name)
        description.set("A server software implementation for a certain anime game, and avoid sorapointa")
        url.set("https://github.com/SDLMoe/sdl-commons")

        licenses {
          license {
            name.set("Apache License, Version 2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
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
    }
  }
}

signing {
  sign(publishing.publications["mavenKotlin"])
}

tasks.javadoc {
  exclude("org.sorapointa.proto")

  if (JavaVersion.current().isJava9Compatible) {
    (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
  }
}
