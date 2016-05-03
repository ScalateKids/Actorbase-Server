package com.actorbase.actorsystem.clientactor

import akka.actor.ActorRef
import spray.httpx.SprayJsonSupport._
import spray.routing._
import akka.pattern.ask

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import com.actorbase.actorsystem.main.Main.{Testsk, Login, Testsf, BinTest, Insert, GetItemFrom, RemoveItemFrom, Testnj, CreateCollection, RemoveCollection}
import com.actorbase.actorsystem.clientactor.messages._

trait CollectionApi extends HttpServiceBase with Authenticator {

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
  def collections(main: ActorRef, owner: String): Route = {
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
    pathPrefix("collections" / "\\S+".r) { collection =>
      pathEndOrSingleSlash {
        get {
          complete {
            main.ask(GetItemFrom(collection))(5 seconds).mapTo[MapResponse]
          }
        } ~
        post {
          complete {
            main.ask(CreateCollection(collection, owner))
          }
        } ~
        delete {
          complete {
            main.ask(RemoveCollection(collection, owner))
          }
        }
      } ~
        pathSuffix("\\S+".r) { key =>
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
                      main ! Insert(owner, collection, key, value)
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
                      main ! Insert(owner, collection, key, value, true)
                      "Update complete"
                    }
                  }
                }
              }
            }
        }
    }
  }
}