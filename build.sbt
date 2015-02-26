name := "Json4s-reactivemongo-compatibility"

version := "1.0"

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
  "org.json4s"        %% "json4s-native"        % "3.2.10",
  "org.json4s"        %% "json4s-jackson"       % "3.2.10",
  "org.reactivemongo" %% "reactivemongo"        % "0.10.5.0.akka23"
)