package org.telecomsudparis.smap

object smapGrpc {
  val METHOD_EXECUTE_CMD: _root_.io.grpc.MethodDescriptor[org.telecomsudparis.smap.MapCommand, org.telecomsudparis.smap.ResultsCollection] =
    _root_.io.grpc.MethodDescriptor.newBuilder()
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("org.telecomsudparis.smap.pb.smap", "ExecuteCmd"))
      .setRequestMarshaller(new com.trueaccord.scalapb.grpc.Marshaller(org.telecomsudparis.smap.MapCommand))
      .setResponseMarshaller(new com.trueaccord.scalapb.grpc.Marshaller(org.telecomsudparis.smap.ResultsCollection))
      .build()
  
  val SERVICE: _root_.io.grpc.ServiceDescriptor =
    _root_.io.grpc.ServiceDescriptor.newBuilder("org.telecomsudparis.smap.pb.smap")
      .setSchemaDescriptor(new _root_.com.trueaccord.scalapb.grpc.ConcreteProtoFileDescriptorSupplier(org.telecomsudparis.smap.SmapProto.javaDescriptor))
      .addMethod(METHOD_EXECUTE_CMD)
      .build()
  
  trait smap extends _root_.com.trueaccord.scalapb.grpc.AbstractService {
    override def serviceCompanion = smap
    def executeCmd(request: org.telecomsudparis.smap.MapCommand): scala.concurrent.Future[org.telecomsudparis.smap.ResultsCollection]
  }
  
  object smap extends _root_.com.trueaccord.scalapb.grpc.ServiceCompanion[smap] {
    implicit def serviceCompanion: _root_.com.trueaccord.scalapb.grpc.ServiceCompanion[smap] = this
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor = org.telecomsudparis.smap.SmapProto.javaDescriptor.getServices().get(0)
  }
  
  trait smapBlockingClient {
    def serviceCompanion = smap
    def executeCmd(request: org.telecomsudparis.smap.MapCommand): org.telecomsudparis.smap.ResultsCollection
  }
  
  class smapBlockingStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT) extends _root_.io.grpc.stub.AbstractStub[smapBlockingStub](channel, options) with smapBlockingClient {
    override def executeCmd(request: org.telecomsudparis.smap.MapCommand): org.telecomsudparis.smap.ResultsCollection = {
      _root_.io.grpc.stub.ClientCalls.blockingUnaryCall(channel.newCall(METHOD_EXECUTE_CMD, options), request)
    }
    
    override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): smapBlockingStub = new smapBlockingStub(channel, options)
  }
  
  class smapStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT) extends _root_.io.grpc.stub.AbstractStub[smapStub](channel, options) with smap {
    override def executeCmd(request: org.telecomsudparis.smap.MapCommand): scala.concurrent.Future[org.telecomsudparis.smap.ResultsCollection] = {
      com.trueaccord.scalapb.grpc.Grpc.guavaFuture2ScalaFuture(_root_.io.grpc.stub.ClientCalls.futureUnaryCall(channel.newCall(METHOD_EXECUTE_CMD, options), request))
    }
    
    override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): smapStub = new smapStub(channel, options)
  }
  
  def bindService(serviceImpl: smap, executionContext: scala.concurrent.ExecutionContext): _root_.io.grpc.ServerServiceDefinition =
    _root_.io.grpc.ServerServiceDefinition.builder(SERVICE)
    .addMethod(
      METHOD_EXECUTE_CMD,
      _root_.io.grpc.stub.ServerCalls.asyncUnaryCall(new _root_.io.grpc.stub.ServerCalls.UnaryMethod[org.telecomsudparis.smap.MapCommand, org.telecomsudparis.smap.ResultsCollection] {
        override def invoke(request: org.telecomsudparis.smap.MapCommand, observer: _root_.io.grpc.stub.StreamObserver[org.telecomsudparis.smap.ResultsCollection]): Unit =
          serviceImpl.executeCmd(request).onComplete(com.trueaccord.scalapb.grpc.Grpc.completeObserver(observer))(
            executionContext)
      }))
    .build()
  
  def blockingStub(channel: _root_.io.grpc.Channel): smapBlockingStub = new smapBlockingStub(channel)
  
  def stub(channel: _root_.io.grpc.Channel): smapStub = new smapStub(channel)
  
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor = org.telecomsudparis.smap.SmapProto.javaDescriptor.getServices().get(0)
  
}