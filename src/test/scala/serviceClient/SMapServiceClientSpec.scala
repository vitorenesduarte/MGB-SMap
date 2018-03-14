package serviceClient

import org.scalatest._
import org.telecomsudparis.smap.MapCommand.OperationType._
import org.telecomsudparis.smap._

import scala.collection.JavaConverters._

//TODO: Should try to use the consistency checking tool https://github.com/ssidhanta/ConSpecTool
//TODO: Try https://github.com/jepsen-io/jepsen

class SMapServiceClientSpec extends FlatSpec with Matchers {
  "Using service" should "run" in {

    val updateItem1 = Item(key = "vehicles1",
      fields = Map("car1" -> "green", "motorbike1" -> "blue", "truck1" -> "red"))

    val updateMapCommand1 = MapCommand(
      item = Some(updateItem1),
      callerId = Thread.currentThread().getName + SMapClient.uuid(),
      operationUuid = SMapClient.uuid(),
      operationType = UPDATE
    )

    val getItem2 = Item(key = "vehicles1",
      fields = Map[String,String]())

    val getMapCommand2 = MapCommand(
      item = Some(getItem2),
      callerId = Thread.currentThread().getName + SMapClient.uuid(),
      operationUuid = SMapClient.uuid(),
      operationType = GET
    )

    val emptyCfg = ClientConfig()
    val hostString = SMapServiceClient.zkGetClosest(emptyCfg)
    val client = new SMapServiceClient(ClientConfig(host = hostString))

    val updateItemResult = client.sendCmd(updateMapCommand1)
    Thread.sleep(1000)
    val getItemResult = client.sendCmd(getMapCommand2)

    println(updateItemResult)
    println(getItemResult)
  }
}
