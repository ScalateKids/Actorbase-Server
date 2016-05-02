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

import java.util.UUID

object Userkeeper {

  def props(username: String, password: String) : Props = Props(new Userkeeper(username, password))

  case object GetPassword

  case class GetCollections(read: Boolean)

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
class Userkeeper private (var username: String = "user",
  var password: String = "pass",
  val uuid: String = UUID.randomUUID.toString) extends Actor with ActorLogging {

  import Userkeeper._

  /** read-write collections list */
  private var collections: ListBuffer[String] = new ListBuffer[String]

  /** read-only collections list */
  private var readCollections: ListBuffer[String] = new ListBuffer[String]

  /** client ActorRef associated */
  private var client: ActorRef = _

  /**
    * Receive method of Userkeeper actor, it currently handle 6 message type:
    *
    * - GetPassword send associated password to the ClientActor reference who ask for it
    * - ChangePassword replace the password to a new password
    * - AddCollection add a new collection to ListBuffer[String] collections or to
    *   readCollections based on a Boolean flag
    * - RemoveCollection remove a new collection to ListBuffer[String] collections or to
    *   readCollections based on a Boolean flag
    * - BindClient associate an ActorRef representing a ClientActor to the instance of this
    *   actor
    *
    * @param
    * @return
    * @throws
    */
  def receive = {

    case GetCollections(read) =>
      if(read) {
        val rCollections = readCollections
        sender ! rCollections
      }
      else {
        val rwCollections = readCollections
        sender ! rwCollections
      }

    case GetPassword => sender ! Some(password)

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

    case BindClient(assoc) => client = assoc

  }

}
