package org.telecomsudparis.smap

import java.util.concurrent.{Executors, TimeUnit}
import java.util.logging.Logger
import java.util.concurrent.{ExecutorService, Executors, TimeUnit}

import com.codahale.metrics.{ConsoleReporter, MetricAttribute}
import io.grpc.netty.NettyServerBuilder
import io.grpc.{Server, ServerBuilder}
import io.netty.channel.socket.nio.NioServerSocketChannel

class SMapServiceServer(server: Server) extends nl.grons.metrics4.scala.DefaultInstrumented {
  val logger: Logger = Logger.getLogger(classOf[SMapServiceServer].getName)

  private[this] val loading = metrics.timer("loading")

  def start(): Unit = {
    server.start()
    logger.info(s"MGB-SMap Server started, listening on ${server.getPort}")
    sys.addShutdownHook {
      // Use stderr here since the logger may has been reset by its JVM shutdown hook.
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      stop()
      System.err.println("*** server shut down")
    }
    ()
  }
  def stop(): Unit = {
    server.shutdown()
  }

  /**
    * Await termination on the main thread since the grpc library uses daemon threads.
    */
  def blockUntilShutdown(): Unit = {
    server.awaitTermination()
  }

}

object SMapServiceServer extends App {
  val parser = new scopt.OptionParser[ServerConfig]("SMapServiceServer") {
    head("SMapServiceServer", "0.1-SNAPSHOT")
    opt[Int]("serverPort").abbr("sp").action( (x, c) =>
      c.copy(serverPort = x) ).text("Server Port is an integer property. Default: 8980")

    opt[Int]("retries").abbr("rt").action( (x, c) =>
      c.copy(retries = x) ).text("Zookeeper Connection Retries is an integer property. Default: 300")

    opt[Boolean]("localReads").abbr("lr").action( (x, c) =>
      c.copy(lReads = x) ).text("Local Reads is an boolean property. Default: true")

    opt[Boolean]("verbosity").abbr("vb").action( (x, c) =>
      c.copy(verbosity = x) ).text("Verbosity is an boolean property. Default: true")

    opt[Boolean]("static").abbr("st").action( (x, c) =>
      c.copy(static = x) ).text("Static is an boolean property. Default: true")

    opt[String]("zookeeperHost").abbr("zkh").action( (x, c) =>
      c.copy(zkHost = x) ).text("Default is the string: 127.0.0.1")

    opt[String]("zookeeperPort").abbr("zkp").action( (x, c) =>
      c.copy(zkPort = x) ).text("Default is the string: 2181")

    opt[String]("timeStamp").abbr("ts").action( (x, c) =>
      c.copy(timeStamp = x) ).text("Default is the string: undefined")

    help("help").text("prints this usage text")
  }
  //var pool1: ExecutorService = Executors.newFixedThreadPool(128)

  trait Instrumented extends nl.grons.metrics4.scala.InstrumentedBuilder {
    val metricRegistry = SMapServiceServer.metricRegistry
  }

  var reporterSettings = scala.collection.JavaConverters.setAsJavaSet(
    Set(MetricAttribute.MAX, MetricAttribute.STDDEV,
      MetricAttribute.M1_RATE, MetricAttribute.M5_RATE,
      MetricAttribute.M15_RATE, MetricAttribute.MIN,
      MetricAttribute.P99, MetricAttribute.P50,
      MetricAttribute.P75, MetricAttribute.P95,
      MetricAttribute.P98, MetricAttribute.P999)
  )

  val metricRegistry = {
    val registry = new com.codahale.metrics.MetricRegistry()
    ConsoleReporter.forRegistry(registry)
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MICROSECONDS)
      .disabledMetricAttributes(reporterSettings)
      .build()
      .start(10, TimeUnit.SECONDS) //changed the interval to seconds instead of minutes
    registry
  }

  // parser.parse returns Option[C]
  parser.parse(args, ServerConfig()) match {
    case Some(config) =>

      var serverSMap = new SMapServer(localReads = config.lReads,
        verbose = config.verbosity,
        Array("-zk=" + config.zkHost + ":" + config.zkPort), retries = config.retries, staticConnection = config.static)

      var clientSMap = new SMapClient(verbose = config.verbosity,
        mapServer = serverSMap)

      var serverBuilder = NettyServerBuilder.forPort(config.serverPort)
      serverBuilder
        .addService(
          smapGrpc.bindService(
            new SMapService(serverSMap, clientSMap),
            scala.concurrent.ExecutionContext.fromExecutor(Executors.newFixedThreadPool(256))
          )
        )
      serverBuilder.executor(Executors.newFixedThreadPool(256))

      val server = new SMapServiceServer(serverBuilder.build())
      server.start()
      serverSMap.serverInit()
      server.blockUntilShutdown()
    case None =>
    // arguments are bad, error message will have been displayed
  }


}
