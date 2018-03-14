package org.telecomsudparis.smap

import scala.concurrent.Promise

case class PromiseResults(pResult: Promise[ResultsCollection])
case class OperationUniqueId(uid: String) extends Serializable
case class CallerId(callerUid: String) extends Serializable
