name := "Json4s-reactivemongo-compatibility"

version := "1.0"

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
  "org.json4s"        %% "json4s-native"        % "3.2.11",
  "org.json4s"        %% "json4s-jackson"       % "3.2.11",
  "org.reactivemongo" %% "reactivemongo"        % "0.10.5.0.akka23",
  "org.scalatest"      % "scalatest_2.11"       % "2.2.4" % "test"
)

scalacOptions in ThisBuild ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:higherKinds",
  "-language:postfixOps"
)