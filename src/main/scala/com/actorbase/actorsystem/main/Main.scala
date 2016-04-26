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

package com.actorbase.actorsystem.main

import akka.actor.{Actor, ActorLogging, ActorRef}
import spray.json.DefaultJsonProtocol._
import com.actorbase.actorsystem.storefinder.Storefinder
import com.actorbase.actorsystem.storefinder.messages._

/**
  * Insert description here
  *
  * @param
  * @return
  * @throws
  */
object Main {
  case class Response(response: String)
  case object Response {
    implicit val goJson = jsonFormat1(Response.apply)
  }
  case class Testsk()
  case class Testsf(clientRef: ActorRef)
}

/**
  * Insert description here
  *
  * @param
  * @return
  * @throws
  */
class Main extends Actor with ActorLogging {
  import Main._

/**
  * Insert description here
  *
  * @param
  * @return
  * @throws
  */
  def receive = {

    case resource: String =>
      log.info(s"$resource request")
      sender ! Response(resource)

    case Testsk =>{
      val sf = context.actorOf(Storefinder.props())
      sf ! Init
      sf ! GetItem("")
      sf ! GetItem("test")
      sf ! Insert("chiave", "valore")
      sf ! RemoveItem("rimuovi")
      sf ! DuplicateRequest
      sender ! Response("test successful")
    }

    case Testsf => {
      val sf = context.actorOf(Storefinder.props())
      for(i <- 0 to 30){
        sf ! Insert("chiave"+i , "valore"+i)
      }
      sf ! GetItem("chiave5", self)
      sf ! RemoveItem("chiave5", self)
      sf ! GetItem("chiave5", self)
      sender ! Response("test successful.... Magari")
    }

    case _ => log.info("Still waiting for an ordination")

  }

}
