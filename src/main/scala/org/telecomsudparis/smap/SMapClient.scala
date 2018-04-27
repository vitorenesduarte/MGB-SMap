package org.telecomsudparis.smap

import java.lang.Thread
import java.util.concurrent.locks.ReentrantReadWriteLock

import com.google.protobuf.{ByteString => ProtobufByteString}

import scala.concurrent._
import scala.concurrent.duration._
import java.util.logging.Logger

import org.telecomsudparis.smap.SMapServiceServer.Instrumented

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
  private[this] val promiseMapTimeWrite = metrics.timer("promiseMapWrite")

  def sendCommand(operation: MapCommand): ResultsCollection =  {
    var response = new ResultsCollection()

    try {
      val opUuid = OperationUniqueId(operation.operationUuid)
      val isRead: Boolean = operation.operationType.isScan || operation.operationType.isGet
      val callerUuid = CallerId(operation.callerId)

      //To achieve sequential consistency, reads must wait pending writes.
      if (isRead) {
        waitPendings(callerUuid)
      } else {
        val writePromise = Promise[Boolean]()
        if(verbose) {
          logger.fine("add pending map" + callerUuid)
        }
        mapServer.pendingMap += (callerUuid -> writePromise)
      }

      if(isRead && mapServer.localReads){
        timeSingleRead.time {
          var localReadsResult = new ResultsCollection()
          mapServer.lock.readLock().lock()
          localReadsResult = {
            if (mapServer.mapCopy isDefinedAt operation.getItem.key) {
              val getItem = Item(key = operation.getItem.key, fields = mapServer.mapCopy(operation.getItem.key).toMap)
              ResultsCollection(Seq(getItem))
            } else {
              ResultsCollection(Seq(Item(key = operation.getItem.key)))
            }
          }
          mapServer.lock.readLock().unlock()
          response = localReadsResult
        }
      } else {
        val pro = PromiseResults(Promise[ResultsCollection]())
        val fut = pro.pResult.future

        mapServer.promiseMap += (opUuid -> pro)

        val msgMGB = SMapClient.generateMsg(operation)
        mapServer.queue.put(msgMGB)

        response = promiseMapTimeWrite.time(Await.result(fut, Duration.Inf))

        mapServer.promiseMap -= opUuid
      }
    } catch {
      case e: Exception => e.printStackTrace()
    }

    response
  }

  def waitPendings(pending: CallerId): Unit = {
    mapServer.pendingMap.get(pending) match {
      case Some(promise: Promise[Boolean]) =>
        if(verbose) logger.fine("wait pending map" + pending)
        waitPendingsTime.time(Await.result(promise.future, Duration.Inf))
      case _  =>
    }
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