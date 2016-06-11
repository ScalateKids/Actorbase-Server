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

package com.actorbase.actorsystem.utils

import scala.math.Ordered.orderingToOrdered

case object ActorbaseCollection {

  sealed trait Permissions

  case object Read extends Permissions

  case object ReadWrite extends Permissions
}

/**
  * Class representing a collection of actorbase
  *
  * @param name a String representing the name of the collection
  * @param owner a String representing the username of the owner of this collection
  */
case class ActorbaseCollection (private var name: String,
  private var owner: String,
  private var size: Int = 0) extends Ordered[ActorbaseCollection] {

  private val uuid: String = owner + name

  private var contributors = Map[String, ActorbaseCollection.Permissions]("admin" -> ActorbaseCollection.ReadWrite)

  /**
    * @return a String representing the name of the collection
    */
  def getName: String = name

  /**
    * @return a String representing the username of the owner of this collection
    */
  def getOwner: String = owner

  /**
    * @return a String representing a universal-unique-identified ID
    */
  def getUUID: String = uuid

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  def getSize: Int = size

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  def containsReadContributor(contributor: String): Boolean = {
    var contains = false
    contributors get (contributor) map { c =>
      c match {
        case ActorbaseCollection.Read => contains = false
        case ActorbaseCollection.ReadWrite => contains = true
      }
    } getOrElse (contains = false)
    contains
  }

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  def containsReadWriteContributor(contributor: String): Boolean = {
    var contains = false
    contributors get (contributor) map { c =>
      c match {
        case ActorbaseCollection.Read => contains = true
        case ActorbaseCollection.ReadWrite => contains = false
      }
    } getOrElse (contains = false)
    contains
  }

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  def addContributor(username: String, permission: ActorbaseCollection.Permissions): Unit = contributors += (username -> permission)

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  def removeContributor(username: String): Unit = contributors -= username

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  def incrementSize = size += 1

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  def decrementSize = size -= 1

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  override def compare(that: ActorbaseCollection): Int = {
    (this.name + this.owner) compare (that.name + that.owner)
  }

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  override def equals(o: Any) = o match {
    case that: ActorbaseCollection => that.uuid.equals(this.uuid)
    case _ => false
  }

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  override def hashCode = uuid.hashCode

}
