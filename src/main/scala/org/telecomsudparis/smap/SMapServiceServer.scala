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
  //TODO: get server parameters from properties

  //Zookeeper host & port
  /*
  val zhost: String = System.getProperties.getProperty("zhost")
  val zport: String = System.getProperties.getProperty("zport")
  val javaClientConfig = Array("-zk=" + zhost + ":" + zport)

  val localReads: Boolean = System.getProperties.getProperty("lread").toBoolean
  val verbose: Boolean = System.getProperties.getProperty("verbose").toBoolean

  //var serviceServer = new SMapServer(localReads, verbose, javaClientConfig)
  */
  var serviceServer = new SMapServer(localReads = true, verbose = true, Array(""))
  var serviceClient = new SMapClient(verbose = true, mapServer = serviceServer)

  val server = new SMapServiceServer(
    ServerBuilder
      .forPort(8980)
      .addService(
        smapGrpc.bindService(
          new SMapService(serviceServer, serviceClient),
          scala.concurrent.ExecutionContext.global
        )
      )
      .build()
  )
  server.start()
  serviceServer.serverInit()
  server.blockUntilShutdown()
}
