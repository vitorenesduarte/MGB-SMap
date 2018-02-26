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
  var javaSocket = Socket.create(javaClientConfig)
  //val dummySocket = DummySocket.create(javaClientConfig)

  var mapCopy = MTreeMap[String, MMap[String, String]]()
  var pendingMap = CTrieMap[OperationUniqueId, Promise[Boolean]]()
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

        val builder = MessageSet.newBuilder()
        builder.setStatus(MessageSet.Status.START)
        builder.addAllMessages(msgList)
        javaSocket.send(builder.build())

        msgList.clear()
        /*
        val taking = queue.take()
        javaSocket.send(taking)
        */
        //javaSocket.sendWithDelay(taking)
      }
    } catch {
      case ex: InterruptedException =>
        println("Consume Loop Interrupted at SMapServer: " ++ serverId)
    }
  }

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

  /*
  def ringBell(uid: OperationUniqueId, pMap: CTrieMap[OperationUniqueId, PromiseResults], pr: Results): Unit = {
    if(pMap isDefinedAt uid) {
        pMap(uid) success pr
    }
  }
  */

  /*
  def ringBellPending(uid: String, pendingM: ConcurrentTrieMap[String, Promise[Boolean]]): Unit = {
    if(pendingM isDefinedAt uid) {
      pendingM(uid) success true
    }
  }
  */

  /*
  def slicedToArray[B](sliced: MTreeMap[A,B])(implicit tag:ClassTag[B]): Array[B] = {
    sliced.values.toArray
    sliced.values.to
  }
  */

  def applyOperation(deliveredOperation: MapCommand)(msgSetStatus: MessageSet.Status): Unit = {
    //val msgStatusNumber = msgSetStatus.getNumber

    //println(queue.size+" "+pendingMap.size+" "+promiseMap.size+" "+localReadsQueue.size)
    val uuid = OperationUniqueId(deliveredOperation.operationUuid)
    val opItem = deliveredOperation.getItem

    deliveredOperation.operationType match {
      /*
      case Put(_,_,_,_) =>
        val p = deliveredOperation.asInstanceOf[Put[String, B, String]]
        //Not pure write, should wait until DELIVERED status
        if(msgStatusNumber == 2) {
          val result = mapCopy put(p.key, p.data)
          ringBell(p.uuid, promiseMap, Left(result))
          ringBellPending(p.uuid, pendingMap)

        }
       */
      //Insert a new record
      case MapCommand.OperationType.INSERT =>
        if(msgSetStatus == MessageSet.Status.COMMITTED){
          //ringBell(uuid, promiseMap, Results())
        } else {
          if(msgSetStatus == MessageSet.Status.DELIVERED){

            //mapCopy += (opItem.key ->)
          }
        }
        /*
      case MapCommand.OperationType.UPDATE =>
      case MapCommand.OperationType.DELETE =>
      case MapCommand.OperationType.GET =>
      case MapCommand.OperationType.SCAN =>
      */


      /*
      case Insert(_,_,_,_) =>
        val i = deliveredOperation.asInstanceOf[Insert[String, B, String]]
        //Pure Write, can respond to the client as soon as I receive COMMITTED status
        if(msgStatusNumber == 1) {
          ringBell(i.uuid, promiseMap, Left(None))
        } else { // DELIVERED
          mapCopy += (i.key -> i.data)
          ringBellPending(i.uuid, pendingMap)
          /*
          if(verbose) {
            logger.debug(s"(ApplyOperation) Server: $serverId, " +
              s"MessageSet Status: $msgStatusNumber, Insert Operation, MapCopy=$mapCopy")
          }
          */
        }

      case Update(_,_,_,_) =>
        val u = deliveredOperation.asInstanceOf[Update[String, B, String]]
        //Pure Write, can respond to the client as soon as I receive COMMITTED status
        if(msgStatusNumber == 1) {
          ringBell(u.uuid, promiseMap, Left(None))
        } else { //DELIVERED
          mapCopy update (u.key, u.data)
          ringBellPending(u.uuid, pendingMap)

          /*
          if(verbose) {
            logger.debug(s"(ApplyOperation) Server: $serverId, " +
              s"MessageSet Status: $msgStatusNumber, Update Operation, MapCopy=$mapCopy")
          }
          */
        }

      case Delete(_,_,_) =>
        val d = deliveredOperation.asInstanceOf[Delete[String, String]]
        //Pure Write, can respond to the client as soon as I receive COMMITTED status
        if(msgStatusNumber == 1) {
          ringBell(d.uuid, promiseMap, Left(None))
        } else { //DELIVERED
          mapCopy -= d.key
          ringBellPending(d.uuid, pendingMap)
          /*
          if(verbose) {
            logger.debug(s"(ApplyOperation) Server: $serverId, " +
              s"MessageSet Status: $msgStatusNumber, Delete Operation, MapCopy=$mapCopy")
          }
          */
        }

      case Get(_,_,_) =>
        val g = deliveredOperation.asInstanceOf[Get[String, String]]
        //in case of (localReads == false) apply GET only to the caller
        if(msgSetStatus.getNumber == 2) {
          val result = mapCopy get g.key
          ringBell(g.uuid, promiseMap, Left(result))
        }

      case Scan(_,_,_,_) =>
        val s = deliveredOperation.asInstanceOf[Scan[String, Integer, String]]
        //in case of (localReads == false) apply SCAN only to the caller
        if(msgSetStatus.getNumber == 2) {
          val partialResult = (mapCopy from s.startingKey) slice(0, s.recordCount)
          val result = partialResult.values.toVector.asJavaCollection

          //val result = List[B]().toVector.asJavaCollection
          ringBell(s.uuid, promiseMap, Right(result))
            /*
          if(verbose) {
            logger.debug(s"(ApplyOperation) Server: $serverId, " +
              s"MessageSet Status: $msgStatusNumber, Scan Operation Result = $result, MapCopy=$mapCopy")
          }
          */
        }
      */
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

  //If I define the ordering[A] here,
  //maybe the java code can then recognize it
  //when I roll back to VCDMapServer[A,B]
  //implicit def ipord: Ordering[IntPair] =
}