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
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

import spray.http.HttpResponse
import spray.http._
import spray.util._

import scala.collection.mutable.ListBuffer

import com.actorbase.actorsystem.clientactor.messages.GetCollectionResponse

/**
  * Insert description here
  *
  * @param
  * @return
  * @throws
  */
class ClientActor(main: ActorRef) extends Actor with ActorLogging with RestApi with CollectionApi {

  implicit val timeout = Timeout(5 seconds)
  /** read-write collections list */
  private var collections: ListBuffer[String] = new ListBuffer[String]
  /** read-only collections list */
  private var readCollections: ListBuffer[String] = new ListBuffer[String]

  private var request = Map[String, Any]()

  private var client: ActorRef = _

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  // private area
  val login = pathPrefix("private") {
    authenticate(basicUserAuthenticator(ec, main)) { authInfo =>
      // only authenticated users can enter here
      get {
        complete{
          // bind the userkeeper to this clientActor
          val future = sender ? com.actorbase.actorsystem.userkeeper.Userkeeper.BindClient( self )
          // ugly as hell
          val result = Await.result(future, timeout.duration).asInstanceOf[Array[ListBuffer[String]]]
          collections = result(0)
          readCollections = result(1)
          log.info("RESTCLIENT ACTOR: received collections")
          (s"Private area: hi ${authInfo.user.login}")
        }
      }
    }
  }

  def handleResponses: Receive = {
    case m:GetCollectionResponse =>
      request ++= m.map
      if (request.size == 10)
        client ! HttpResponse(entity = HttpEntity(com.actorbase.actorsystem.clientactor.messages.MapResponse("customers", request).toString()))
  }

  def httpReceive: Receive = {
    client = sender
    runRoute(collections(main, "user") ~ route(main) ~ login)
  }

  def receive = handleResponses orElse httpReceive

}
