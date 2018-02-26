package org.telecomsudparis.smap

import com.google.protobuf.{ByteString => ProtobufByteString}
import scala.concurrent._
import scala.concurrent.duration._

//producer
class SMapClient(var verbose: Boolean, mapServer: SMapServer) {
  var clientId: String = SMapClient.uuid()
  var threadName: String = SMapClient.uuid()
  var pendings = scala.collection.mutable.ListBuffer.empty[String]

  if(verbose) {
    println(s"Client Id: $clientId")
    println(s"Thread Name: $threadName")
  }

  def sendCommand(operation: MapCommand): ResultsCollection = {
    val msgMGB = SMapClient.generateMsg(operation)
    val opUuid = OperationUniqueId(operation.operationUuid)

    val pro = PromiseResults(Promise[ResultsCollection]())
    val fut = pro.pResult.future

    mapServer.promiseMap += (opUuid -> pro)

    if(!mapServer.localReads) {
      mapServer.queue.put(msgMGB)
    } else {
      mapServer.localReadsQueue.put(operation)
    }

    val response = Await.result(fut, Duration.Inf)
    mapServer.promiseMap -= opUuid
    response
  }


  /*
  def sendPut(key: String, data: B): resultsType = {
    val uid: String = VCDMapClient.uuid()
    val putObj = Put(key, data, uid, threadName)
    val msgSet = VCDMapClient.generateMsgSet(key, putObj)

    val pro = Promise[resultsType]()
    val fut = pro.future

    mapServer.promiseMap += (uid -> pro)
    mapServer.queue.put(msgSet)

    val response = Await.result(fut, Duration.Inf)
    mapServer.promiseMap -= uid
    response
  }
  */

  /*
  def sendInsert(key: String, data: B): resultsType = {
    // waitLastCall()
    //val sayMyName = Thread.currentThread().getName()

    val uid: String = threadName+SMapClient.uuid()
    //val insertObj = Insert(key, data, uid, sayMyName)
    val insertObj = Insert(key, data, uid, threadName)

    //val msgSet = VCDMapClient.generateMsgSet(key, insertObj)
    val msg = SMapClient.generateMsg(key, insertObj)

    val writePromise = Promise[Boolean]()
    pendings += uid
    mapServer.pendingMap += (uid ->  writePromise)

    val pro = Promise[resultsType]()
    val fut = pro.future
    mapServer.promiseMap += (uid -> pro)

    mapServer.queue.put(msg)

    val response = Await.result(fut, Duration.Inf)
    mapServer.promiseMap -= uid

    response
  }

  def sendGet(key: String): resultsType = {
    waitPendings()
    val uid: String = threadName+SMapClient.uuid()

    val getObj = Get(key, uid, threadName)

    //val msgSet = VCDMapClient.generateMsgSet(key, getObj)
    val msg = SMapClient.generateMsg(key, getObj)

    val pro = Promise[resultsType]()
    val fut = pro.future
    mapServer.promiseMap += (uid -> pro)

    if(!mapServer.localReads) {
      mapServer.queue.put(msg)
    } else {
      mapServer.localReadsQueue.put(getObj)
    }

    val response = Await.result(fut, Duration.Inf)
    mapServer.promiseMap -= uid

    response
  }

  def sendUpdate(key: String, data: B): resultsType =  {
    // waitLastCall()
    val uid: String = threadName+SMapClient.uuid()

    val updObj = Update(key, data, uid, threadName)
    //val msgSet = VCDMapClient.generateMsgSet(key, updObj)
    val msg = SMapClient.generateMsg(key, updObj)

    val writePromise = Promise[Boolean]()
    pendings += uid
    mapServer.pendingMap += (uid ->  writePromise)

    val pro = Promise[resultsType]()
    val fut = pro.future
    mapServer.promiseMap += (uid -> pro)

    mapServer.queue.put(msg)

    val response = Await.result(fut, Duration.Inf)
    mapServer.promiseMap -= uid

    response
  }

  def sendDelete(key: String): resultsType = {
    // waitLastCall()
    val uid: String = threadName+SMapClient.uuid()
    val delObj = Delete(key, uid, threadName)
    //val msgSet = VCDMapClient.generateMsgSet(key, delObj)
    val msg = SMapClient.generateMsg(key, delObj)

    val writePromise = Promise[Boolean]()
    pendings += uid
    mapServer.pendingMap += (uid ->  writePromise)

    val pro = Promise[resultsType]()
    val fut = pro.future
    mapServer.promiseMap += (uid -> pro)

    mapServer.queue.put(msg)

    val response = Await.result(fut, Duration.Inf)
    mapServer.promiseMap -= uid

    response
  }

  def sendScan(startingKey: String, recordCount: Integer): resultsType = {
    waitPendings()

    val uid: String = threadName+SMapClient.uuid()
    val scanObj = Scan(startingKey, recordCount, uid, threadName)

    //val msgSet = VCDMapClient.generateMsgSet(startingKey, scanObj)
    val msg = SMapClient.generateMsg(startingKey, scanObj)

    val pro = Promise[resultsType]()
    val fut = pro.future

    mapServer.promiseMap += (uid -> pro)

    if(!mapServer.localReads) {
      mapServer.queue.put(msg)
    } else {
    mapServer.localReadsQueue.put(scanObj)
    }

    val response = Await.result(fut, Duration.Inf)
    mapServer.promiseMap -= uid
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
  */

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

  def generateMsg[B](toMGB: MapCommand): Message = synchronized {
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