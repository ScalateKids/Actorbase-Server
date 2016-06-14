/**
  * The MIT License (MIT)
  * <p/>
  * Copyright (c) 2016 ScalateKids
  * <p/>
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  * <p/>
  * The above copyright notice and this permission notice shall be included in all
  * copies or substantial portions of the Software.
  * <p/>
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  * SOFTWARE.
  * <p/>
  *
  * @author Scalatekids TODO DA CAMBIARE
  * @version 1.0
  * @since 1.0
  */

package com.actorbase.actorsystem.actors.httpserver

import akka.actor.{Actor, ActorSystem, ActorLogging, ActorRef, PoisonPill, Props}
import akka.io.IO
import com.actorbase.actorsystem.messages.AuthActorMessages.AddCollectionTo
import spray.can.Http
import akka.event.LoggingReceive

import akka.cluster._
import akka.cluster.routing._
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import com.typesafe.config.ConfigFactory

import com.actorbase.actorsystem.actors.clientactor.ClientActor
import com.actorbase.actorsystem.actors.authactor.AuthActor
import com.actorbase.actorsystem.actors.main.Main
import com.actorbase.actorsystem.messages.MainMessages._
import com.actorbase.actorsystem.utils.ActorbaseCollection
import com.actorbase.actorsystem.messages.AuthActorMessages.AddCredentials

/**
  * Insert description here
  *
  * @param
  * @return
  * @throws
  */
class HTTPServer(main: ActorRef, authProxy: ActorRef, address: String, listenPort: Int) extends Actor
    with ActorLogging with SslConfiguration {

  implicit val system = context.system
  IO(Http)(system) ! Http.Bind(self, interface = address, port = listenPort)

  val initLoad: Unit = loadData

  /**
    *
    */
  def loadData: Unit = {
    import com.actorbase.actorsystem.utils.CryptoUtils
    import java.io.File

    val root = new File("actorbasedata/")
    var dataShard = Map[String, Array[Byte]]().empty
    var contributors = Map.empty[String, Set[ActorbaseCollection]]

    println("\n LOADING ......... ")
    if (root.exists && root.isDirectory) {
      var (name, owner) = ("", "")
      root.listFiles.filter(_.isDirectory).foreach {
        x => {
          x.listFiles.filter(_.isFile).foreach {
            x => {
              x match {
                case meta if meta.getName.endsWith("actbmeta") =>
                  val metaData = CryptoUtils.decrypt[Map[String, Any]]("Dummy implicit k", meta)
                  metaData get "collection" map (c => name = c.asInstanceOf[String])
                  metaData get "owner" map (o => owner = o.toString())
                  // name = metaData.get("collection").get.asInstanceOf[String]
                  // owner = metaData.get("owner").get.toString
                  main ! CreateCollection(ActorbaseCollection(name, owner))
                case user if (user.getName == "userdata.shadow") =>
                  CryptoUtils.decrypt[Map[String, String]]("Dummy implicit k", user) map { x => if (x._1 != "admin") authProxy ! AddCredentials(x._1, x._2) }
                case contributor if (contributor.getName == "contributors.shadow") =>
                  // contributors ++= CryptoUtils.decrypt[Map[String, Set[ActorbaseCollection]]]("Dummy implicit k", contributor)
                case _ => dataShard ++= CryptoUtils.decrypt[Map[String, Array[Byte]]]("Dummy implicit k", x)
              }
            }
          }
          val collection = new ActorbaseCollection(name, owner)
          dataShard.foreach {
            case (k, v) =>
              main ! InsertTo(owner, collection, k, v, false) // check and remove cast
          }
          // contributors.foreach {
          //   case (k, v) =>
          //     v.foreach (entry => authProxy ! AddCollectionTo(k, entry)) // check and remove cast
          // }
          dataShard = dataShard.empty
          contributors = contributors.empty
        }
      }
      // should probably delete actorbasedata here
      root.delete
    } else log.warning("Directory not found!")
  }

  /**
    * Receive method, handle connection from outside, registering it to a
    * dedicated actor
    *
    * @param
    * @return
    * @throws
    */
  def receive: Receive = LoggingReceive {
    case _: Http.Connected =>
      val serverConnection = sender()
      val handler = context.actorOf(Props(new ClientActor(main, authProxy)))
      serverConnection ! Http.Register(handler)
  }

}

/**
  * Insert description here
  *
  * @param
  * @return
  * @throws
  */
object HTTPServer {
  def main(args: Array[String]) = {
    val (hostname, port) =
      if (args.nonEmpty)
        (args(0), args(1))
	  else {
		println("errore")
        ("127.0.0.1", 2500)
		}
    val config = ConfigFactory.parseString(s"""
akka.remote.netty.tcp.hostname=${hostname}
akka.remote.netty.tcp.port=${port}
listen-on=${hostname}
""").withFallback(ConfigFactory.load())
    val system = ActorSystem(config getString "name", config)
    // singleton authactor
    system.actorOf(ClusterSingletonManager.props(
      singletonProps = Props(classOf[AuthActor]),
      terminationMessage = PoisonPill,
      settings = ClusterSingletonManagerSettings(system)),
      name = "authactor")
    // proxy
    val authProxy = system.actorOf(ClusterSingletonProxy.props(
      singletonManagerPath = "/user/authactor",
      settings = ClusterSingletonProxySettings(system)),
      name = "authProxy")
    // main sharding
    ClusterSharding(system).start(
      typeName = Main.shardName,
      entityProps = Main.props(authProxy),
      settings = ClusterShardingSettings(system),
      extractShardId = Main.extractShardId,
      extractEntityId = Main.extractEntityId)

    val main = ClusterSharding(system).shardRegion(Main.shardName)
    val http = system.actorOf(Props(classOf[HTTPServer], main, authProxy, config getString "listen-on", config getInt "exposed-port"))
  }
}
