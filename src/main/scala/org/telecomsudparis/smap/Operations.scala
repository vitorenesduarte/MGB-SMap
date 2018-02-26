package org.telecomsudparis.smap

//trait ReadOp

//Not Pure Write
//case class Put[A, B, String](key: A, data: B, uuid: String, tName: String) extends Serializable

//Pure Writes

/*
case class Insert[A, B](key: A, data: B, opUuid: OperationUuid, tName: ThreadName) extends Serializable
case class Update[A, B](key: A, data: B, opUuid: OperationUuid, tName: ThreadName) extends Serializable
case class Delete[A](key: A, opUuid: OperationUuid, tName: ThreadName) extends Serializable

case class Get[A](key: A, opUuid: OperationUuid, tName: ThreadName) extends Serializable with ReadOp
case class Scan[A](startingKey: A, recordCount: Integer, opUuid: OperationUuid, tName: ThreadName) extends Serializable with ReadOp
*/

//case class OperationUuid(uuid: String) extends Serializable
//case class ThreadName(tName: String) extends Serializable
