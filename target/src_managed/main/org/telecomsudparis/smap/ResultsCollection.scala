// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package org.telecomsudparis.smap

import scala.collection.JavaConverters._

@SerialVersionUID(0L)
final case class ResultsCollection(
    results: _root_.scala.collection.Seq[org.telecomsudparis.smap.Result] = _root_.scala.collection.Seq.empty
    ) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[ResultsCollection] with com.trueaccord.lenses.Updatable[ResultsCollection] {
    @transient
    private[this] var __serializedSizeCachedValue: Int = 0
    private[this] def __computeSerializedValue(): Int = {
      var __size = 0
      results.foreach(results => __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(results.serializedSize) + results.serializedSize)
      __size
    }
    final override def serializedSize: Int = {
      var read = __serializedSizeCachedValue
      if (read == 0) {
        read = __computeSerializedValue()
        __serializedSizeCachedValue = read
      }
      read
    }
    def writeTo(`_output__`: _root_.com.google.protobuf.CodedOutputStream): Unit = {
      results.foreach { __v =>
        _output__.writeTag(1, 2)
        _output__.writeUInt32NoTag(__v.serializedSize)
        __v.writeTo(_output__)
      };
    }
    def mergeFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): org.telecomsudparis.smap.ResultsCollection = {
      val __results = (_root_.scala.collection.immutable.Vector.newBuilder[org.telecomsudparis.smap.Result] ++= this.results)
      var _done__ = false
      while (!_done__) {
        val _tag__ = _input__.readTag()
        _tag__ match {
          case 0 => _done__ = true
          case 10 =>
            __results += _root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, org.telecomsudparis.smap.Result.defaultInstance)
          case tag => _input__.skipField(tag)
        }
      }
      org.telecomsudparis.smap.ResultsCollection(
          results = __results.result()
      )
    }
    def clearResults = copy(results = _root_.scala.collection.Seq.empty)
    def addResults(__vs: org.telecomsudparis.smap.Result*): ResultsCollection = addAllResults(__vs)
    def addAllResults(__vs: TraversableOnce[org.telecomsudparis.smap.Result]): ResultsCollection = copy(results = results ++ __vs)
    def withResults(__v: _root_.scala.collection.Seq[org.telecomsudparis.smap.Result]): ResultsCollection = copy(results = __v)
    def getFieldByNumber(__fieldNumber: Int): scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => results
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => _root_.scalapb.descriptors.PRepeated(results.map(_.toPMessage)(_root_.scala.collection.breakOut))
      }
    }
    override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
    def companion = org.telecomsudparis.smap.ResultsCollection
}

object ResultsCollection extends com.trueaccord.scalapb.GeneratedMessageCompanion[org.telecomsudparis.smap.ResultsCollection] with com.trueaccord.scalapb.JavaProtoSupport[org.telecomsudparis.smap.ResultsCollection, pb.Smap.ResultsCollection] {
  implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[org.telecomsudparis.smap.ResultsCollection] with com.trueaccord.scalapb.JavaProtoSupport[org.telecomsudparis.smap.ResultsCollection, pb.Smap.ResultsCollection] = this
  def toJavaProto(scalaPbSource: org.telecomsudparis.smap.ResultsCollection): pb.Smap.ResultsCollection = {
    val javaPbOut = pb.Smap.ResultsCollection.newBuilder
    javaPbOut.addAllResults(scalaPbSource.results.map(org.telecomsudparis.smap.Result.toJavaProto)(_root_.scala.collection.breakOut).asJava)
    javaPbOut.build
  }
  def fromJavaProto(javaPbSource: pb.Smap.ResultsCollection): org.telecomsudparis.smap.ResultsCollection = org.telecomsudparis.smap.ResultsCollection(
    results = javaPbSource.getResultsList.asScala.map(org.telecomsudparis.smap.Result.fromJavaProto)(_root_.scala.collection.breakOut)
  )
  def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): org.telecomsudparis.smap.ResultsCollection = {
    require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
    val __fields = javaDescriptor.getFields
    org.telecomsudparis.smap.ResultsCollection(
      __fieldsMap.getOrElse(__fields.get(0), Nil).asInstanceOf[_root_.scala.collection.Seq[org.telecomsudparis.smap.Result]]
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[org.telecomsudparis.smap.ResultsCollection] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
      org.telecomsudparis.smap.ResultsCollection(
        __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.collection.Seq[org.telecomsudparis.smap.Result]]).getOrElse(_root_.scala.collection.Seq.empty)
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = SmapProto.javaDescriptor.getMessageTypes.get(4)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = SmapProto.scalaDescriptor.messages(4)
  def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = {
    var __out: _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = null
    (__number: @_root_.scala.unchecked) match {
      case 1 => __out = org.telecomsudparis.smap.Result
    }
    __out
  }
  lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
  def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = org.telecomsudparis.smap.ResultsCollection(
  )
  implicit class ResultsCollectionLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, org.telecomsudparis.smap.ResultsCollection]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, org.telecomsudparis.smap.ResultsCollection](_l) {
    def results: _root_.com.trueaccord.lenses.Lens[UpperPB, _root_.scala.collection.Seq[org.telecomsudparis.smap.Result]] = field(_.results)((c_, f_) => c_.copy(results = f_))
  }
  final val RESULTS_FIELD_NUMBER = 1
}
