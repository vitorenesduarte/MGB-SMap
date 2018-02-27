package client

import org.telecomsudparis.smap._
import org.scalatest._
import scala.concurrent._

//TODO: Should try to use the consistency checking tool https://github.com/ssidhanta/ConSpecTool
class ClientSpec extends FlatSpec with Matchers {
  "Simple Check" should "run" in {
    val s1 = new SMapServer(localReads = true, verbose = true, config = Array(""))
    s1.serverInit()
    val c1 = new SMapClient(verbose = true, s1)
    /*
    val c1 = new SMapClient(true, s1)
    val send1 = new Thread(new Runnable {
      def run() {
        c1.sendUpdate("a", 3) shouldEqual Left(None)
        c1.sendGet("a") shouldEqual Left(Some(3))
        c1.sendUpdate("b",4) shouldEqual Left(None)
        c1.sendGet("b") shouldEqual Left(Some(4))
      }
    })

    val c2 = new SMapClient(true, s1)

    val send2 = new Thread(new Runnable {
      def run(): Unit = {
        c2.sendUpdate("c", 4) shouldEqual Left(None)
        c2.sendGet("c") shouldEqual Left(Some(4))
        c2.sendUpdate("b", 6) shouldEqual Left(None)
        c2.sendGet("b") shouldEqual Left(Some(6))
      }
    })

    send1.start()
    send2.start()
    Thread.sleep(1000)
    println("SERVERMAP:" + s1.mapCopy)
    */
  }

}
