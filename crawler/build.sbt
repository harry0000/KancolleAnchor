name := "PlaceCrawler"

version := "1.0.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.0",
  "com.github.tototoshi" %% "scala-csv" % "1.3.2",
  "io.spray" %%  "spray-json" % "1.3.2",
  "org.seleniumhq.selenium" % "selenium-java" % "2.35.0",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-language:implicitConversions")

mainClass in (Compile, run) := Some("com.harry0000.kancolle.ac.Main")

fork in run := true

val driver =
  if (System.getProperty("os.name").toLowerCase.contains("windows"))
    "chromedriver.exe"
  else
    "chromedriver"

javaOptions in run += "-Dwebdriver.chrome.driver=./driver/" + driver
