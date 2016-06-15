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

// TEMPORARY BRIDGE BETWEEN MAIN AND STOREKEEPER

package com.actorbase.actorsystem.actors.storefinder

import akka.actor.{ Actor, ActorLogging, OneForOneStrategy, Props }

import akka.cluster.Cluster
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.routing.ClusterRouterPool
import akka.cluster.routing.ClusterRouterPoolSettings
import akka.routing.{ ActorRefRoutee, ConsistentHashingPool, FromConfig, Router }
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import akka.routing.Broadcast
import akka.actor.SupervisorStrategy._

import com.actorbase.actorsystem.messages.StorefinderMessages._
import com.actorbase.actorsystem.messages.StorekeeperMessages.{GetItem, GetAll, InsertItem, RemoveItem, InitMn}
import com.actorbase.actorsystem.messages.MainMessages.CompleteTransaction
import com.actorbase.actorsystem.actors.storekeeper.Storekeeper
import com.actorbase.actorsystem.actors.manager.Manager
import com.actorbase.actorsystem.utils.ActorbaseCollection
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

object Storefinder {
  def props(collection: ActorbaseCollection): Props = Props(new Storefinder(collection))
}

/**
  *
  * @param collection ActorbaseCollection that represent the name of the collection
  * @param skMap TreeMap[KeyRange, ActorRef]. This collection represent a map from keyranges to an ActorRef of a
  *              Storekeeper
  * @param range represent the range of the keys mappable in this storefinder
  * @param maxSize represent the max size of the collection
  */
class Storefinder(private var collection: ActorbaseCollection) extends Actor with ActorLogging {

  val cluster = Cluster(context.system)
  val config = ConfigFactory.load().getConfig("storekeepers")
  val role =
    if (config.getString("role") == "") None
    else Some(config getString "role")

  val storekeepers = context.actorOf(ClusterRouterPool(ConsistentHashingPool(0),
    ClusterRouterPoolSettings(config getInt "max-instances", config getInt "instances-per-node", true, useRole = role)).props(Storekeeper.props(collection.getName, collection.getOwner, config getInt "size")), name = "storekeepers")

  val manager = context.actorOf(Manager.props(collection.getName, collection.getOwner, storekeepers), collection.getUUID + "-manager")

  storekeepers ! Broadcast(InitMn(manager))

  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])
  override def postStop(): Unit = cluster.unsubscribe(self)

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: Exception      => Resume
        // case _: NullPointerException     => Restart
        // case _: IllegalArgumentException => Stop
        // case _: Exception                => Escalate
    }

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  def receive: Receive = {

    case message: StorefinderMessage => message match {

      /**
        *
        */
      // case com.actorbase.actorsystem.storefinder.messages.Init(name, manager, range) =>
      //   log.info("SF: init")


      /**
        * Insert message, insert a key/value into a designed collection
        *
        * @param key a String representing the new key to be inserted
        * @param value a Any object type representing the value to be inserted
        * with associated key, default to Array[Byte] type
        * @param update a Boolean flag, define the insert behavior (with or without
        * updating the value)
        *
        */
      case ins: Insert =>
        // log.info("SF: inserting " + ins.key)
        storekeepers forward (ConsistentHashableEnvelope(message = InsertItem(self, ins.key, ins.value, ins.update), hashKey = ins.key))

      /**
        * Message that forward to Storekeeper in order to retrieve a given key
        *
        * @param key a String representing the key of the item to be retrieved
        */
      case Get(key) =>
        // log.info(s"SF: getItem of key -> ${key}")
        storekeepers forward (ConsistentHashableEnvelope(message = GetItem(key), hashKey = key))

      /**
        * Message that returns the entire collection mapped by this Storefinder
        */
      case GetAllItems =>
        // log.info("SF: getallitem")
        storekeepers forward Broadcast(GetAll(self))

      /**
        * Message that removes an item with the given key
        *
        * @param key a String representing the key of the item to be removed
        */
      case rem: Remove => storekeepers forward RemoveItem(self, rem.key)

      /**
        * Update the size of the collection that this storefinder represents,
        * increasing it if a insert is performed, decreasing it in case of
        * a remove
        *
        * @param increment a Boolean value representing whether the collection
        * represented by this storefinder is increased in size by an insert operation
        * or decreased by a remove operation
        */
      case UpdateCollectionSize(increment) =>
        // log.info(s"SF: Update size ${collection.getOwner}")
        if (increment)
          collection.incrementSize
        else collection.decrementSize

      /**
        * Await for storekeeper entire partial map returning, and
        * forward it to main actor
        *
        * @param clientRef an ActorRef pointing to the client who sent the request
        * @param items a Map[String, Any] containing all key-value pair of the partial
        * collection contained inside a single storekeeper, receive order is unpredictable
        */
      case PartialMapTransaction(clientRef, items) =>
        context.parent ! CompleteTransaction(clientRef, collection, items)

    }
  }

}
