package com.actorbase.actorsystem.clientactor

import akka.actor.ActorRef
import spray.httpx.SprayJsonSupport._
import spray.routing._
import akka.pattern.ask
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import com.actorbase.actorsystem.utils.ActorbaseCollection
import com.actorbase.actorsystem.main.Main.{Insert, GetItemFrom, RemoveItemFrom, CreateCollection, RemoveCollection}
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
            var coll = new ActorbaseCollection(collection, "anonymous")
            // coll.setSize(100)
            main.ask(ConsistentHashableEnvelope(message = GetItemFrom(coll), hashKey = coll.getName))(5 seconds).mapTo[MapResponse]
          }
        } ~
        post {
          complete {
            //TODO controllare se esiste già
            main ! (ConsistentHashableEnvelope(message = CreateCollection(collection, owner), hashKey = collection))
            "Create collection complete"
          }
        } ~
        delete {
          complete {
            //TODO controllare, se non esiste inutile mandare il messaggio
            main ! (ConsistentHashableEnvelope(message = RemoveCollection(collection, owner), hashKey = collection))
            "Remove collection complete"
          }
        }
      } ~
      pathSuffix("\\S+".r) { key =>
        get {
          complete {
            //TODO controllare, se collection non esiste, inutile instradare
            main.ask(ConsistentHashableEnvelope(message = GetItemFrom(new ActorbaseCollection(collection, "anonymous"), key), hashKey = collection))(5 seconds).mapTo[Response] // Array[Byte] -> Response for stress-test demo
          }
        } ~
        delete {
          complete {
            //TODO controllare, se collection non esiste, inutile instradare
            main ! (ConsistentHashableEnvelope(message = RemoveItemFrom(collection, key), hashKey = collection))
            "Remove complete"
          }
        } ~
        post {
          decompressRequest() {
            entity(as[Array[Byte]]) { value =>
              detach() {
                complete {
                  //TODO vedere se la collezione è presente, se non lo è mandare un createCollection
                  main ! (ConsistentHashableEnvelope(message = Insert(owner, collection, key, value), hashKey = collection))
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
                  //TODO vedere se la collezione è presente, se non lo è mandare un createCollection
                  main ! (ConsistentHashableEnvelope(message = Insert(owner, collection, key, value, true), hashKey = collection))
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
