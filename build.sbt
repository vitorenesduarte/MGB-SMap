import Dependencies._
import Keys._

enablePlugins(PackPlugin)
name := "MGB-SMap"
packMain := Map("MGB-SMap"->"org.telecomsudparis.smap.SMapServiceServer")
organization := "org.telecomsudparis.smap"
version := "0.1-SNAPSHOT"
scalaVersion := "2.12.3"

//publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

// disable using the Scala version in output paths and artifacts
crossPaths := false
scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")

/*
scalacOptions in Global ++= Seq(
  "-deprecation",
  "-unchecked",
  "-encoding", "UTF-8",
  "-feature",
  "-Xlint",
  "-Xverify",
  "-Xfuture",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-unused-import"
)
*/

//debug
//javaOptions in run ++= Seq("-Xms512M", "-Xmx512M", "-Xss1M", "-XX:+CMSClassUnloadingEnabled", "-XX:MaxPermSize=256M", "-XX:+PrintGCDetails", "-XX:+PrintGCTimeStamps", "-XX:-HeapDumpOnOutOfMemoryError", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=3000")

logLevel := Level.Error

resolvers += Resolver.mavenLocal

//For ScalaPB:
PB.targets in Compile := Seq(
   //scalapb.gen() -> (sourceManaged in Compile).value
   PB.gens.java -> (sourceManaged in Compile).value,
   scalapb.gen(javaConversions=true) -> (sourceManaged in Compile).value
)

libraryDependencies ++= Dependencies.commonDependencies
