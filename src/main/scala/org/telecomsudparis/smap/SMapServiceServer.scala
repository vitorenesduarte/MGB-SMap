package org.telecomsudparis.smap

import java.util.logging.Logger
import scala.util.Properties
import io.grpc.{Server, ServerBuilder}

class SMapServiceServer(server: Server) {
  val logger: Logger = Logger.getLogger(classOf[SMapServiceServer].getName)

  def start(): Unit = {
    server.start()
    logger.info(s"MGB-SMap Server started, listening on ${server.getPort}")
    sys.addShutdownHook {
      // Use stderr here since the logger may has been reset by its JVM shutdown hook.
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      stop()
      System.err.println("*** server shut down")
    }
    ()
  }
  def stop(): Unit = {
    server.shutdown()
  }

  /**
    * Await termination on the main thread since the grpc library uses daemon threads.
    */
  def blockUntilShutdown(): Unit = {
    server.awaitTermination()
  }

}

object SMapServiceServer extends App {
  val parser = new scopt.OptionParser[ServerConfig]("SMapServiceServer") {
    head("SMapServiceServer", "0.1-SNAPSHOT")
    opt[Int]("serverPort").abbr("sp").action( (x, c) =>
      c.copy(serverPort = x) ).text("Server Port is an integer property. Default: 8980")

    opt[Boolean]("localReads").abbr("lr").action( (x, c) =>
      c.copy(lReads = x) ).text("Local Reads is an boolean property. Default: true")

    opt[Boolean]("verbosity").abbr("vb").action( (x, c) =>
      c.copy(verbosity = x) ).text("Verbosity is an boolean property. Default: true")

    opt[String]("zookeeperHost").abbr("zkh").action( (x, c) =>
      c.copy(zkHost = x) ).text("Default is the string: 127.0.0.1")

    opt[String]("zookeeperPort").abbr("zkp").action( (x, c) =>
      c.copy(zkPort = x) ).text("Default is the string: 2181")

    opt[String]("timeStamp").abbr("ts").action( (x, c) =>
      c.copy(timeStamp = x) ).text("Default is the string: undefined")

    help("help").text("prints this usage text")
  }

  // parser.parse returns Option[C]
  parser.parse(args, ServerConfig()) match {
    case Some(config) =>
      var serverSMap = new SMapServer(localReads = config.lReads,
        verbose = config.verbosity,
        Array("-zk " + config.zkHost + ":" + config.zkPort))

      var clientSMap = new SMapClient(verbose = config.verbosity,
        mapServer = serverSMap)

      val server = new SMapServiceServer(
        ServerBuilder
          .forPort(config.serverPort)
          .addService(
            smapGrpc.bindService(
              new SMapService(serverSMap, clientSMap),
              scala.concurrent.ExecutionContext.global
            )
          )
          .build()
      )
      server.start()
      serverSMap.serverInit()
      server.blockUntilShutdown()
    case None =>
    // arguments are bad, error message will have been displayed
  }


}
