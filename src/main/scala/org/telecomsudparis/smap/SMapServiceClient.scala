package org.telecomsudparis.smap
import java.util.concurrent.{CountDownLatch, TimeUnit}
import java.util.logging.Logger

import io.grpc.{ManagedChannelBuilder, Status}

import scala.concurrent.Await
import scala.concurrent.duration._

import scala.io.StdIn


class SMapServiceClient (host: String, port: Int){
  val logger: Logger = Logger.getLogger(classOf[SMapServiceClient].getName)

  val channel =
    ManagedChannelBuilder
      .forAddress(host, port)
      .usePlaintext(true)
      .build()

  val blockingStub = smapGrpc.blockingStub(channel)

  def shutdown(): Unit = channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)

  import io.grpc.StatusRuntimeException

  /**
    * Blocking unary call.  Calls executeCmd and returns the response.
    */
  def sendCmd(request: MapCommand): Unit = {
    try {
      val result = blockingStub.executeCmd(request)
    } catch {
      case e: StatusRuntimeException =>
        logger.warning(s"RPC failed:${e.getStatus}")
    }
  }
}

object SMapServiceClient extends App {
  val logger = Logger.getLogger(getClass.getName)

  //TODO: Get parameters from properties
  val client = new SMapServiceClient("localhost", 8980)
  /*
  var stop = false

  try {
    while (!stop) {
      client.sendCmd()
    }
  } finally client.shutdown()
  */
}
