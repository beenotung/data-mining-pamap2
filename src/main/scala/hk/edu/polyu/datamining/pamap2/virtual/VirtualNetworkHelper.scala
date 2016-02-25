package hk.edu.polyu.datamining.pamap2.virtual

import java.io.PrintWriter
import java.net.{ServerSocket, Socket}

import com.typesafe.config.ConfigFactory

import scala.io.Source
import scala.util.control.Breaks

/**
  * Created by beenotung on 2/25/16.
  */
object VirtualNetworkHelper {
  lazy val config = ConfigFactory parseResources "node.compute.conf"
  lazy val host_port = config getInt "virtualnetwork.host.port"
  lazy val guest_port = config getInt "virtualnetwork.guest.port"

  /** REMARK : block and wait **/
  def sentHostIPToVM(hostIP: String) = {
    println(s"starting to find program in guest os")
    val loop = new Breaks()
    loop.breakable({
      while (true) {
        try {
          val socket = new Socket(hostIP, guest_port)
          println("connected to guest system")
          val out = new PrintWriter(socket.getOutputStream)
          out.println(hostIP)
          out.close()
          socket.close()
        } catch {
          case e: Exception =>
            println(e)
        }
      }
    })
  }

  /** REMARK : block and wait **/
  def getHostIPFromVM: String = {
    println(s"waiting host connection on port $guest_port")
    val serverSocket = new ServerSocket(guest_port)
    var hostIP = ""
    val loop = new Breaks()
    loop.breakable({
      while (true) {
        try {
          val socket = serverSocket.accept()
          val in = Source.fromInputStream(socket.getInputStream)
          hostIP = in.getLines().toIndexedSeq(0)
        } catch {
          case e: Exception => println(e)
        }
      }
    })
    println(s"resolved host ip : $hostIP")
    hostIP
  }

}
