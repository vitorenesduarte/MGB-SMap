package client

import org.telecomsudparis.smap._
import org.scalatest._
import scala.concurrent._
import org.telecomsudparis.smap.MapCommand.OperationType._
import org.telecomsudparis.smap._

//TODO: Should try to use the consistency checking tool https://github.com/ssidhanta/ConSpecTool
class SMapClientSpec extends FlatSpec with Matchers {
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

}
