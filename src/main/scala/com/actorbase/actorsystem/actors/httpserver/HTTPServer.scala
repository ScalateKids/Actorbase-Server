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
import com.actorbase.actorsystem.messages.AuthActorMessages.{ AddCollectionTo, UpdateCredentials }
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
  * Class that represent a HTTPServer actor. This actor is responsible to accept the connection 
  * incoming from clients and to instantiate a ClientActor assigned to the client asking. 
  *
  * @param main: an ActorRef to a main actor
  * @param authProxy: an ActorRef to the AuthActor
  * @param addess: a String representing the address on which actorbase has to listen on
  * @param listenPort: a Int representing the port on which actorbase has to listen on
  */
class HTTPServer(main: ActorRef, authProxy: ActorRef, address: String, listenPort: Int) extends Actor
    with ActorLogging with SslConfiguration {

  implicit val system = context.system
  IO(Http)(system) ! Http.Bind(self, interface = address, port = listenPort)

  val initLoad: Unit = loadData

  /**
    * Loads all the data saved on the rootfolder onto the system. This is used to repopulate
    * the database after a restart.
    * The actor reads all the data from files and it proceed to send messages to the right actors
    * to repopulate the server.
    *
    * @return no return value
    */
  def loadData: Unit = {
    import com.actorbase.actorsystem.utils.CryptoUtils
    import java.io.File

    val root = new File("actorbasedata/")
    var dataShard = Map[String, Any]().empty
    var usersmap = Map[String, String]().empty
    var contributors = Map.empty[String, Set[ActorbaseCollection]]
    var data = Map.empty[ActorbaseCollection, Map[String, Any]]
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
                  main ! CreateCollection(ActorbaseCollection(name, owner))
                case user if (user.getName == "usersdata.shadow") =>
                  usersmap ++= CryptoUtils.decrypt[Map[String, String]]("Dummy implicit k", user)
                case contributor if (contributor.getName == "contributors.shadow") =>
                  contributors ++= CryptoUtils.decrypt[Map[String, Set[ActorbaseCollection]]]("Dummy implicit k", contributor)
                case _ => dataShard ++= CryptoUtils.decrypt[Map[String, Any]]("Dummy implicit k", x)
              }
            }
          }
          val collection = new ActorbaseCollection(name, owner)
          data += (collection -> dataShard)
          contributors.foreach {
            case (k, v) =>
              v.foreach (entry => authProxy ! AddCollectionTo(k, entry)) // check and remove cast
          }
          dataShard = dataShard.empty
          contributors = contributors.empty
        }
      }
      data.foreach {
        case (k, v) =>
          v.foreach {
            case (kk, vv) =>
              main ! InsertTo(k.getOwner, k, kk, vv.asInstanceOf[Array[Byte]], false)
          }
      }
      dataShard = dataShard.empty

      def getRecursively(f: File): Seq[File] = f.listFiles.filter(_.isDirectory).flatMap(getRecursively) ++ f.listFiles
      getRecursively( root ).foreach { f =>
        if (!f.getName.endsWith("shadow") && f.getName != "usersdata")
          f.delete()
      }

      usersmap map { x =>
        if (x._1 != "admin")
          authProxy ! AddCredentials(x._1, x._2)
        else authProxy ! UpdateCredentials(x._1, "Actorb4se", x._2)
      }

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
  * HTTPServer object, it contains the main of the application
  */
object HTTPServer {
  def main(args: Array[String]) = {
    val (hostname, port) =
      if (args.nonEmpty)
        (args(0), args(1))
      else {
       // println("errore")
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
