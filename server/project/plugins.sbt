// The Typesafe repository
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/maven-releases/"

libraryDependencies += "com.h2database" % "h2" % "1.4.191"

addSbtPlugin("org.scalikejdbc"   % "scalikejdbc-mapper-generator" % "2.4.0")

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.4")
