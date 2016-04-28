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

package com.actorbase.actorsystem.clientactor

import akka.actor.ActorRef
import spray.httpx.SprayJsonSupport._
import spray.routing._
import akka.pattern.ask

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import com.actorbase.actorsystem.main.Main._

/**
  * Insert description here
  *
  * @param
  * @return
  * @throws
  */
trait RestApi extends HttpServiceBase with Authenticator {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  /**
    * HTTP routes mapped to handle CRUD operations, these should be nouns
    * (not verbs!) e.g.
    *
    * GET /collections                       - List all collections
    * GET /collections/customers             - Retrieve collection customers
    * POST /collections/customers            - Insert new customer
    * PUT /collections/customers/aracing     - Update
    * DELETE /collections/customers/aracing  - Delete key aracing from customers
    *
    */
  def route(main: ActorRef): Route = {

    /**
      * Collections route, manage all collection related operations, based
      * on the request received in the form of:
      *
      * GET collections/<collection>/<key>
      * Retrieve value associated to <key> from the collection <collection>
      *
      * POST collections/<collection>/<key>
      * Insert a value associated to <key> into the collection <collection>
      * contained inside a binary payload
      *
      * PUT collections/<collection>/<key>
      * Update a value associated to <key> into the collection <collection>
      * contained inside a binary payload
      *
      * DELETE collections/<collection>/<key>
      * remove an item of key <key> from the collection <collection>
      *
      * All routes return a standard marshallable of type Array[Byte]
      */
    path("collections" / "\\S+".r / "\\S+".r) { (collection, key) =>
      get {
        complete {
          main.ask(GetItemFrom(collection, key))(5 seconds).mapTo[Array[Byte]]
        }
      } ~
      delete {
        complete {
          main ! RemoveItemFrom(collection, key)
          "Remove complete"
        }
      } ~
      post {
        decompressRequest() {
          entity(as[Array[Byte]]) { value =>
            detach() {
              complete {
                main ! Insert(collection, key, value)
                "Insert complete"
              }
            }
          }
        }
      } ~
      put {
        decompressRequest() {
          entity(as[Array[Byte]]) { value =>
            detach() {
              complete {
                main ! Insert(collection, key, value, true)
                "Update complete"
              }
            }
          }
        }
      }
    } ~
    /* TEST ROUTES */
    // bin test
    path("actorbase" / "binary") {
      get {
        complete {
          main.ask(BinTest)(5 seconds).mapTo[Array[Byte]]
        }
      }
    } ~
    // test miniotta
    path("actorbase" / "find" / "\\S+".r) { resource =>
      get {
        complete {
          main.ask(Testsf(resource))(5 seconds).mapTo[Response]
        }
      }
    } ~
    path("actorbase" / "\\S+".r) { resource =>
      get {
        complete {
          main.ask(resource)(5 seconds).mapTo[Response]
        }
      }
    } ~
    //test route for sf and sk
    path("testStorekeeper"){
      get {
        complete {
          main.ask(Testsk)(5 seconds).mapTo[Response]
        }
      }
    } ~
    //test route for sf and sk
    path("testStorefinder"){
      get {
        complete {
          main.ask(Testsf)(5 seconds).mapTo[Response]
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
}
