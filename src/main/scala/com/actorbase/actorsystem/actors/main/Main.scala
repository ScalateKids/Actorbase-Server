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
  * @author Scalatekids
  * @version 1.0
  * @since 1.0
  */

package com.actorbase.actorsystem.actors.main

import akka.actor.{ Actor, ActorLogging, ActorRef, OneForOneStrategy, PoisonPill, Props }
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.{ExtractEntityId, ExtractShardId}
import akka.actor.SupervisorStrategy._

import com.actorbase.actorsystem.messages.AuthActorMessages.{ AddCollectionTo, RemoveCollectionFrom }
import com.actorbase.actorsystem.actors.storefinder.Storefinder
import com.actorbase.actorsystem.utils.ActorbaseCollection
import com.actorbase.actorsystem.messages.MainMessages._
import com.actorbase.actorsystem.messages.StorefinderMessages._
import com.actorbase.actorsystem.messages.ClientActorMessages.MapResponse

import scala.collection.mutable
import scala.concurrent.duration._

/**
  * Class that represents a Main actor. This actor is responsible of managing
  * incoming requests.
  */
object Main {

  /**
    * Props method, used to build an instance of Main actor
    * @param authProxy ActorRef representing the Authenticator actor that will be used by the Main actor
    * @return an object of type Props, usable directly with an actorsystem running
    */
  def props(authProxy: ActorRef) = Props(classOf[Main], authProxy)

  /** name of the sharded entity */
  def shardName = "mainActor"

  /**
    * ExtractShardId is a function needed by akka's cluster sharding extension in order to
    * retrieve shard region ids while addressing messages between sharded actors
    *
    * @return a String representing an UUID of a shard-region where the actor belongs to
    */
  val extractShardId: ExtractShardId = {
    case CreateCollection(_, collection) => (collection.getUUID.hashCode % 30).toString
    case RemoveFrom(_, uuid, _) => (uuid.hashCode % 30).toString
    case InsertTo(_,collection, _, _, _) => (collection.getUUID.hashCode % 30).toString
    case GetFrom(_,collection, _) => (collection.getUUID.hashCode % 30).toString
    case AddContributor(_, _, _, uuid) => (uuid.hashCode % 30).toString
    case RemoveContributor(_, _, uuid) => (uuid.hashCode % 30).toString
  }

  /**
    * ExtractEntityId is a function needed by akka's cluster sharding extension in order to
    * retrieve entity actors ids while addressing messages
    *
    * @return a String representing an UUID of an entity actor inside a shard-region
    */
  val extractEntityId: ExtractEntityId = {
    case msg: CreateCollection => (msg.collection.getUUID, msg)
    case msg: RemoveFrom => (msg.uuid, msg)
    case msg: InsertTo => (msg.collection.getUUID, msg)
    case msg: GetFrom => (msg.collection.getUUID, msg)
    case msg: AddContributor => (msg.uuid, msg)
    case msg: RemoveContributor => (msg.uuid, msg)
  }

}

/**
  * Class that represents a Main actor. This actor is responsible of managing
  * incoming requests.
  */
class Main(authProxy: ActorRef) extends Actor with ActorLogging {

  private var sfMap = Map[ActorbaseCollection, ActorRef]().empty
  private var requestMap = Map[String, mutable.Map[String, mutable.Map[String, Array[Byte]]]]() // a bit clunky, should switch to a queue



