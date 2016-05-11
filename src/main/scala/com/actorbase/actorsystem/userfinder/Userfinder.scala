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

package com.actorbase.actorsystem.userfinder

import akka.actor.{Actor, ActorRef, ActorLogging, Props}
import com.actorbase.actorsystem.userfinder.messages._
import com.actorbase.actorsystem.userkeeper.Userkeeper
import com.actorbase.actorsystem.userkeeper.Userkeeper._

import scala.collection.immutable.TreeMap

object Userfinder {
  def props() : Props = Props(new Userfinder())
}

class Userfinder extends Actor with ActorLogging {

  def receive = running(TreeMap[String, ActorRef]().empty)

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  def running(ukMap: TreeMap[String, ActorRef]): Receive = {

    /**
      * Insert message, create an actor of type Userkeeper initializing it with
      * a username/password pair String and save the reference of type ActorRef
      * into ukMap[String, ActorRef]
      *
      * @param username a String representing the username of the new user
      * @param password a String type representing the hash of the password for
      * the newly inserted user
      */
    case InsertTo(username, password) =>
      log.info("UF: insert user into userkeeper")
      if(!ukMap.contains(username))
        context become running(ukMap + (username -> context.actorOf(Userkeeper.props(username, password))))
      else
        log.info(s"Duplicate insert for username $username: already exists")

    /**
      * Retrieve a password associated to an user given his username
      *
      * @param username a String representing the username of the user whose
      * password is requested
      */
    case GetPasswordOf(username) =>
      log.info(s"Password request for $username")
      if(ukMap.contains(username))
        ukMap.get(username).get forward GetPassword
      else log.info(s"$username not found, can't get password")

    /**
      * Retrieve a list of collections associated to an user given his username
      *
      * @param username a String representing the username of the user whose
      * collections is requested
      * @param read a Boolean representing the permission level on the requested
      * collections, true for read-only collections, false for read-write ones
      */
    case GetCollectionsOf(username, read) =>
      log.info(s"Get collections of user $username with read permission $read")
      if(ukMap.contains(username))
        ukMap.get(username).get forward GetCollections(read)
      else log.info(s"$username not found can't get collections")

    /**
      * Change the password  associated to an user given his username
      *
      * @param username a String representing the username of the user owner of
      * the password designatef for change
      * @param newPassword a String representing the new password to be set for
      * the requested username
      */
    case ChangePasswordOf(username, newPassword) =>
      log.info(s"Changing password of user $username")
      if(ukMap.contains(username))
        ukMap.get(username).get forward ChangePassword(newPassword)
      else log.info(s"$username not found can't change password")

    /**
      * Remove a collection to an associated user given his username
      *
      * @param username a String representing the username of the user owner of
      * the collection designated for removal
      * @param read a Boolean representing the permission level on the requested
      * collection, true for read-only collections, false for read-write ones
      * @param collection a String representing the collection name to be removed
      */
    case RemoveCollectionFrom(username, read, collection) =>
      log.info(s"Removing collection with read persmission $read from user $username")
      if(ukMap.contains(username))
        ukMap.get(username).get forward RemoveCollection(read, collection)
      else log.info(s"$username not found can't remove collection")

    /**
      * Add a collection to an associated user given his username
      *
      * @param username a String representing the username of the user owner of
      * the collection designated for addition
      * @param read a Boolean representing the permission level on the requested
      * collection, true for read-only collections, false for read-write ones
      * @param collection a String representing the collection name to be added
      */
    case AddCollectionTo(username, read, collection) =>
      log.info(s"Adding collection with read persmission $read to user $username")
      if(ukMap.contains(username))
        ukMap.get(username).get forward AddCollection(read, collection)
      else log.info(s"$username not found can't add collection")

    /**
      * Insert description here
      *
      * @param
      * @return
      * @throws
      */
    case UpdateCollectionSizeTo(collection, increment) =>
      log.info("UF: increment size")
      ukMap.find(_._1 == collection.getOwner).head._2 ! UpdateCollectionSize(collection, increment)
  }

}
