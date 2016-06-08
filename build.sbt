name := "Actorbase-Server"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.4",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.4",
  "com.typesafe.akka" %% "akka-cluster-sharding" % "2.4.4",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.4-SNAPSHOT",
  "com.typesafe.akka" % "akka-cluster-metrics_2.11" % "2.4.6",
  "com.typesafe" % "config" % "1.2.1",
  "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test",
  "io.spray" %% "spray-can" % "1.3.3",
  "io.spray" %% "spray-routing" % "1.3.3",
  "io.spray" %% "spray-json" % "1.3.2",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "com.github.t3hnar" % "scala-bcrypt_2.10" % "2.6",
  "org.scala-lang.modules" %% "scala-pickling" % "0.10.1",
  "com.github.romix.akka" %% "akka-kryo-serialization" % "0.4.1")

// addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.5")

// addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.1.0")
