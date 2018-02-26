package org.telecomsudparis.smap
import scala.concurrent.Future

class SMapService[B](serviceServer: SMapServer, serviceClient: SMapClient) extends smapGrpc.smap {

  //pass both as parameters to the ServerBuilder

  override def executeCmd(request: MapCommand): Future[ResultsCollection] = {
    Future.successful(processCmd(request))
  }

  private def processCmd(req: MapCommand): ResultsCollection = {
    serviceClient.sendCommand(req)
  }

}
