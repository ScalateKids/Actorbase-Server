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

package com.actorbase.actorsystem.storefinder

import akka.actor.{Actor, ActorLogging, Props}

import akka.cluster.routing.ClusterRouterPool
import akka.cluster.routing.ClusterRouterPoolSettings
import akka.routing.{ ConsistentHashingPool, FromConfig }
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import akka.routing.Broadcast

import com.actorbase.actorsystem.storefinder.messages._
import com.actorbase.actorsystem.storekeeper.messages._
import com.actorbase.actorsystem.storekeeper.Storekeeper
import com.actorbase.actorsystem.warehouseman.Warehouseman
import com.actorbase.actorsystem.warehouseman.messages.RemoveSfFolder

import com.actorbase.actorsystem.utils.ActorbaseCollection

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

  // val storekeepers = context.actorOf(ConsistentHashingPool(20).props(Props(new Storekeeper(context.actorOf(Warehouseman.props(collection.getName))))), name = "storekeepers")
  val storekeepers = context.actorOf(ClusterRouterPool(ConsistentHashingPool(0),
    ClusterRouterPoolSettings(10000, 20, true, None)).props(Storekeeper.props), name = "storekeepers")
  // val storekeepers = context.actorOf(FromConfig.props(Props(new Storekeeper(context.actorOf(Warehouseman.props(collection.getName))))), name = "storekeepers")

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  def receive: Receive = {

    /**
      *
      */
    case com.actorbase.actorsystem.storefinder.messages.Init(name, manager, range) =>
      log.info("SF: init")


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
    case ins: com.actorbase.actorsystem.storefinder.messages.Insert =>
      log.info("SF: inserting " + ins.key)
      storekeepers ! (ConsistentHashableEnvelope(message = com.actorbase.actorsystem.storekeeper.messages.Insert(ins.key, ins.value, ins.update), hashKey = ins.key))

    /**
      * Message that search for a given key
      */
    case get: com.actorbase.actorsystem.storefinder.messages.GetItem =>
      log.info(s"SF: getItem of key -> ${get.key}")
      storekeepers forward (ConsistentHashableEnvelope(message = com.actorbase.actorsystem.storekeeper.messages.GetItem(get.key), hashKey = get.key))

    /**
      * Message that returns the entire collection mapped by this Storefinder
      */
    case com.actorbase.actorsystem.storefinder.messages.GetAllItem =>
      log.info("SF: getallitem")
      storekeepers forward Broadcast(com.actorbase.actorsystem.storekeeper.messages.GetAllItem(self))

    /**
      * Message that removes an item with the given key
      *
      * @param key a String representing the key of the item to be removed
      */
    case rem: com.actorbase.actorsystem.storefinder.messages.RemoveItem =>
      log.info("SF: remove")
      storekeepers ! com.actorbase.actorsystem.storekeeper.messages.RemoveItem(rem.key)

    /**
      *
      */
    case UpdateCollectionSize(increment) =>
      // log.info(s"SF: Update size ${collection.getOwner}")
      if (increment)
        collection.incrementSize
      else collection.decrementSize
      // context.parent ! com.actorbase.actorsystem.main.messages.UpdateCollectionSize(collection, increment)

    /**
      *
      */
    case GetAllItemResponse(clientRef, items) =>
      context.parent ! com.actorbase.actorsystem.main.Main.GetItemFromResponse(clientRef, collection, items)

  }

}
