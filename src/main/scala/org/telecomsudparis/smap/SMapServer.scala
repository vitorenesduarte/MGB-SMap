package org.telecomsudparis.smap

import org.imdea.vcd.pb.Proto._
import org.imdea.vcd._

import scala.concurrent._
import ExecutionContext.Implicits.global
import concurrent.ExecutionContext.Implicits.global._
import java.util.concurrent.{ExecutorService, Executors}

import scala.collection.concurrent.{TrieMap => CTrieMap}
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.mutable.{TreeMap => MTreeMap}
import scala.collection.mutable.{Map => MMap}
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import java.util.concurrent.LinkedBlockingQueue
import java.io.IOException
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
  * Consumer Class
  */
class SMapServer(var localReads: Boolean, var verbose: Boolean, var config: Array[String]) {
  var serverId: String = Thread.currentThread().getName + java.util.UUID.randomUUID.toString
  var javaClientConfig = Config.parseArgs(config)
  var javaSocket = Socket.create(javaClientConfig, 100)
  //val dummySocket = DummySocket.create(javaClientConfig)

  var mapCopy = MTreeMap[String, MMap[String, String]]()
  var pendingMap = CTrieMap[CallerId, Promise[Boolean]]()
  var promiseMap = CTrieMap[OperationUniqueId, PromiseResults]()
  var queue = new LinkedBlockingQueue[Message]()
  var localReadsQueue = new LinkedBlockingQueue[MapCommand]()

  @volatile var stop = false

  /**
    * Using implicit conversions, definition at package.scala.
    */

  var receiveMessages: Thread = new Thread(() => receiveLoop)
  var consume: Thread = new Thread(()=> consumeLocally)
  var localReading: Thread = new Thread(()=> doLocalReading)
  var pool: ExecutorService = Executors.newFixedThreadPool(3)

  /**
    * Lock definition to properly access mapCopy when doing local reads.
    */
  var lock = new ReentrantReadWriteLock()

  if(verbose) {
    println("SMapServer Id: " + serverId)
    println("Local Reads: " + localReads)
  }

  /**
    * Thread handling MessageSets received from MGB.
    */
  def receiveLoop(): Unit = {
    try {
      while (!stop) {
        val receivedMsgSet = javaSocket.receive()
        serverExecuteCmd(receivedMsgSet)
      }
    } catch {
      case ex: IOException =>
        println("Receive Loop Interrupted at SMapServer: " ++ serverId)
    }
  }

  /**
    *  The thread builds a MGB MessageSet and sends it through javaClient Socket
    */
  def consumeLocally(): Unit = {
    try {
      var msgList = ListBuffer[Message]().asJava
      while (!stop) {
        val m: Message = queue.take()
        msgList.add(m)
        queue.drainTo(msgList)

        val mgbMsgSet = MessageSet.newBuilder().setStatus(MessageSet.Status.START).addAllMessages(msgList).build()
        javaSocket.send(mgbMsgSet)

        msgList.clear()

      }
    } catch {
      case ex: InterruptedException =>
        println("Consume Loop Interrupted at SMapServer: " ++ serverId)
    }
  }

  /**
    *  Thread for processing local read requests, calls applyOperation with hardcoded DELIVERED status
    */
  def doLocalReading(): Unit = {
    try {
      var readList = ListBuffer[MapCommand]().asJava
      while (!stop) {
        val readOperation = localReadsQueue.take()
        readList.add(readOperation)
        localReadsQueue.drainTo(readList)

        try {
          lock.readLock().lock()
          //Since we're doing local reads I assume DELIVERED msgStatus
          readList.asScala.foreach(rOp => applyOperation(rOp)(MessageSet.Status.DELIVERED))
        } finally {
          lock.readLock().unlock()
        }
        readList.clear()
      }
    } catch {
      case ex: InterruptedException =>
        println("LocalRead Consume Loop Interrupted at SMapServer: " ++ serverId)
    }
  }

  def serverInit(): Unit = {
    pool.execute(receiveMessages)
    pool.execute(consume)
    if(localReads)
      pool.execute(localReading)
  }

  def serverClose(): Unit = {
    this.stop = true
    pool.shutdownNow()
    javaSocket.closeRw()
    //dummySocket.closeRw()
  }

  /**
    *  Calls applyOperation for each MapCommand obtained through the MGB MessageSet
    */
  def serverExecuteCmd(mset: MessageSet): Unit = {
    val msgSetAsList = mset.getMessagesList.asScala
    val unmarshalledSet = msgSetAsList map (msg => SMapServer.unmarshallMGBMsg(msg))

    try {
      lock.writeLock().lock()
      unmarshalledSet foreach (e => applyOperation(e)(mset.getStatus))
    } finally {
      lock.writeLock().unlock()
    }

  }

  def ringBell(uid: OperationUniqueId, pm: CTrieMap[OperationUniqueId, PromiseResults], pr: ResultsCollection): Unit = {
    if(pm isDefinedAt uid) {
        pm(uid).pResult success pr
    }
  }

