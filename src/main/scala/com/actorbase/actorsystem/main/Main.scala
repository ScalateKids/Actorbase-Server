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

package com.actorbase.actorsystem.main

import akka.actor.{ Actor, ActorLogging, ActorRef, PoisonPill, Props }
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.{ExtractEntityId, ExtractShardId}

import scala.collection.mutable

import com.actorbase.actorsystem.storefinder.Storefinder
import com.actorbase.actorsystem.utils.ActorbaseCollection
import com.actorbase.actorsystem.messages.MainMessages._
import com.actorbase.actorsystem.messages.StorefinderMessages._

/**
  * Insert description here
  *
  * @param
  * @return
  * @throws
  */
object Main {

  /**
    * Props method, used to build an instance of Main actor
    *
    * @return an object of type Props, usable directly with an actorsystem running
    */
  def props = Props[Main]//.withDispatcher("control-aware-dispatcher")

  /** name of the sharded entity */
  def shardName = "mainActor"

  /**
    * ExtractShardId is a function needed by akka's cluster sharding extension in order to
    * retrieve shard region ids while addressing messages between sharded actors
    *
    * @return a String representing an UUID of a shard-region where the actor belongs to
    */
  val extractShardId: ExtractShardId = {
    case CreateCollection(collection) => (collection.getUUID.hashCode % 100).toString
    case RemoveFrom(uuid, _) => (uuid.hashCode % 100).toString
    case InsertTo(collection, _, _, _) => (collection.getUUID.hashCode % 100).toString
    case GetFrom(collection, _) => (collection.getUUID.hashCode % 100).toString
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
  }

}

/**
  * Insert description here
  *
  * @param
  * @return
  * @throws
  */
class Main extends Actor with ActorLogging {

  private var sfMap = Map[ActorbaseCollection, ActorRef]().empty
  private var requestMap = Map[String, mutable.Map[ActorbaseCollection, mutable.Map[String, Any]]]() // a bit clunky, should switch to a queue

  /**
    * Method that create a collection in Actorbase.
    *
    * @param name the name of the collection
    * @param owner the owner of the collection
    * @return an ActorRef pointing to the Storefinder just created that maps the collection
    */
  private def createCollection(collection: ActorbaseCollection): Option[ActorRef] = {
    if (sfMap.contains(collection)) sfMap.get(collection) // to be tested, probably uses equals, fuck up with different sizes
    else {
      log.info(s"creating ${collection.getName} for ${collection.getOwner}")
      val sf = context.actorOf(Storefinder.props(collection))
      sfMap += (collection -> sf)
      Some(sf)
    }
  }

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
      case InsertTo(collection, key, value, update) =>
        sfMap.find(x => x._1.compare(collection) == 0) map (_._2 ! Insert(key, value, update)) getOrElse (
          createCollection(collection) map (_ ! Insert(key, value, update)) getOrElse log.error("Error retrieving storefinder ActorRef"))

      /**
        * Create a collection in the system
        *
        * @param name a String representing the name of the collection
        * @param owner a String representing the owner of the collection
        */
      case CreateCollection(collection) => createCollection(collection)

      /**
        * Get item from collection message, given a key of type String, retrieve
        * a value from a specified collection, if key is empty, remove the
        * entire collection
        *
        * @param collection a String representing the collection name
        * @param key a String representing the key to be retrieved
        */
      case GetFrom(collection, key) =>
        if (key.nonEmpty)
          sfMap.find(_._1.compare(collection) == 0) map (_._2 forward Get(key)) getOrElse log.warning(s"Key $key not found")
        else {
          // WIP: still completing
          sfMap.find(x => x._1.compare(collection) == 0) map { coll =>
            requestMap.find(_._1 == coll._1.getOwner) map (_._2 += (coll._1 -> mutable.Map[String, Any]())) getOrElse (
              requestMap += (collection.getOwner -> mutable.Map[ActorbaseCollection, mutable.Map[String, Any]](coll._1 -> mutable.Map[String, Any]())))
            if (coll._1.getSize > 0)
              sfMap get collection map (_ forward GetAllItems) getOrElse log.warning (s"MAIN: key $key not found")
            else
              sender ! com.actorbase.actorsystem.clientactor.messages.MapResponse(collection.getName, Map[String, Any]())
          }
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
          ref._2.find(_._1.compare(collection) == 0) map { colMap =>
            colMap._2 ++= items
            log.info(s"${colMap._2.size} - ${collection.getSize} - ${colMap._1.getSize}")
            if (colMap._2.size == collection.getSize) {
              clientRef ! com.actorbase.actorsystem.clientactor.messages.MapResponse(collection.getName, colMap._2.toMap)
              colMap._2.clear
              ref._2.-(collection)
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
      case RemoveFrom(uuid, key) =>
        if (key.nonEmpty)
          sfMap.filterKeys(x => (x.getUUID == uuid)).head._2 ! Remove(key)
        else {
          sfMap find (_._1.getUUID == uuid) map { coll =>
            coll._2 ! PoisonPill
            sfMap -= coll._1
          } getOrElse log.warning(s"Collection with $uuid not found")
        }

      /**
        * Add Contributor from collection, given username of Contributor and read
        * ore readWrite permission
        *
        * @param username a String to identify the contributor to add
        * @param permission a boolean representing the permission : (true = readWrite , false = readOnly)
        * @param collection a String representing the collection name
        *
        */
      case AddContributor(username , permission, collection) =>
        // need controls
        // ufRef ! AddCollectionTo(username, permission, ActorbaseCollection(collection, username))

      /**
        * Remove Contributor from collection, given username of Contributor , and permission
        *
        * @param username a String to identify the contributor to remove
        * @param permission a boolean representing the permission : (true = readWrite , false = readOnly)
        * @param collection a String representing the collection name
        *
        */
      case RemoveContributor(username, permission, collection) =>
        // need controls
        // ufRef ! RemoveCollectionFrom(username, permission, ActorbaseCollection(collection, username))

    }
  }
}
