name := "data-mining-PAMAP2"
lazy val akkaVersion = "2.4.0"
scalaVersion := "2.11.7"

enablePlugins(JavaAppPackaging)

maintainer := "https://hub.docker.com/u/aabbcc1241/"
packageSummary := s"Akka ${version.value} Server"

resolvers ++= Seq(
  "RethinkScala Repository" at "http://kclay.github.io/releases",
  "RethinkScala Repository" at "http://kclay.github.io/snapshots"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.github.scopt" %% "scopt" % "3.2.0",
  "org.slf4j" % "slf4j-simple" % "1.6.2",
  "com.rethinkdb" % "rethinkdb-driver" % "2.2-beta-1",
  "org.openjdk.jmh" % "jmh-core" % "0.1"
)
//  "com.rethinkdb" % "rethink-java-driver" % "0.3"

// Create custom run tasks to start a seed and a cluster node
// http://www.scala-sbt.org/0.13.0/docs/faq.html#how-can-i-create-a-custom-run-task-in-addition-to-run
lazy val runLocalSeed = taskKey[Unit]("Start the seed node on local network")
fullRunTask(runLocalSeed, Compile, "hk.edu.polyu.datamining.pamap2.Main", "--seed --local")
fork in runLocalSeed := true

javaOptions in runLocalSeed ++= Seq(
)

lazy val runPublicSeed = taskKey[Unit]("Start the seed node on public network")
fullRunTask(runPublicSeed, Compile, "hk.edu.polyu.datamining.pamap2.Main", "--seed --public")
fork in runPublicSeed := true

javaOptions in runPublicSeed ++= Seq(
)

lazy val runVMCompute = taskKey[Unit]("Start a compute node")
fullRunTask(runVMCompute, Compile, "hk.edu.polyu.datamining.pamap2.Main", "--compute --vm")
fork in runVMCompute := true

javaOptions in runVMCompute ++= Seq(
  "-XX:+AggressiveHeap"
)

lazy val runCompute = taskKey[Unit]("Start a compute node")
fullRunTask(runCompute, Compile, "hk.edu.polyu.datamining.pamap2.Main", "--compute")
fork in runCompute := true

javaOptions in runCompute ++= Seq(
  "-XX:+AggressiveHeap"
)

lazy val runUI = taskKey[Unit]("Start a UI node")
fullRunTask(runUI, Compile, "hk.edu.polyu.datamining.pamap2.Main", "--ui")
fork in runUI := true

javaOptions in runUI ++= Seq(
  "-XX:+AggressiveHeap"
)

lazy val runHost = taskKey[Unit]("Start a host node")
fullRunTask(runUI, Compile, "hk.edu.polyu.datamining.pamap2.HostMain")
fork in runHost := true

//javaOptions ++= Seq(
//  "-XX:+AggressiveHeap"
//)