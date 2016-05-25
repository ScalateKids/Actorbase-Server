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

package com.actorbase.actorsystem.storekeeper

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}

import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

import com.actorbase.actorsystem.storekeeper.messages._
import com.actorbase.actorsystem.clientactor.messages.Response
import com.actorbase.actorsystem.warehouseman.Warehouseman
import com.actorbase.actorsystem.warehouseman.messages._
import com.actorbase.actorsystem.storefinder.messages.{GetAllItemResponse, UpdateCollectionSize}

import scala.collection.immutable.TreeMap
import scala.concurrent.duration._

object Storekeeper {

  def props: Props = Props[Storekeeper]
  def props(c: String): Props = Props(new Storekeeper(c))

}

/**
  *
  * @param data
  * @param manager
  * @param range
  * @param maxSize
  */
class Storekeeper(private val collectionUUID: String) extends Actor with ActorLogging {

  private val initDelay = 60 seconds     // delay for the first persistence message to be sent
  private val intervalDelay = 60 seconds //15 minutes   // interval in-between each persistence message has to be sent
  private var scheduler: Cancellable = _   // akka scheduler used to track time
  private val warehouseman = context.actorOf(Warehouseman.props(collectionUUID))

  /**
    * Actor lifecycle method, initialize a scheduler to persist data after some time
    * and continously based on a fixed interval
    *
    * @param
    * @return
    * @throws
    */
  override def preStart(): Unit = {
    // log.info("SK prestarted")
     scheduler = context.system.scheduler.schedule(
       initialDelay = initDelay,
       interval = intervalDelay,
       receiver = self,
       message = Persist
     )
  }

  def receive = running(TreeMap[String, Any]().empty)

  /**
    * Actor lifecycle method, cancel the scheduler in order to not send persistence
    * messages to the void
    *
    * @param
    * @return
    * @throws
    */
  // override def postStop(): Unit = scheduler.cancel()

  def running(data: TreeMap[String, Any]): Receive = {

    /**
      * GetItem message, this actor will send back a value associated with the input key
      *
      * @param key a String representing the key of the item to be returned (sta roba sarà da cambiare)
      *
      */
    case getItem: GetItem  =>
      sender ! Response(data.get(getItem.key).getOrElse("None").toString())

    /**
      * GetAllItem message, this actor will send back the collection name and all the collection.
      */
    case GetAllItem(parent) =>
      log.info("SK GetAllItems")
      parent ! GetAllItemResponse(sender, data)

    /**
      * RemoveItem message, when the actor receive this message it will erase the item associated with the
      * key in input. This method doesn't throw an exception if the item is not present.
      */
    case rem: RemoveItem =>
      sender ! UpdateCollectionSize(false)
      context become running(data - rem.key)

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
      log.info("SK: Inserting " + ins.key)

      /**
        * private method that insert an item to the collection, can allow the update of the item or not
        * changing the param update
        *
        * @param update boolean. 1 if the insert allow an update, 0 otherwise
        * @param key String representing the key of the item
        * @param value Any representing the value of the item
        */
      def insertOrUpdate(update: Boolean, key: String): Boolean = {
        var done = true
        if (update)
          sender ! UpdateCollectionSize(true)
        else if (!update && !data.contains(key))
          sender ! UpdateCollectionSize(true)
        else if (!update && data.contains(key)) {
          log.warning("SK: Duplicate key found, cannot insert")
          done = false
        }
        done
      }

      if (insertOrUpdate(ins.update, ins.key) == true)
        context become running(data + (ins.key -> ins.value))

      /**
        * Persist data to disk
        */
      case Persist => warehouseman ! Save( data )

  }

}
