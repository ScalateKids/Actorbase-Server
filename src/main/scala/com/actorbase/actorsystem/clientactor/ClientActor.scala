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
import spray.can.Http

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Try

// import spray.http.HttpResponse
// import spray.http._
// import spray.util._
// import HttpMethods._
// import spray.http.HttpHeaders._
// import spray.http.ContentTypes._

// import akka.routing.Broadcast
// import com.actorbase.actorsystem.utils.ActorbaseCollection
// import com.actorbase.actorsystem.main.Main.{Insert, GetItemFrom, RemoveItemFrom, CreateCollection, RemoveCollection}

import scala.collection.mutable.ListBuffer

// import com.actorbase.actorsystem.clientactor.messages.GetCollectionResponse


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

  private var client: Option[ActorRef] = None

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
        complete {
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

  def handleHttpRequests: Receive = {
    case _: Http.ConnectionClosed => Try(context.stop(self))
  }

  // def handleResponses: Receive = {
  //   case HttpRequest(GET, Uri.Path("/collections/customers"), _, _, _) =>
  //     client = Some(sender)
  //     var coll = new ActorbaseCollection("customers", "anonymous")
  //     // coll.setSize(100)
  //     main ! Broadcast(GetItemFrom(coll))

  //   case m:GetCollectionResponse =>
  //     request ++= m.map
  //     if (request.size == 50)
  //       client.get ! HttpResponse(entity = HttpEntity(request.toString()), headers = List(`Content-Type`(`application/json`)))
  // }

  def httpReceive: Receive = runRoute(collections(main, "anonymous") ~ route(main) ~ login)

  override def receive = handleHttpRequests orElse httpReceive

}
