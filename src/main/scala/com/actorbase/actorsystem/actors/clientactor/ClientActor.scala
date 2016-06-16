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
  * @author Scalatekids   * @version 1.0
  * @since 1.0
  */

package com.actorbase.actorsystem.actors.clientactor

import akka.actor.{ Actor, ActorLogging, ActorRef }
import akka.pattern.ask
import com.actorbase.actorsystem.messages.AuthActorMessages.{ Authenticate, UpdateCredentials }
import spray.can.Http
import spray.http.HttpHeader
import spray.httpx.SprayJsonSupport._

import scala.concurrent.duration._
import scala.util.Try

import com.actorbase.actorsystem.messages.ClientActorMessages.ListResponse
import com.actorbase.actorsystem.messages.AuthActorMessages.{ AddCredentials, RemoveCredentials, ListUsers }


/**
  * Class that represents a ClientActor. This actor has to maintain a connection with the client
  * and it receives all the requests from it, he dispatch this requests to an actor responsable
  * to fullfil it
  */
class ClientActor(main: ActorRef, authProxy: ActorRef) extends Actor with ActorLogging with CollectionApi {

  /**
    * Directives for authentication routes, these lets the management of authentication
    * and personal credentials
    */
  val authDirectives = {
    pathPrefix("auth" / "\\S+".r) { user =>
      post {
        decompressRequest() {
          entity(as[Array[Byte]]) { value =>
            detach() {
              complete {
                (authProxy ? Authenticate(user, new String(value, "UTF-8"))).mapTo[String]
              }
            }
          }
        }
      }
    }
  } ~
  pathPrefix("private" / "\\S+".r) { user =>
    post {
      decompressRequest() {
        headerValueByName("Old-Password") { oldpw =>
          entity(as[Array[Byte]]) { value =>
            detach() {
              complete {
                  (authProxy ? UpdateCredentials(user, oldpw, new String(value, "UTF-8"))).mapTo[String]
              }
            }
          }
        }
      }
    }
  }

  /**
    * Directives for administrator users routes, these lets the management of the users of the system,
    * like creation of new user and deletion of existing ones.
    */
  // private area
  val adminDirectives = {
    /**
      * User management route, only administrator users can enter here and make
      * operations, a GET request equals listing all users of the system
      */
    pathPrefix("users") {
      pathEndOrSingleSlash {
        authenticate(basicUserAuthenticator(ec, authProxy)) { authInfo =>
          get {
            authorize(authInfo.hasAdminPermissions) {
              // only admin users can enter here
              complete {
                (authProxy ? ListUsers).mapTo[ListResponse]
              }
            }
          }
        }
      }
    }
  } ~
  pathPrefix("users" / "\\S+".r) { user =>
    /**
      * user/<username> a POST request to this route equals adding a new user
      * to the system with username <username> and password as request payload
      */
    pathEndOrSingleSlash {
      authenticate(basicUserAuthenticator(ec, authProxy)) { authInfo =>
        authorize(authInfo.hasAdminPermissions) {
          post {
            // only admin users can enter here
            complete {
              val value = "Actorb4se"
                (authProxy ? AddCredentials(user, value)).mapTo[String]
            }
          } ~
          put {
            /**
              * user/<username> a PUT request to this route equals updating an
              * existing user of username <username>
              */
            // only admin users can enter here
            complete {
              val value = "Actorb4se"
                (authProxy ? UpdateCredentials(user, value, value)).mapTo[String]
            }
          } ~
          delete {
            /**
              * user/<username> a DELETE request to this route equals removing an existing user
              * from the system
              */
            // only admin users can enter here
            complete {
              (authProxy ? RemoveCredentials(user)).mapTo[String]
            }
          }
        }
      }
    }
  }

  /**
    * Handle http special request, e.g. <code>ConnectionClosed</code>, trying to
    * stop this actor
    */
  def handleHttpRequests: Receive = {
    case _: Http.ConnectionClosed => Try(context.stop(self))
  }

  /**
    * Handle all directives to manage and query the system
    */
  def httpReceive: Receive = runRoute(collectionsDirectives(main, authProxy) ~ authDirectives ~ adminDirectives)

  /**
    * Overrides of the receive method of the Akka Actor class
    */
  override def receive = handleHttpRequests orElse httpReceive

}
