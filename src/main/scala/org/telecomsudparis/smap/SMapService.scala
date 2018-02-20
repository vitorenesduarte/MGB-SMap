package org.telecomsudparis.smap
import scala.concurrent.Future

class SMapService[B](config: Array[String]) extends smapGrpc.smap {

  //pass both as parameters to the ServerBuilder
  var serviceServer = new SMapServer[B]("serverService", localReads = true, verbose = true, config)
  var serviceClient = new SMapClient(true, serviceServer)

  override def executeCmd(request: MapCommand): Future[Result] = {
    Future.successful(processCmd(request))
  }

  private def processCmd(req: MapCommand): Result = {
    Result(true)
  }

}
