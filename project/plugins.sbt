resolvers += "Databricks Maven Proxy" at "https://maven-proxy.cloud.databricks.com"

// Plugin versions chosen so Maven Central coordinates resolve through the Databricks proxy.
// Older ivy-only sbt-plugin-releases artifacts (sbt-header 5.2.0, sbt-assembly 0.15.0, sbt-scoverage 1.6.0) 404 there.

addSbtPlugin("com.github.sbt"    % "sbt-ci-release"    % "1.11.1")
addSbtPlugin("com.eed3si9n"      % "sbt-projectmatrix" % "0.9.2")
addSbtPlugin("org.scoverage"     % "sbt-scoverage"     % "2.0.11")
addSbtPlugin("com.github.sbt"    % "sbt-header"        % "5.11.0")
addSbtPlugin("com.eed3si9n"      % "sbt-assembly"      % "2.3.1")
addSbtPlugin("io.get-coursier"   % "sbt-shading"       % "2.1.5")

addDependencyTreePlugin

// sbt-jacoco - workaround related dependencies required to download
val ow2Version = "9.5"
val jacocoVersion = "0.8.11-absa.1"
val sbtJacocoVersion = "3.4.1-absa.4"
val scalaArmVersion = "2.0"

def jacocoUrl(artifactName: String): String = s"https://github.com/AbsaOSS/jacoco/releases/download/$jacocoVersion/org.jacoco.$artifactName-$jacocoVersion.jar"
def ow2Url(artifactName: String): String = s"https://maven-proxy.cloud.databricks.com/org/ow2/asm/$artifactName/$ow2Version/$artifactName-$ow2Version.jar"
def armUrl(scalaMajor: String): String = s"https://maven-proxy.cloud.databricks.com/com/jsuereth/scala-arm_$scalaMajor/$scalaArmVersion/scala-arm_$scalaMajor-$scalaArmVersion.jar"

addSbtPlugin("com.jsuereth" %% "scala-arm" % scalaArmVersion from armUrl("2.11"))
addSbtPlugin("com.jsuereth" %% "scala-arm" % scalaArmVersion from armUrl("2.12"))

addSbtPlugin("za.co.absa.jacoco" % "report" % jacocoVersion from jacocoUrl("report"))
addSbtPlugin("za.co.absa.jacoco" % "core" % jacocoVersion from jacocoUrl("core"))
addSbtPlugin("za.co.absa.jacoco" % "agent" % jacocoVersion from jacocoUrl("agent"))
addSbtPlugin("org.ow2.asm" % "asm" % ow2Version from ow2Url("asm"))
addSbtPlugin("org.ow2.asm" % "asm-commons" % ow2Version from ow2Url("asm-commons"))
addSbtPlugin("org.ow2.asm" % "asm-tree" % ow2Version from ow2Url("asm-tree"))

addSbtPlugin("za.co.absa.sbt" % "sbt-jacoco" % sbtJacocoVersion from s"https://github.com/AbsaOSS/sbt-jacoco/releases/download/$sbtJacocoVersion/sbt-jacoco-$sbtJacocoVersion.jar")
