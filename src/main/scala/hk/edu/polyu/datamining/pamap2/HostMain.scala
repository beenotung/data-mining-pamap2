package hk.edu.polyu.datamining.pamap2

import hk.edu.polyu.datamining.pamap2.virtual.VirtualNetworkHelper

/**
  * Created by beenotung on 2/25/16.
  */
object HostMain extends App {
  def hasFlag(s: String): Boolean = args.exists(_.equals(s"--$s"))

  val ip = if (hasFlag("public")) HostIP.PublicIP else HostIP.LocalIP
  VirtualNetworkHelper.sentHostIPToVM(ip)
}
