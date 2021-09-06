val scala3Version = "3.0.2"
val zioVersion = "2.0.0-M2"

lazy val compileDependencies = Seq(
  "dev.zio" %% "zio" % zioVersion,
) map (_ % Compile)

lazy val testDependencies = Seq(
  "dev.zio" %% "zio-test"     % zioVersion,
  "dev.zio" %% "zio-test-sbt" % zioVersion,
) map (_ % Test)

lazy val settings = Seq(
  name := "zio-playground",
  version := "0.1.0",
  scalaVersion := scala3Version,
  libraryDependencies ++= compileDependencies ++ testDependencies,
  testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
)


lazy val root = (project in file("."))
  .settings(settings)
