import com.diffplug.gradle.spotless.FormatExtension

plugins {
  id("dependency-update")
  alias(libs.plugins.kotlin) apply false
  alias(libs.plugins.kotlinx.serialization) apply false
  alias(libs.plugins.spotless)
}

installGitHooks()

allprojects {
  group = "moe.sdl.commons"
  version = "0.3.0"
  repositories {
    mavenCentral()
    google()
  }
}

spotless {
  fun FormatExtension.excludes() {
    targetExclude("**/build/", "**/generated/", "**/resources/")
  }

  fun FormatExtension.common() {
    trimTrailingWhitespace()
    lineEndings = com.diffplug.spotless.LineEnding.UNIX
    endWithNewline()
  }

  val ktlintConfig = mapOf(
    "ij_kotlin_allow_trailing_comma" to "true",
    "ij_kotlin_allow_trailing_comma_on_call_site" to "true",
    "trailing-comma-on-declaration-site" to "true",
    "trailing-comma-on-call-site" to "true",
    "ktlint_standard_no-wildcard-imports" to "disabled",
    "ktlint_standard_filename" to "disabled",
    "ktlint_disabled_import-ordering" to "disabled",
  )

  kotlin {
    target("**/*.kt")
    leadingTabsToSpaces(2)
    excludes()
    common()
    ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintConfig)
  }

  kotlinGradle {
    target("**/*.gradle.kts")
    leadingTabsToSpaces(2)
    excludes()
    common()
    ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintConfig)
  }
}
