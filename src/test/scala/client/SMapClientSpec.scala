package client

import org.telecomsudparis.smap._
import org.scalatest._
import scala.concurrent._
import org.telecomsudparis.smap.MapCommand.OperationType._
import org.telecomsudparis.smap._
import scala.collection.JavaConverters._

//TODO: Should try to use the consistency checking tool https://github.com/ssidhanta/ConSpecTool
class SMapClientSpec extends FlatSpec with Matchers {
  /*
  "INSERT GET lreads = true" should "run" in {
    val s1 = new SMapServer(localReads = true, verbose = true, config = Array(""))
    s1.serverInit()

    val c1 = new SMapClient(verbose = true, s1)

    val getItem = Item(key = "vehicles", fields = Map("car" -> "", "bike" -> ""))
    val getMapCommand = MapCommand(
      item = Some(getItem), callerId = Thread.currentThread().getName, operationUuid = java.util.UUID.randomUUID.toString, operationType = GET
    )

    val writeItem = Item(key = "vehicles", fields = Map("car" -> "red"))
    val writeMapCommand = MapCommand(
      item = Some(writeItem), callerId = Thread.currentThread().getName, operationUuid = java.util.UUID.randomUUID.toString, operationType = INSERT
    )

    val writeAns = c1.sendCommand(writeMapCommand)
    val getAns = c1.sendCommand(getMapCommand)

    println(writeAns)
    println(getAns)

    Thread.sleep(1000)

    println("SERVERMAP:" + s1.mapCopy)
  }

  "INSERT GET lreads = false" should "run" in {
    val s1 = new SMapServer(localReads = false, verbose = true, config = Array(""))
    s1.serverInit()

    val c1 = new SMapClient(verbose = true, s1)

    val getItem = Item(key = "vehicles", fields = Map("car" -> "", "bike" -> ""))
    val getMapCommand = MapCommand(
      item = Some(getItem), callerId = Thread.currentThread().getName, operationUuid = java.util.UUID.randomUUID.toString, operationType = GET
    )

    val writeItem = Item(key = "vehicles", fields = Map("car" -> "red"))
    val writeMapCommand = MapCommand(
      item = Some(writeItem), callerId = Thread.currentThread().getName, operationUuid = java.util.UUID.randomUUID.toString, operationType = INSERT
    )

    val writeAns = c1.sendCommand(writeMapCommand)
    val getAns = c1.sendCommand(getMapCommand)

    println(writeAns)
    println(getAns)

    Thread.sleep(1000)

    println("SERVERMAP:" + s1.mapCopy)
  }
*/
  "GET UPDATE GET lreads = true" should "run" in {
    val s1 = new SMapServer(localReads = true, verbose = true, config = Array(""))
    s1.serverInit()

    val c1 = new SMapClient(verbose = true, s1)

    val getItem = Item(key = "vehicles", fields = Map("car" -> "", "motorbike" -> ""))
    val getMapCommand = MapCommand(
      item = Some(getItem),
      callerId = Thread.currentThread().getName,
      operationUuid = java.util.UUID.randomUUID.toString,
      operationType = GET
    )

    val updateItem = Item(key = "vehicles", fields = Map("car" -> "green", "motorbike" -> "blue"))
    val updateMapCommand = MapCommand(
      item = Some(updateItem),
      callerId = Thread.currentThread().getName,
      operationUuid = java.util.UUID.randomUUID.toString,
      operationType = UPDATE
    )

    val getItem2 = Item(key = "vehicles", fields = Map[String,String]())
    val getMapCommand2 = MapCommand(
      item = Some(getItem2),
      callerId = Thread.currentThread().getName,
      operationUuid = java.util.UUID.randomUUID.toString,
      operationType = GET
    )

    //val getAns = c1.sendCommand(getMapCommand)
    val updateAns = c1.sendCommand(updateMapCommand)
    Thread.sleep(1000)
    val getAns2 = c1.sendCommand(getMapCommand2)

    //println("FIRST GET: \n" + getAns)
    //println("SECOND GET: \n" + getAns2)

    Thread.sleep(1000)

    println("SERVERMAP:" + s1.mapCopy)
  }

  /*
  "INSERT SCAN lreads = true" should "run" in {
    val s1 = new SMapServer(localReads = true, verbose = true, config = Array(""))
    s1.serverInit()
    val c1 = new SMapClient(verbose = true, s1)

    val updateItem1 = Item(key = "vehicles1", fields = Map("car1" -> "green", "motorbike1" -> "blue", "truck1" -> "red"))
    val updateItem2 = Item(key = "vehicles2", fields = Map("car2" -> "red", "motorbike2" -> "blue"))
    val updateItem3 = Item(key = "vehicles3", fields = Map("car3" -> "black", "motorbike3" -> "black", "truck3" -> "black"))
    val updateItem4 = Item(key = "vehicles4", fields = Map("car4" -> "yellow", "motorbike4" -> "red", "truck4" -> "blue"))
    val updateItem5 = Item(key = "vehicles5", fields = Map("car5" -> "blue", "motorbike5" -> "white"))

    val updateMapCommand1 = MapCommand(
      item = Some(updateItem1),
      callerId = Thread.currentThread().getName,
      operationUuid = java.util.UUID.randomUUID.toString,
      operationType = UPDATE
    )
    val updateMapCommand2 = MapCommand(
      item = Some(updateItem2),
      callerId = Thread.currentThread().getName,
      operationUuid = java.util.UUID.randomUUID.toString,
      operationType = UPDATE
    )
    val updateMapCommand3 = MapCommand(
      item = Some(updateItem3),
      callerId = Thread.currentThread().getName,
      operationUuid = java.util.UUID.randomUUID.toString,
      operationType = UPDATE
    )
    val updateMapCommand4 = MapCommand(
      item = Some(updateItem4),
      callerId = Thread.currentThread().getName,
      operationUuid = java.util.UUID.randomUUID.toString,
      operationType = UPDATE
    )
    val updateMapCommand5 = MapCommand(
      item = Some(updateItem5),
      callerId = Thread.currentThread().getName,
      operationUuid = java.util.UUID.randomUUID.toString,
      operationType = UPDATE
    )

    val scanFields = Item(fields = Map("car1"->"","motorbike1"->"", "car2" -> "", "car3"->""))
    val scanMapCommand = MapCommand(
      item = Some(scanFields),
      startKey = "vehicles1",
      recordcount = 3,
      operationUuid = java.util.UUID.randomUUID.toString,
      operationType = SCAN
    )

    c1.sendCommand(updateMapCommand1)
    c1.sendCommand(updateMapCommand2)
    c1.sendCommand(updateMapCommand3)
    c1.sendCommand(updateMapCommand4)
    c1.sendCommand(updateMapCommand5)

    val scanAns = c1.sendCommand(scanMapCommand)

    println("FIRST GET: \n" + scanAns)

    Thread.sleep(1000)

    println("SERVERMAP:" + s1.mapCopy)
    println(scanAns.results.map(r => r.fields).toVector.asJava)
  }
  */
}
