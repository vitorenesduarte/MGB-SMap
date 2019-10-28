package org.telecomsudparis.smap

import java.lang.Thread
import java.util.concurrent.locks.ReentrantReadWriteLock

import com.google.protobuf.{ByteString => ProtobufByteString}

import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.mutable.{Map => MMap}
import java.util.logging.Logger
import org.telecomsudparis.smap.SMapServiceServer.Instrumented
import com.google.common.primitives.Ints
import com.google.protobuf.ByteString

import org.imdea.vcd.Generator.BLACK

/**
  * Producer Class
  */
class SMapClient(var verbose: Boolean, mapServer: SMapServer) extends Instrumented {
  val logger: Logger = Logger.getLogger(classOf[SMapServiceClient].getName)

  var clientId: String = SMapClient.uuid()

  if(verbose) {
    logger.info(s"SMapClient Id: $clientId")
  }

  private[this] val waitPendingsTime = metrics.timer("waitPendingsTime")
  private[this] val timeSingleRead = metrics.timer("timeSingleRead")
  private[this] val timeSingleScan = metrics.timer("timeSingleScan")
  private[this] val promiseMapTimeWrite = metrics.timer("promiseMapWrite")

  def sendCommand(operation: MapCommand): ResultsCollection =  {
    var response = new ResultsCollection()

    try {
      val opUuid = OperationUniqueId(operation.operationUuid)
      val isRead: Boolean = operation.operationType.isScan || operation.operationType.isGet
      val callerUuid = CallerId(operation.callerId)

      if (verbose) {
        logger.info(opUuid + " -> START")
      }

      val pro = PromiseResults(Promise[ResultsCollection]())
      val fut = pro.pResult.future

      mapServer.promiseMap.put(opUuid, pro)

      val msgMGB = SMapClient.generateMsg(operation,clientId)

      if(verbose) logger.fine("queuing " + msgMGB)

      mapServer.queue.put(msgMGB)

      response = promiseMapTimeWrite.time(Await.result(fut, Duration.Inf))

      mapServer.promiseMap.remove(opUuid)
    } catch {
      case e: Exception => e.printStackTrace()
    }

    response
  }
}

import org.imdea.vcd.pb.Proto._
object SMapClient {
  /*
  def generateMsgSet[B](h: String, d: B): MessageSet = synchronized {
    val hash1 =
      if(!d.isInstanceOf[ReadOp]) {
        //hash == key parameter in pure write operations
        ProtobufByteString.copyFrom(Serialization.serialise(h))
      } else {
        //hash == new byte[]{0} in case of read ops
        //NOTE: "And we only send collects of white color messages to a majority"
        ProtobufByteString.copyFrom(Array[Byte](0))
      }

    val data1 = ProtobufByteString.copyFrom(Serialization.serialise(d))

    val builder = MessageSet.newBuilder()
    val msg = Message.newBuilder().setHash(hash1).setData(data1).build()
    builder.addMessages(msg)
    builder.setStatus(MessageSet.Status.START)

    builder.build()
  }
  */
  def generateMsg(toMGB: MapCommand, clientID: String): Message = synchronized {
    val mgbHash = ProtobufByteString.copyFrom(toMGB.getItem.key.getBytes())
    val mgbData = toMGB.toByteString
    val id = ProtobufByteString.copyFromUtf8(clientID)
    val builder = Message.newBuilder().setFrom(id).setData(mgbData).addHashes(mgbHash).setPure(toMGB.operationType.isGet)
    if (toMGB.operationType.isScan) {
      builder.addHashes(BLACK)
    }
    builder.build()
  }

  //TODO: Remove .toString
  def uuid(): String = synchronized {
    java.util.UUID.randomUUID.toString
  }
}
