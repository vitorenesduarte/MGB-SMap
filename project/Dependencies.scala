import sbt._
import Keys._

object Dependencies {

  val commonDependencies: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % "3.0.1",
    "org.imdea.vcd" % "vcdjavaclient" % "0.1",
    "com.google.protobuf" % "protobuf-java" % "3.4.0",
    "org.kohsuke.args4j" % "args4j-maven-plugin" % "2.32",
    "org.apache.zookeeper"% "zookeeper" %"3.4.10",

    "com.trueaccord.scalapb" %% "scalapb-runtime"      % com.trueaccord.scalapb.compiler.Version.scalapbVersion % "protobuf",
    // for gRPC
    "io.grpc"                %  "grpc-netty"           % "1.8.0",
    "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % com.trueaccord.scalapb.compiler.Version.scalapbVersion,
    // for JSON conversion
    "com.trueaccord.scalapb" %% "scalapb-json4s"       % "0.3.0"
  )

}
