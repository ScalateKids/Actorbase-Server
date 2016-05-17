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
  * @author Scalatekids TODO DA CAMBIARE
  * @version 1.0
  * @since 1.0
  */

package com.actorbase.actorsystem.httpserver

import akka.actor.{Actor, ActorSystem, ActorLogging, ActorRef, Props}
import akka.io.IO
import spray.can.Http
import akka.event.LoggingReceive

import akka.routing._
import akka.cluster._
import akka.cluster.routing._
import com.typesafe.config.ConfigFactory

import com.actorbase.actorsystem.clientactor.ClientActor
import com.actorbase.actorsystem.main.Main

/**
  * Insert description here
  *
  * @param
  * @return
  * @throws
  */
class HTTPServer(main: ActorRef, listenPort: Int) extends Actor
    with ActorLogging with SslConfiguration {

  implicit val system = context.system
  IO(Http)(system) ! Http.Bind(self, interface = "192.168.43.39", port = listenPort)

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
      val handler = context.actorOf(Props(new ClientActor(main)))
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
object HTTPServer extends App {
  val config = ConfigFactory.load()
  implicit val system = ActorSystem("actorbase", config)
  Cluster(system).registerOnMemberUp {
    val roundRobinPool = RoundRobinPool(nrOfInstances = 10)
    val clusterRoutingSettings = ClusterRouterPoolSettings(totalInstances = 10, maxInstancesPerNode = 5, allowLocalRoutees = true, useRole = None)
    val clusterPool = ClusterRouterPool(roundRobinPool, clusterRoutingSettings)
    val main = system.actorOf(clusterPool.props(Props[Main].withDispatcher("control-aware-dispatcher")))
    system.actorOf(Props(new HTTPServer(main, config getInt "exposed-port")) )
  }
}
