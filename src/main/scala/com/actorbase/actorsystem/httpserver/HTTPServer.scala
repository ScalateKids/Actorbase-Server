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

import com.actorbase.actorsystem.clientactor.ClientActor
import com.actorbase.actorsystem.main.Main

/**
  * Insert description here
  *
  * @param
  * @return
  * @throws
  */
class HTTPServer(main: ActorRef, listenPort: Int) extends Actor with ActorLogging {

  implicit val system = context.system
  IO(Http)(system) ! Http.Bind(self, interface = "127.0.0.1", port = listenPort)

  /**
    * Insert description here
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
  implicit val system = ActorSystem("actorbase")
  val main = system.actorOf(Props[Main])
  system.actorOf(Props(new HTTPServer(main, 9999)))
}
