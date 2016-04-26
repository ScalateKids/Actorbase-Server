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

package com.actorbase.actorsystem.clientactor

import akka.actor.{Actor, ActorLogging, ActorRef}
import spray.httpx.SprayJsonSupport._
import spray.routing._
import akka.pattern.ask

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import com.actorbase.actorsystem.main.Main.Testsk
import com.actorbase.actorsystem.main.Main.Testsf

/**
  * Insert description here
  *
  * @param
  * @return
  * @throws
  */
class ClientActor(main: ActorRef) extends Actor
    with HttpServiceBase with ActorLogging with Authenticator {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  val route: Route = {
    path("actorbase" / "\\S+".r) { resource =>
      get {
        complete {
          log.info(s"Request for $resource")
          main.ask(resource)(5 seconds).mapTo[com.actorbase.actorsystem.main.Main.Response]
        }
      }
    } ~
    //test route for sf and sk
    path("testStorekeeper"){
      get {
        complete {
          log.info(s"Test storefinder e storekeeper")
          var l = main.ask(Testsk)(5 seconds).mapTo[com.actorbase.actorsystem.clientactor.messages.Response]
          println(l.toString)
        }
      }
    } ~
    //test route for sf and sk
    path("testStorefinder"){
      get {
        complete {
          log.info(s"Test storefinder e storekeeper")
          main.ask(Testsf)(5 seconds).mapTo[com.actorbase.actorsystem.clientactor.messages.Response]
        }
      }
    } ~
    // private area
    pathPrefix("private") {
      authenticate(basicUserAuthenticator(ec, main)) { authInfo =>
        // only authenticated users can enter here
        get {
          complete(s"Private area: hi ${authInfo.user.login}")
        }
      }
    }
  }

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  def receive = {
    runRoute(route)
  }

}
