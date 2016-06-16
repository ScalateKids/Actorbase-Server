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

package com.actorbase.actorsystem.messages.StorekeeperMessages

import akka.actor.ActorRef
import akka.dispatch.ControlMessage
import akka.routing.ConsistentHashingRouter.ConsistentHashable

sealed abstract trait StorekeeperMessage
/**
 * message for ask to the warehouseman to make data permanent on drive
 */
final case object Persist extends StorekeeperMessage with ControlMessage
/**
 * Message for do manager actor initialization
 * @param mn referement to actor manager to initialize
 */
final case class InitMn(mn: ActorRef) extends StorekeeperMessage with ControlMessage
/** 
 * Message for get all data in the Storekeeper
 * @param parent storefinder actor that made the request and where data will be sent
 */
 final case class GetAll(parent: ActorRef) extends StorekeeperMessage

// final case class Init(manager: ActorRef, range: KeyRange) extends StorekeeperMessage
/**
 * Message for get item with defined Key
 * @param key key of the requested data
 */
final case class GetItem(key: String) extends ConsistentHashable with StorekeeperMessage {
  override def consistentHashKey: Any = key
}
/**
 * Message for insert item in the storekeeper
 * @param parentRef referement of the actor that made the insertion request
 * @param key string with the key of the data to insert
 * @param value byte array with data that will be insert
 * @param update boolean flag to allow o deny data overwrite (true = allow ; false = deny)
 */
final case class InsertItem(parentRef: ActorRef, key: String, value: Array[Byte], update: Boolean = false) extends ConsistentHashable with StorekeeperMessage {
  override def consistentHashKey: Any = key
}
/**
 * Message for remove item from the storekeeper
 * @param parentRef referemtn to actor that made the request
 * @param key key of the data to remove
 */
final case class RemoveItem(parentRef: ActorRef, key: String) extends ConsistentHashable with StorekeeperMessage {
  override def consistentHashKey: Any = key
}
