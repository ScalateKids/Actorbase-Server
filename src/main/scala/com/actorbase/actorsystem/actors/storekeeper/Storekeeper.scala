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

package com.actorbase.actorsystem.actors.storekeeper

import akka.actor.{ Actor, ActorLogging, ActorRef, Cancellable, Props }
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.pubsub.DistributedPubSubMediator.Subscribe
import akka.cluster.pubsub.DistributedPubSub

import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

import com.actorbase.actorsystem.messages.StorekeeperMessages._
import com.actorbase.actorsystem.messages.StorefinderMessages.{PartialMapTransaction, UpdateCollectionSize}
import com.actorbase.actorsystem.messages.WarehousemanMessages.{Init, Save}
import com.actorbase.actorsystem.messages.ClientActorMessages.Response
import com.actorbase.actorsystem.actors.warehouseman.Warehouseman
import com.actorbase.actorsystem.actors.manager.Manager.OneMore

import scala.concurrent.duration._

object Storekeeper {

  def props: Props = Props[Storekeeper].withDispatcher("control-aware-dispatcher")
  def props(n: String, o: String): Props = Props(classOf[Storekeeper], n, o).withDispatcher("control-aware-dispatcher")

}

/**
  *
  * @param data
  * @param manager
  * @param range
  * @param maxSize
  */
class Storekeeper(private val collectionName: String, private val collectionOwner: String) extends Actor with ActorLogging {

  val mediator = DistributedPubSub(context.system).mediator
  // subscribe to the topic named "persist-data"
  mediator ! Subscribe("persist-data", self)

  private val initDelay = 130 seconds       // delay for the first persistence message to be sent
  private val intervalDelay = 130 seconds   // interval in-between each persistence message has to be sent
  private var scheduler: Cancellable = _   // akka scheduler used to track time
  private val warehouseman = context.actorOf(Warehouseman.props( collectionOwner + collectionName ))
  private var manager: Option[ActorRef] = None
  private var checked = false
  val cluster = Cluster(context.system)

  warehouseman ! Init( collectionName, collectionOwner)

  /**
    * Actor lifecycle method, initialize a scheduler to persist data after some time
    * and continously based on a fixed interval
    *
    * @param
    * @return
    * @throws
    */
  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[MemberUp])
    scheduler = context.system.scheduler.schedule(
      initialDelay = initDelay,
      interval = intervalDelay,
      receiver = self,
      message = Persist
    )
  }

  /**
    * Actor lifecycle method, cancel the scheduler in order to not send persistence
    * messages to the void
    *
    * @param
    * @return
    * @throws
    */
  override def postStop(): Unit = {
    scheduler.cancel()
    cluster.unsubscribe(self)
  }

  def receive = running(Map[String, Array[Byte]]().empty)

  def running(data: Map[String, Array[Byte]]): Receive = {

    case message: StorekeeperMessage => message match {

      case InitMn(mn) =>
        // log.info("new MN received")
        manager = Some(mn)

      /**
        * GetItem message, this actor will send back a value associated with the input key
        *
        * @param key a String representing the key of the item to be returned (sta roba sarà da cambiare)
        *
        */
      case GetItem(key)  =>
        sender ! Response(data.get(key).getOrElse("None".getBytes))

      /**
        * GetAllItem message, this actor will send back the collection name and all the collection.
        */
      case GetAll(parent) =>
        if (data.nonEmpty)
          parent ! PartialMapTransaction(sender, data)

      /**
        * RemoveItem message, when the actor receive this message it will erase the item associated with the
        * key in input. This method doesn't throw an exception if the item is not present.
        */
      case RemoveItem(key) =>
        if (data contains(key)) {
          sender ! UpdateCollectionSize(false)
          context become running(data - key)
        }

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
      case ins: InsertItem =>
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
          if (!update && !data.contains(key)) {
            log.info("SK: got work!")
            sender ! UpdateCollectionSize(true)
            if (data.size > 256 && !checked) {
              checked = true
              manager map (_ ! OneMore) getOrElse (checked = false)
            }
          }
          else if (!update && data.contains(key)) {
            // log.warning(s"SK: Duplicate key found, cannot insert $key")
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
}
