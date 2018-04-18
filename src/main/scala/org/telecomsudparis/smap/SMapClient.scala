package org.telecomsudparis.smap

import java.lang.Thread

import com.google.protobuf.{ByteString => ProtobufByteString}
import scala.concurrent._
import scala.concurrent.duration._
import java.util.logging.Logger

/**
  * Producer Class
  */
class SMapClient(var verbose: Boolean, mapServer: SMapServer) extends nl.grons.metrics4.scala.DefaultInstrumented  {
  val logger: Logger = Logger.getLogger(classOf[SMapServiceClient].getName)

  var clientId: String = SMapClient.uuid()
  //var pendings = scala.collection.mutable.ListBuffer.empty[CallerId]
  //var pendingWrite: Option[CallerId] = None


  if(verbose) {
    logger.info(s"SMapClient Id: $clientId")
  }

  private[this] val internalWait = metrics.timer("internalWait")

  def sendCommand(operation: MapCommand): ResultsCollection = {
    //var st = System.currentTimeMillis()

    val opUuid = OperationUniqueId(operation.operationUuid)
    val isRead: Boolean = operation.operationType.isScan || operation.operationType.isGet
    val callerUuid = CallerId(operation.callerId)

    //To achieve sequential consistency, reads must wait pending writes.
    if(isRead){
      waitPendings(callerUuid)
    } else {
      val writePromise = Promise[Boolean]()
      mapServer.pendingMap += (callerUuid ->  writePromise)
    }

    val pro = PromiseResults(Promise[ResultsCollection]())
    val fut = pro.pResult.future

    mapServer.promiseMap += (opUuid -> pro)

    if(mapServer.localReads && isRead) {
     mapServer.localReadsQueue.put(operation)
    } else {
      val msgMGB = SMapClient.generateMsg(operation)
      mapServer.queue.put(msgMGB)
    }

    val response = Await.result(fut, Duration.Inf)
    mapServer.promiseMap -= opUuid

    /*
    var end = System.currentTimeMillis()
    var ft = end - st
    logger.info(s"Wait: ${ft} Thread: ${Thread.currentThread().getName}")
    */

    response
  }

  def waitPendings(pending:CallerId): Unit = {
    val writeFuture = mapServer.pendingMap(pending).future
    Await.result(writeFuture, Duration.Inf)
    mapServer.pendingMap -= pending
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

  def generateMsg(toMGB: MapCommand): Message = synchronized {
    val mgbHash =
      if(toMGB.operationType.isScan || toMGB.operationType.isGet) {
        //hash == new byte[]{0} in case of read ops
        //NOTE: "And we only send collects of white color messages to a majority"
        ProtobufByteString.copyFrom(Array[Byte](0))
      } else {
        //hash == key parameter in pure write operations
        ProtobufByteString.copyFrom(toMGB.getItem.key.getBytes())
      }
    val mgbData = toMGB.toByteString
    val msg: Message = Message.newBuilder().setHash(mgbHash).setData(mgbData).build()
    msg
  }

  //TODO: Remove .toString
  def uuid(): String = synchronized {
    java.util.UUID.randomUUID.toString
  }
}