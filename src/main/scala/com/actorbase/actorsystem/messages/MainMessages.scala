/**
  * The MIT License (MIT)
  * <p/>
  * Copyright (c) 2016 ScalateKids
  * <p/>
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the \"Software\"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  * <p/>
  * The above copyright notice and this permission notice shall be included in all
  * copies or substantial portions of the Software.
  * <p/>
  * THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
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

package com.actorbase.actorsystem.messages.MainMessages

import akka.actor.ActorRef
import akka.dispatch.ControlMessage
import scala.collection.immutable.TreeMap

import com.actorbase.actorsystem.utils.ActorbaseCollection

sealed abstract trait MainMessage

final case class InsertTo(collection: ActorbaseCollection, key: String, value: Array[Byte], update: Boolean = false) extends MainMessage

final case class GetFrom(collection: ActorbaseCollection, key: String = "") extends MainMessage

final case class CompleteTransaction(clientRef: ActorRef, collection: ActorbaseCollection, items: TreeMap[String, Any]) extends MainMessage with ControlMessage

final case class RemoveFrom(uuid: String, key: String = "") extends MainMessage

final case class AddContributor(username: String, permission: Boolean = false , collection: String) extends MainMessage

final case class RemoveContributor(username: String, permission: Boolean = false , collection: String) extends MainMessage

final case class CreateCollection(collection: ActorbaseCollection) extends MainMessage
