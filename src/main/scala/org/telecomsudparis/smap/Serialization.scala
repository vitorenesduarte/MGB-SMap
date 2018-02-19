package org.telecomsudparis.smap

import java.io._
import java.nio.ByteBuffer

//from https://stackoverflow.com/questions/39369319/convert-any-type-in-scala-to-arraybyte-and-back

object Serialization {

  def serialise(value: Any): Array[Byte] = {
    val stream: ByteArrayOutputStream = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(stream)
    oos.writeObject(value)
    oos.close
    stream.toByteArray
  }

  def deserialise(bytes: Array[Byte]): Any = {
    val ois = new ObjectInputStream(new ByteArrayInputStream(bytes))
    val value = ois.readObject
    ois.close
    value
  }

  def toByteBuffer(bytes: Array[Byte]): ByteBuffer = {
    ByteBuffer.wrap(bytes)
  }

  def fromByteBuffer(bb: ByteBuffer): Array[Byte] = {
    bb.array
  }
/*
  class ObjectInputStreamWithCustomClassLoader(
  fileInputStream: FileInputStream
) extends ObjectInputStream(fileInputStream) {
  override def resolveClass(desc: java.io.ObjectStreamClass): Class[_] = {
    try { Class.forName(desc.getName, false, getClass.getClassLoader) }
    catch { case ex: ClassNotFoundException => super.resolveClass(desc) }
  }
  }
  */

  def writeObjectToFile(obj: AnyRef, fileName: String): Unit = {
    val fos = new FileOutputStream(fileName)
    val oos = new ObjectOutputStream(fos)
    oos.writeObject(obj)
    oos.close()
  }

  def readObjectFromFile(fileName: String): AnyRef = {
    val fis = new FileInputStream(fileName)
    val ois = new ObjectInputStream(fis)
    val anotherObj = ois.readObject()
    ois.close()
    anotherObj
  }
}