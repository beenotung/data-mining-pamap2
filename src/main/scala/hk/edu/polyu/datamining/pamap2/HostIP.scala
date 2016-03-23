package hk.edu.polyu.datamining.pamap2

import java.io.{BufferedReader, InputStreamReader}
import java.net.{InetAddress, NetworkInterface, URL, UnknownHostException}

import scala.collection.JavaConversions._
import scala.language.postfixOps

object HostIP {

  /**
    * @return the ip adress if it's a local adress (172.16.xxx.xxx, 172.31.xxx.xxx , 192.168.xxx.xxx, 10.xxx.xxx.xxx)
    */
  def load(): Option[String] = {
    val interfaces = NetworkInterface.getNetworkInterfaces
    val interface = interfaces find (_.getName equals "eth0")

    interface flatMap { inet =>
      // the docker adress should be siteLocal
      inet.getInetAddresses find (_ isSiteLocalAddress) map (_ getHostAddress)
    }
  }

  def all(): java.util.List[String] =
    NetworkInterface.getNetworkInterfaces
      .flatMap {
        _.getInetAddresses.map(_.getHostAddress)
      }
      .filterNot(ip => ip.contains("%"))
      .toList

  def hexToString(byte: Byte): String =
    byte match {
      case 10 => "A"
      case 11 => "B"
      case 12 => "C"
      case 13 => "D"
      case 14 => "E"
      case 15 => "F"
      case x: Byte => ("0".toByte + x).toChar + ""
    }

  def +(a: String, b: String) = a + b

  def PublicIP: String = {
    val reader = new BufferedReader(new InputStreamReader(new URL("http://icanhazip.com").openStream()))
    val ip = reader.readLine()
    reader.close()
    ip
  }

  def LocalIP: String = try {
    InetAddress.getLocalHost.getHostAddress
  } catch {
    case e: UnknownHostException => "127.0.0.1"
    case e: Throwable => throw e
  }
}