package org.telecomsudparis.smap
import java.io.File

case class ServerConfig(lReads: Boolean = true, verbosity: Boolean = true, zkHost: String = "127.0.0.1",
                  zkPort: String = "2181",
                  timeStamp: String = "undefined", serverPort: Int = 8980, retries: Int = 300, static: Boolean = false)

case class ClientConfig(zkHost: String = "127.0.0.1", zkPort: String = "2181",
                  timeStamp: String = "undefined", serverPort: Int = 8980, host: String = "")
