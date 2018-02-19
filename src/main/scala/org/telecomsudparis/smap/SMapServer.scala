package org.telecomsudparis.smap

import org.imdea.vcd.pb.Proto._
import org.imdea.vcd._

import scala.concurrent._
import ExecutionContext.Implicits.global
import concurrent.ExecutionContext.Implicits.global._
import java.util.concurrent.{ExecutorService, Executors}

import scala.collection.concurrent.{TrieMap => ConcurrentTrieMap}
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.mutable.{TreeMap => MTreeMap}
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import java.util.concurrent.LinkedBlockingQueue
import java.io.IOException
import java.util.concurrent.locks.ReentrantReadWriteLock

//consumer
class SMapServer[B](serverId: String, var localReads: Boolean, var verbose: Boolean, var config: Array[String]) {
  //FIXME: This is horrible, should properly type stuff
  type pResults = Promise[resultsType]
  type resultsType = Either[Option[B], java.util.Collection[B]]

  var javaClientConfig = Config.parseArgs(config)
  var javaSocket = Socket.create(javaClientConfig)
  //val dummySocket = DummySocket.create(javaClientConfig)

  //var mapCopy = MHashMap[String, B]()
  //FIXME: Should rollback to [A,B], only using string because of YCSB.
  var mapCopy = MTreeMap[String, B]()
  //var mapCopy = MTreeMap[A, B]()

  //We want to be sequentially consistent
  //Object.uuid -> Promise
  //FIXME: Better typing
  var pendingMap = ConcurrentTrieMap[String, Promise[Boolean]]()

  //ThreadName -> Object.uuid
  // var lastCall = ConcurrentTrieMap[String, String]()

  //FIXME: First map parameter (A) should be of type OperationUniqueId and NOT raw String
  var promiseMap = ConcurrentTrieMap[String, pResults]()

  var queue = new LinkedBlockingQueue[Message]()

  //val localReadsQueue = new LinkedBlockingQueue[Get[A,String]]()
  var localReadsQueue = new LinkedBlockingQueue[ReadOp]()

  @volatile var stop = false
  //uses implicit conversions
  var receiveMessages: Thread = new Thread(() => receiveLoop)
  var consume: Thread = new Thread(()=> consumeLocally)
  var localReading: Thread = new Thread(()=> doLocalReading)
  var pool: ExecutorService = Executors.newFixedThreadPool(3)
  var lock = new ReentrantReadWriteLock()

  if(verbose) {
    println("SMapServer Id: " + this.serverId)
    println("Local Reads: " + this.localReads.toString)
  }

  //override lazy val logger = Logger[VCDMapServer[B]]

  implicit def funcToRunnable( func : () => Unit ) = new Runnable(){ def run() = func()}

  def receiveLoop(): Unit = {
    try {
      while (!stop) {
        val receivedMsgSet = javaSocket.receive()
        serverExecuteCmd(receivedMsgSet)
      }
    } catch {
      case ex: IOException =>
        println("Receive Loop Interrupted at Server " ++ serverId)
    }
  }

  //WRITER THREAD
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
        println("Consume Loop Interrupted at Server " ++ serverId)
    }
  }

  def doLocalReading(): Unit = {
    try {
      var readList = ListBuffer[ReadOp]().asJava
      while (!stop) {
        val readOperation = localReadsQueue.take()
        readList.add(readOperation)
        localReadsQueue.drainTo(readList)

        //val readOpElem = localReadsQueue.take()
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
        println("LocalRead Consume Loop Interrupted at Server " ++ serverId)
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

  def serverExecuteCmd(mset: MessageSet): Unit = {
    val msgSetAsList = mset.getMessagesList.asScala
    val unmarshalledSet = msgSetAsList map (msg => SMapServer.unmarshallMsg(msg))

    try {
      lock.writeLock().lock()
      unmarshalledSet foreach (e => applyOperation(e)(mset.getStatus))
    } finally {
      lock.writeLock().unlock()
    }

  }

  def ringBell(uid: String, pMap: ConcurrentTrieMap[String, pResults], pr: resultsType): Unit = {
    if(pMap isDefinedAt uid) {
        pMap(uid) success pr
    }
  }

  def ringBellPending(uid: String, pendingM: ConcurrentTrieMap[String, Promise[Boolean]]): Unit = {
    if(pendingM isDefinedAt uid) {
      pendingM(uid) success true
    }
  }

  /*
  def slicedToArray[B](sliced: MTreeMap[A,B])(implicit tag:ClassTag[B]): Array[B] = {
    sliced.values.toArray
    sliced.values.to
  }
  */

  def applyOperation(deliveredOperation: Any)(msgSetStatus: MessageSet.Status): Unit = {
    val msgStatusNumber = msgSetStatus.getNumber

    //   println(queue.size+" "+pendingMap.size+" "+promiseMap.size+" "+localReadsQueue.size)

    deliveredOperation match {
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

      case _ => println("Unknown Operation")
    }

  }

  def saveMap = Serialization.writeObjectToFile(this.mapCopy, "/tmp/map.ser")

  def readMap = Serialization.readObjectFromFile("/tmp/map.ser")

  def setMapFromFile() = { this.mapCopy = readMap.asInstanceOf[MTreeMap[String,B]] }

}

object SMapServer {
  //WARNING: Returning only the data field.
  def unmarshallMsg(m: Message): Any = {
    val h = m.getHash
    val d = m.getData

    Serialization.deserialise(m.getData.toByteArray)
  }

  //If I define the ordering[A] here,
  //maybe the java code can then recognize it
  //when I roll back to VCDMapServer[A,B]
  //implicit def ipord: Ordering[IntPair] =
}