  def ringBellPending(cid: CallerId, pendingM: CTrieMap[CallerId, Promise[Boolean]]): Unit = {
    if(pendingM isDefinedAt cid) {
      pendingM(cid) success true
    }
  }

  def applyOperation(deliveredOperation: MapCommand)(msgSetStatus: MessageSet.Status): Unit = {
    import org.telecomsudparis.smap.MapCommand.OperationType._
    import org.imdea.vcd.pb.Proto.MessageSet.Status

    val uuid = OperationUniqueId(deliveredOperation.operationUuid)
    val cid = CallerId(deliveredOperation.callerId)
    val opItem = deliveredOperation.getItem
    val opItemKey = opItem.key

    deliveredOperation.operationType match {
      case INSERT =>
        if(msgSetStatus == Status.COMMITTED){
          ringBell(uuid, promiseMap, ResultsCollection())
        } else {
          //opItem is immutable.Map, doing a conversion.
          val mutableFieldsMap: MMap[String, String] = MMap() ++ opItem.fields
          if(msgSetStatus == Status.DELIVERED){
            mapCopy += (opItemKey -> mutableFieldsMap)
            //ringBellPending(cid, pendingMap)
          }
        }

      case UPDATE =>
        if(msgSetStatus == Status.COMMITTED){
          ringBell(uuid, promiseMap, ResultsCollection())
        } else {
          val mutableFieldsMap: MMap[String, String] = MMap() ++ opItem.fields
          if(msgSetStatus == Status.DELIVERED){
            if(mapCopy isDefinedAt opItemKey){
              mutableFieldsMap.foreach(f => mapCopy(opItemKey).update(f._1, f._2))
            } else {
              mapCopy += (opItemKey -> mutableFieldsMap)
              //ringBellPending(cid, pendingMap)
            }
          }
        }

      case DELETE =>
        if(msgSetStatus == Status.COMMITTED){
          ringBell(uuid, promiseMap, ResultsCollection())
        } else {
          if(msgSetStatus == Status.DELIVERED){
            mapCopy -= opItemKey
            //ringBellPending(cid, pendingMap)
          }
        }

      case GET =>
        //in case of (localReads == false) apply GET only to the caller
        if(msgSetStatus == Status.DELIVERED){
          val result: ResultsCollection = {
            if(mapCopy isDefinedAt opItemKey) {
              val tempResult: MMap[String, String] = MMap()
              //From YCSB, if fields set is empty must read all fields
              val keySet = if(opItem.fields.isEmpty) mapCopy(opItemKey).keys else opItem.fields.keys
              for (fieldKey <- keySet) {
                if(mapCopy(opItemKey) isDefinedAt fieldKey)
                  tempResult += (fieldKey -> mapCopy(opItemKey)(fieldKey))
                //else should do tempResult += (fieldKey -> defaultEmptyValue)
              }
              //Item.fields is an immutableMap, making a conversion then.
              val getItem = Item(key = opItemKey, fields = tempResult.toMap)
              ResultsCollection(Seq(getItem))
            } else {
              ResultsCollection(Seq(Item(key = opItemKey)))
            }
          }
          ringBell(uuid, promiseMap, result)
        }

      case SCAN =>
        if(msgSetStatus == Status.DELIVERED){
          var seqResults: Seq[Item] = Seq()

          if(mapCopy isDefinedAt deliveredOperation.startKey) {
            val mapCopyScan = (mapCopy from deliveredOperation.startKey).slice(0, deliveredOperation.recordcount)
            for (elem <- mapCopyScan.values) {
              val tempResult: MMap[String, String] = MMap()
              //From YCSB, if fields set is empty must read all fields
              val keySet = if (opItem.fields.isEmpty) mapCopy(deliveredOperation.startKey).keys else opItem.fields.keys
              for (fieldKey <- keySet) {
                if (elem isDefinedAt fieldKey)
                  tempResult += (fieldKey -> elem(fieldKey))
                //else should do tempResult += (fieldKey -> defaultEmptyValue)
              }
              val tempItem = Item(fields = tempResult.toMap)
              seqResults :+= tempItem
            }
          }
          //Returning empty sequence of items in case of nondefined startingKey
          ringBell(uuid, promiseMap, ResultsCollection(seqResults))
        }

      case _ => println("Unknown Operation")
    }

  }

  /*
  def saveMap = Serialization.writeObjectToFile(this.mapCopy, "/tmp/map.ser")

  def readMap = Serialization.readObjectFromFile("/tmp/map.ser")

  def setMapFromFile() = { this.mapCopy = readMap.asInstanceOf[MTreeMap[String,B]] }
  */

}

object SMapServer {
  //WARNING: Returning only the data field.
  def unmarshallMGBMsg(m: Message): MapCommand = {
    val hashMGB = m.getHash
    val dataMGB = m.getData

    MapCommand.parseFrom(dataMGB.toByteArray)
  }

}