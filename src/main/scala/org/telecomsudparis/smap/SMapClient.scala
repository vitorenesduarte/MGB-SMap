package org.telecomsudparis.smap

import com.google.protobuf.{ByteString => ProtobufByteString}
import scala.concurrent._
import scala.concurrent.duration._

/**
  * Producer Class
  */
class SMapClient(var verbose: Boolean, mapServer: SMapServer) {
  var clientId: String = SMapClient.uuid()
  var pendings = scala.collection.mutable.ListBuffer.empty[CallerId]

  if(verbose) {
    println(s"SMapClient Id: $clientId")
  }

  def sendCommand(operation: MapCommand): ResultsCollection = {
    val opUuid = OperationUniqueId(operation.operationUuid)
    val isRead: Boolean = operation.operationType.isScan || operation.operationType.isGet

    /*
    //FIXME: Proper define calledId in the YCSB side
    //To achieve sequential consistency, reads must wait pending writes.
    if(isRead){
      //waitPendings()
    } else {
      val writePromise = Promise[Boolean]()
      val callerUuid = CallerId(operation.callerId)
      pendings += callerUuid
      mapServer.pendingMap += (callerUuid ->  writePromise)
    }
    */

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
    response
  }

  def waitPendings(): Unit = {
    if (pendings.nonEmpty) {
      for (pending <- pendings) {
        val writeFuture = mapServer.pendingMap(pending).future
        Await.result(writeFuture, Duration.Inf)
        mapServer.pendingMap -= pending
      }
      pendings.clear()
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