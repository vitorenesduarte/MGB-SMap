package org.telecomsudparis.smap

import org.imdea.vcd.pb.Proto._
import org.imdea.vcd._

import scala.concurrent._
//import ExecutionContext.Implicits.global
//import concurrent.ExecutionContext.Implicits.global._
import java.util.concurrent.{ExecutorService, Executors}

import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.mutable.HashMap
import scala.collection.mutable.Map
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.io.IOException
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.logging.Logger

import org.telecomsudparis.smap.SMapServiceServer.Instrumented

/**
  * Consumer Class
  */
class SMapServer(var localReads: Boolean, var verbose: Boolean, var config: Array[String], var retries: Int, var staticConnection: Boolean) extends Instrumented {
  val logger: Logger = Logger.getLogger(classOf[SMapServiceClient].getName)

  private[this] val processWrites = metrics.timer("processWrites")
  private[this] val processReads = metrics.timer("processReads")

  private[this] val processOneRead = metrics.timer("processOneRead")
  private[this] val processOneScan = metrics.timer("processOneScan")
  private[this] val processUpdateCommit = metrics.timer("processUpdateCommit")
  private[this] val processUpdateDelivered = metrics.timer("processUpdateDelivered")

  private[this] val scanSlicing = metrics.timer("scanSlicing")

  var serverId: String = Thread.currentThread().getName + java.util.UUID.randomUUID.toString
  var javaClientConfig = Config.parseArgs(config)

  var javaSocket = if(staticConnection) Socket.createStatic(javaClientConfig, retries) else Socket.create(javaClientConfig, retries)
  //var javaSocket = DummySocket.create(javaClientConfig)

  var map = HashMap[String, Map[String, String]]()
  var promiseMap = new ConcurrentHashMap[OperationUniqueId, PromiseResults]()
  var queue = new LinkedBlockingQueue[Message]()

  @volatile var stop = false

  /**
    * Using implicit conversions, definition at package.scala.
    */

  var receiveMessages: Thread = new Thread(() => receiveLoop)
  var consume: Thread = new Thread(()=> consumeLocally)
  var pool: ExecutorService = Executors.newFixedThreadPool(3)

  // FIXME read concurrently ?
  // FIXME ring pending is wrong (should onlye only if this is the same callID)
  // FIXME ResultsCollection(Seq(Item())) = 16us ! return everything

  /**
    * Lock definition to properly access mapCopy when doing local reads.
    */
  var lock = new ReentrantReadWriteLock()

  if(verbose) {
    logger.info("SMapServer Id: " + serverId)
    logger.info("SMapServer Local Reads: " + localReads)
    logger.info("SMapServer verbose: " + verbose)
    logger.info("SMapServer batching : " + javaClientConfig.getBatching)
  }

  /**
    * Thread handling MessageSets received from MGB.
    */
  def receiveLoop(): Unit = {
    try {
      while (!stop) {
        val receivedMsgSet = javaSocket.receive()
        processWrites.time(serverExecuteCmd(receivedMsgSet))
      }
    } catch {
      case ex: InterruptedException =>
        logger.warning("SMapServer Receive Loop Interrupted: " ++ serverId)
      case ex: Exception =>
        throw new RuntimeException()
    }
  }

  /**
    *  The thread builds a MGB MessageSet and sends it through javaClient Socket
    */
  def consumeLocally(): Unit = {
    try {
      var msgList = ListBuffer[Message]().asJava
      while (!stop) {
        msgList.add(queue.take())
        queue.drainTo(msgList)
        msgList.forEach(javaSocket.send(_))
        if (verbose) {
          msgList.forEach(m => logger.fine(m.toString))
        }
        msgList.clear()
      }
    } catch {
      case ex: InterruptedException =>
        logger.warning("SMapServer Consume Loop Interrupted at: " ++ serverId)
      case ex: Exception =>
        throw new RuntimeException()
    }
  }

  def serverInit(): Unit = {
    pool.execute(receiveMessages)
    pool.execute(consume)
  }

  def serverClose(): Unit = {
    this.stop = true
    pool.shutdownNow()
    javaSocket.close()
  }

