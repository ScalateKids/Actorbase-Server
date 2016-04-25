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

package com.actorbase.actorsystem.restclientactor

import akka.actor.{Actor, ActorLogging, ActorRef}
//import spray.can.Http     intellij dice che sono inutili, provo a commentare
import spray.httpx.SprayJsonSupport._
import spray.routing._
import akka.pattern.ask
//import akka.util.Timeout  intellij dice che sono inutili, provo a commentare

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import com.actorbase.actorsystem.main.Main.Response
import com.actorbase.actorsystem.main.Main.Testsk

/**
  * Insert description here
  *
  * @param
  * @return
  * @throws
  */
class RestClientActor(main: ActorRef) extends Actor with  HttpServiceBase with ActorLogging {
  val route: Route = {
    path("actorbase" / "\\S+".r) { resource =>
      get {
        complete {
          log.info(s"Request for $resource")
          main.ask(resource)(5 seconds).mapTo[Response]
        }
      }
    }~
    //test route for sf and sk
    path("testStorefinder".r){ resource =>
      get {
        complete {
          log.info(s"Test storefinder e storekeeper")
          main.ask(Testsk)(5 seconds).mapTo[Response]
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
  def receive = runRoute(route)

}
