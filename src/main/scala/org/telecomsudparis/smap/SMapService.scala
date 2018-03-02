package org.telecomsudparis.smap
import scala.concurrent.Future

class SMapService(serviceServer: SMapServer, serviceClient: SMapClient) extends smapGrpc.smap {

  override def executeCmd(request: MapCommand): Future[ResultsCollection] = {
    Future.successful(processCmd(request))
  }

  private def processCmd(req: MapCommand): ResultsCollection = {
    serviceClient.sendCommand(req)
  }

}