  /**
    * Method that overrides the supervisorStrategy method.
    */

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: Exception => Resume
    }

  /**
    * Method that creates a collection in Actorbase.
    *
    * @param collection an ActorbaseCollection representing the collection that needs to be created
    * @return an ActorRef pointing to the Storefinder just created that maps the collection
    */
  private def createCollection(collection: ActorbaseCollection): Option[ActorRef] = {
    if (sfMap.contains(collection))
      sfMap get collection
    else {
      if (collection.getOwner != "admin") {
        authProxy ! AddCollectionTo("admin", collection)
        collection.addContributor("admin", ActorbaseCollection.ReadWrite)
      }
      log.info(s"creating ${collection.getName} for ${collection.getOwner}")
      val sf = context.actorOf(Storefinder.props(collection))
      sfMap += (collection -> sf)
      authProxy ! AddCollectionTo(collection.getOwner, collection)
      Some(sf)
    }
  }

  /**
    * Method that creates a collection in Actorbase.
    *
    * @return an Int value representing the number of Storefinders contained in this Main actor
    */

  def getSize(): Int = sfMap.size

  /**
    * Receive method of the Main actor, it does different things based on the message it receives:<br>
    * _InsertTo: when the actor receives this message it inserts the item in the collection requested by the user.<br>
    * _CreateCollection: when the actor receives this message it creates the collection inserted by the user <br>
    * _GetFrom: when the actor receives this message it sends back the item requested by the user<br>
    * _CompleteTransaction: when the actor receives this message it awaits for Storefinder response of all storekeepers, expecting
    * a given number of response, equals to the number of key-value pairs of the collection requested<br>
    * _RemoveFrom: when the actor receives this message it removes the item requested by the user<br>
    * _AddContributor: when the actor receives this message it adds the specified user to the contributor list of the defined by the user<br>
    * _RemoveContributor: when the actor receives this message it removes the specified user to the contributor list of the defined by the user<br>
    *
    */

  def receive: Receive = {

    case message: MainMessage => message match {

      /**
        * Insert message, insert a key/value into a designed collection, searching
        * if sfMap contains the collection I need, if it's present search for the
        * right keyrange
        *
        * @param owner a String representing the owner of the collection
        * @param name a String representing the collection name
        * @param key a String representing the new key to be inserted
        * @param value a Any object type representing the value to be inserted
        * with associated key, default to Array[Byte] type
        * @param update a Boolean flag, define the insert behavior (with or without
        * updating the value)
        */
      case InsertTo(requester, collection, key, value, update) =>
        log.info("MAIN: got work!")
        sfMap.find(x => x._1 == collection) map { c =>
          if (requester == c._1.getOwner || c._1.containsReadWriteContributor(requester))
            c._2 forward Insert(key, value, update)
        } getOrElse (
          createCollection(collection) map (_ forward Insert(key, value, update)) getOrElse sender ! "UndefinedCollection") // perhaps-fix

      /**
        * Create a collection in the system
        *
        * @param name a String representing the name of the collection
        * @param owner a String representing the owner of the collection
        */
      case CreateCollection(requester, collection) =>
        if (requester == "admin" || requester == collection.getOwner) {
          createCollection(collection)
          sender ! "OK"
        } else sender ! "NoPrivileges"

      /**
        * Get item from collection message, given a key of type String, retrieve
        * a value from a specified collection, if key is empty, remove the
        * entire collection
        *
        * @param collection a String representing the collection name
        * @param key a String representing the key to be retrieved
        */
      case GetFrom(requester, collection, key) =>
        if (key.nonEmpty)
          sfMap.find(_._1 == collection) map { c =>
            if (c._1.getOwner == requester || c._1.containsReadWriteContributor(requester) || c._1.containsReadContributor(requester))
              c._2 forward Get(key)
            else sender ! Left("NoPrivileges")
          } getOrElse sender ! Left("UndefinedCollection")
        else {
          // WIP: still completing
          sfMap.find(_._1 == collection) map { coll =>
            if (coll._1.getOwner == requester || coll._1.containsReadWriteContributor(requester) || coll._1.containsReadContributor(requester)) {
              requestMap.find(_._1 == coll._1.getOwner) map (_._2 += (coll._1.getUUID -> mutable.Map[String, Array[Byte]]())) getOrElse (
                requestMap += (collection.getOwner -> mutable.Map(coll._1.getUUID -> mutable.Map[String, Array[Byte]]())))
              if (coll._1.getSize > 0)
                sfMap get collection map (_ forward GetAllItems) getOrElse sender ! Left("UndefinedCollection")
              else
                sender ! Right(MapResponse(collection.getOwner, collection.getName, Map[String, Array[Byte]]()))
            } else sender ! Left("UndefinedCollection")
          } getOrElse sender ! Left("UndefinedCollection")
        }

      /**
        * Await for storefinder response of all storekeeper, expecting
        * a given number of response, equals to the number of key-value pairs
        * of the collection requested
        *
        * @param clientRef the reference of the client demanding the collection
        * @param collection an ActorbaseCollection item containing the number of response
        * expected for the requested collection at the current state
        * @param items a TreeMap[String, Any] representing a shard of the requested collection, represent a storekeeper payload
        * @return
        * @throws
        */
      case CompleteTransaction(clientRef, collection, items) =>
        requestMap.find(_._1 == collection.getOwner) map { ref =>
          ref._2.find(_._1 == collection.getUUID) map { colMap =>
            colMap._2 ++= items
            log.info(s"${colMap._2.size} - ${collection.getSize}")
            if (colMap._2.size == collection.getSize) {
              clientRef ! Right(MapResponse(collection.getOwner, collection.getName, colMap._2.toMap))
              colMap._2.clear
              ref._2.-(collection.getUUID)
            }
          } getOrElse log.warning("GetItemFromResponse: collectionMap not found")
        } getOrElse log.warning("GetItemFromResponse: refPair not found")

      /**
        * Remove item from collection  message, given a key of type String,
        * delete key-value pair from a specified collection
        *
        * @param collection a String representing the collection name
        * @param key a String representing the key to be deleted
        *
        */
      case RemoveFrom(requester, uuid, key) =>
        if (key.nonEmpty)
          sfMap.find(_._1.getUUID == uuid) map { c =>
            if (requester == c._1.getOwner || c._1.containsReadWriteContributor(requester))
              c._2 forward Remove(key)
            else sender ! "NoPrivileges"
          } getOrElse sender ! "UndefinedCollection"
        else {
          sfMap find (_._1.getUUID == uuid) map { coll =>
            if (requester == coll._1.getOwner || requester == "admin") {
              coll._2 ! PoisonPill
              sfMap = sfMap - coll._1
              sender ! "OK"
              authProxy ! RemoveCollectionFrom("admin", coll._1)
              authProxy ! RemoveCollectionFrom(requester, coll._1)
              if (coll._1.getOwner != requester)
                authProxy ! RemoveCollectionFrom(coll._1.getOwner, coll._1)
            } else sender ! "NoPrivileges"
          } getOrElse sender ! "UndefinedCollection"
        }

      /**
        * Add Contributor from collection, given username of Contributor and read
        * or readWrite permission
        *
        * @param username a String to identify the contributor to add
        * @param permission a boolean representing the permission : (true = readWrite , false = readOnly)
        * @param collection a String representing the collection name
        *
        */
      case AddContributor(requester, username, permission, uuid) =>
        val optColl = sfMap find (_._1.getUUID == uuid)
        optColl map { x =>
          x._1.addContributor(username, permission)
          if (x._1.getOwner == requester)
            authProxy ! AddCollectionTo(username, x._1)
          else sender ! "NoPrivileges"
        } getOrElse sender ! "UndefinedCollection"


      /**
        * Remove Contributor from collection, given username of Contributor , and permission
        *
        * @param username a String to identify the contributor to remove
        * @param collection a String representing the collection name
        *
        */
      case RemoveContributor(requester, username, uuid) =>
        val optColl = sfMap find (_._1.getUUID == uuid)
        optColl map  { x =>
          x._1.removeContributor(username)
          if (x._1.getOwner == requester)
            authProxy ! RemoveCollectionFrom(username, x._1)
          else sender ! "NoPrivileges"
        } getOrElse sender ! "UndefinedCollection"

    }
  }
}
