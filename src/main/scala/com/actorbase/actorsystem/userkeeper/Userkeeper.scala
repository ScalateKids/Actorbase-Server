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

package com.actorbase.actorsystem.userkeeper

import akka.actor.{Actor, ActorRef, ActorLogging, Props}
import scala.collection.mutable.ListBuffer

object Userkeeper {

  def props() : Props = Props(new Userkeeper("user", "pass"))

  case class GetCollections(read: Boolean)

  case class GetPassword(client: ActorRef)

  case class ChangePassword(newPassword: String)

  case class RemoveCollection(read: Boolean, collection: String)

  case class AddCollection(read: Boolean, collection: String)

  case class BindClient(client: ActorRef)

}

/**
  * Insert description here
  *
  * @param
  * @return
  * @throws
  */
class Userkeeper(var username: String = "user", var password: String = "pass") extends Actor with ActorLogging {

  import Userkeeper._

  private var collections: ListBuffer[String] = new ListBuffer[String]

  private var readCollections: ListBuffer[String] = new ListBuffer[String]

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  def receive = {

    case GetCollections(read) =>
      if(read)
        sender ! readCollections
      else sender ! collections

    case GetPassword(client) => client ! Some(password)

    case ChangePassword(newPassword) => password = newPassword

    case AddCollection(read, collection) =>
      if(read)
        readCollections :+= collection
      else
        collections :+= collection

    case RemoveCollection(read, collection) =>
      if(read)
        readCollections -= collection
      else
        collections -= collection

  }

}