  /**
    *  Calls applyOperation for each MapCommand obtained through the MGB MessageSet
    */
  def serverExecuteCmd(mset: MessageSet): Unit = {
    val msgSetAsList = mset.getMessagesList.asScala
    val unmarshalledSet = msgSetAsList map (msg => SMapServer.unmarshallMGBMsg(msg))

    unmarshalledSet foreach (e => applyOperation(e)(mset.getStatus))
  }


  def ringBell(uid: OperationUniqueId, pr: ResultsCollection): Unit = {
    val promise = promiseMap.get(uid)
    if(promise != null) {
      promise.pResult success pr
    }
  }

  def applyOperation(deliveredOperation: MapCommand)(msgSetStatus: MessageSet.Status): Unit = {
    import org.telecomsudparis.smap.MapCommand.OperationType._
    import org.imdea.vcd.pb.Proto.MessageSet.Status

    val uuid = OperationUniqueId(deliveredOperation.operationUuid)
    val cid = CallerId(deliveredOperation.callerId)
    val opItem = deliveredOperation.getItem
    val opItemKey = opItem.key

    if (verbose) {
      logger.info(deliveredOperation.operationUuid + " -> " + msgSetStatus)
    }

    if(msgSetStatus == Status.COMMIT){
      if (verbose) {
        logger.info("ignoring commit notification of " + deliveredOperation.operationUuid)
      }
      // ignore
    } else {
      deliveredOperation.operationType match {
        case INSERT =>
          //opItem is immutable.Map, doing a conversion.
          val mutableFieldsMap: Map[String, String] = Map() ++ opItem.fields
          lock.writeLock().lock()
          map += (opItemKey -> mutableFieldsMap)
          lock.writeLock().unlock()
          ringBell(uuid, ResultsCollection())

        case UPDATE =>
          val mutableFieldsMap: Map[String, String] = Map() ++ opItem.fields
          processUpdateDelivered.time {
            lock.writeLock().lock()
            if (map isDefinedAt opItemKey) {
              mutableFieldsMap.foreach(f => map(opItemKey).update(f._1, f._2))
            } else {
              map += (opItemKey -> mutableFieldsMap)
            }
            lock.writeLock().unlock()
            ringBell(uuid, ResultsCollection())
          }

        case DELETE =>
          lock.writeLock().lock()
          map -= opItemKey
          lock.writeLock().unlock()
          ringBell(uuid, ResultsCollection())

        case GET =>
          processOneRead.time {
            lock.readLock().lock()
            val result: ResultsCollection = {
              if (map isDefinedAt opItemKey) {
                val getItem = Item(key = opItemKey, fields = map(opItemKey).toMap)
                ResultsCollection(Seq(getItem))
              } else {
                ResultsCollection(Seq(Item(key = opItemKey)))
              }
            }
            lock.readLock().unlock()
            ringBell(uuid, result)
          }

        // case SCAN =>
        //   processOneScan.time {
        //     if ((msgSetStatus == Status.DELIVERED) && (promiseMap isDefinedAt uuid)) {
        //       var seqResults: Seq[Item] = Seq()
        //       if (mapCopy isDefinedAt deliveredOperation.startKey) {
        //         val mapCopyScan = scanSlicing.time {
        //           (mapCopy from deliveredOperation.startKey).slice(0, deliveredOperation.recordcount)
        //         }
        //         for (elem <- mapCopyScan.values) {
        //           val tempResult: MMap[String, String] = MMap()
        //           //From YCSB, if fields set is empty must read all fields
        //           val keySet = if (opItem.fields.isEmpty) mapCopy(deliveredOperation.startKey).keys else opItem.fields.keys
        //           for (fieldKey <- keySet) {
        //             if (elem isDefinedAt fieldKey)
        //               tempResult += (fieldKey -> elem(fieldKey))
        //             //else should do tempResult += (fieldKey -> defaultEmptyValue)
        //           }
        //           val tempItem = Item(fields = tempResult.toMap)
        //           seqResults :+= tempItem
        //         }
        //       }
        //       //Returning empty sequence of items in case of nondefined startingKey
        //       ringBell(uuid, ResultsCollection(seqResults))
        //     }
        //   }

        case _ => println("Unknown Operation")
      }
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
    val dataMGB = m.getData
    MapCommand.parseFrom(dataMGB.toByteArray)
  }

}
