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
import spray.can.Http

import scala.util.Try

import com.actorbase.actorsystem.messages.AuthActorMessages.{AddCredentials, RemoveCredentials}


/**
  * Insert description here
  *
  * @param
  * @return
  * @throws
  */
class ClientActor(main: ActorRef, authProxy: ActorRef) extends Actor with ActorLogging with RestApi with CollectionApi {

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  // private area
  val adminDirectives = {
    /**
      * User management route, only administrator users can enter here and make
      * operations, a GET request equals listing all users of the system
      */
    pathPrefix("users") {
      authenticate(basicUserAuthenticator(ec, authProxy)) { authInfo =>
        get {
          authorize(authInfo.hasAdminPermissions) {
            // only admin users can enter here
            complete {
              "list users"
            }
          }
        }
      }
    }
  } ~
  pathSuffix("\\S+".r) { user =>
    /**
      * user/<username> a POST request to this route equals adding a new user
      * to the system with username <username> and password as request payload
      */
    authenticate(basicUserAuthenticator(ec, authProxy)) { authInfo =>
      post {
        decompressRequest() {
          entity(as[String]) { value =>
            detach() {
              authorize(authInfo.hasAdminPermissions) {
                // only admin users can enter here
                authProxy ! AddCredentials(user, value)
                complete {
                  s"added user $user"
                }
              }
            }
          }
        }
      } ~
      put {
        /**
          * user/<username> a PUT request to this route equals updating an
          * existing user of username <username>
          */
        decompressRequest() {
          entity(as[String]) { value =>
            detach() {
              authorize(authInfo.hasAdminPermissions) {
                // only admin users can enter here
                authProxy ! AddCredentials(user, value)
                complete {
                  s"updated user $user"
                }
              }
            }
          }
        }
      } ~
      delete {
        /**
          * user/<username> a DELETE request to this route equals removing an existing user
          * from the system
          */
        authorize(authInfo.hasAdminPermissions) {
          // only admin users can enter here
          authProxy ! RemoveCredentials(user)
          complete {
            s"removed user $user"
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
  def httpReceive: Receive = runRoute(collectionsDirectives(main, authProxy) ~ route(main) ~ adminDirectives)

  override def receive = handleHttpRequests orElse httpReceive

}
