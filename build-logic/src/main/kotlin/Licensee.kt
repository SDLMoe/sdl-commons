import app.cash.licensee.LicenseeExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

fun Project.configureLicensee() = configure<LicenseeExtension> {
  val allowedLicenses = arrayOf(
    "Apache-2.0",
    "MIT",
    "ISC",
    "BSD-2-Clause",
    "BSD-3-Clause",
    "CC0-1.0",
    "EPL-1.0",
  )
  allowedLicenses.forEach { allow(it) }
}
