package org.telecomsudparis.smap

trait ReadOp

//Not Pure Write
//case class Put[A, B, String](key: A, data: B, uuid: String, tName: String) extends Serializable

//Pure Writes
case class Insert[A, B, String](key: A, data: B, uuid: String, tName: String) extends Serializable
case class Update[A, B, String](key: A, data: B, uuid: String, tName: String) extends Serializable
case class Delete[A, String](key: A, uuid: String, tName: String) extends Serializable

case class Get[A, String](key: A, uuid: String, tName: String) extends Serializable with ReadOp
case class Scan[A, Integer, String](startingKey: A, recordCount: Integer, uuid: String, tName: String) extends Serializable with ReadOp
