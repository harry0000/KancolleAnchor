name := "KanColleAnchor"

version := "0.1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.scalikejdbc" %% "scalikejdbc"                  % "2.4.1",
  "org.scalikejdbc" %% "scalikejdbc-config"           % "2.4.1",
  "org.scalikejdbc" %% "scalikejdbc-play-initializer" % "2.5.1",
  "org.flywaydb"    %% "flyway-play"                  % "3.0.0",
  "ch.qos.logback"  %  "logback-classic"              % "1.1.7",
  "mysql"           %  "mysql-connector-java"         % "5.1.39",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1"   % "test",
  "com.h2database"         %  "h2"                 % "1.4.191" /*% "test"*/
)

scalikejdbcSettings // http://scalikejdbc.org/documentation/setup.html

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-language:implicitConversions")
