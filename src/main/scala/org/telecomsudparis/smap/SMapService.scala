package org.telecomsudparis.smap

class SMapService extends smapGrpc.smapBlockingClient {
  override def executeCmd(request: MapCommand): Result = ???

}
