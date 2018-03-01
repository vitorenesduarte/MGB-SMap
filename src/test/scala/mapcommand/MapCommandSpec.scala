package mapcommand

import org.scalatest._
import org.telecomsudparis.smap.MapCommand.OperationType._
import org.telecomsudparis.smap._

//TODO: Should try to use the consistency checking tool https://github.com/ssidhanta/ConSpecTool
class MapCommandSpec extends FlatSpec with Matchers {
  "Building GET Map Command" should "run" in {
    val testItem = Item(key = "vehicles", fields = Map("car" -> "", "bike" -> ""))
    val testMapCommand = MapCommand(
      item = Some(testItem), callerId = Thread.currentThread().getName, operationUuid = java.util.UUID.randomUUID.toString, operationType = GET
    )
    println(testMapCommand)
  }

}
