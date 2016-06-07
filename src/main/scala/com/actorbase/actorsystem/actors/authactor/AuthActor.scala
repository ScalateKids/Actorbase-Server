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

package com.actorbase.actorsystem.actors.authactor

import akka.actor.{Actor, ActorLogging}

import com.actorbase.actorsystem.messages.AuthActorMessages._
import com.actorbase.actorsystem.messages.ClientActorMessages.ListResponse
import com.actorbase.actorsystem.utils.{ ActorbaseCollection, CryptoUtils }
import com.github.t3hnar.bcrypt._
import org.mindrot.jbcrypt.BCrypt

import java.io.File

class AuthActor extends Actor with ActorLogging {

  private val rootFolder = "actorbasedata/usersdata/"

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  override def receive = running(Set[Profile](Profile("admin", "actorbase".bcrypt(generateSalt), Set.empty[ActorbaseCollection])))

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  def persist(profiles: Set[Profile]): Unit = {
    var profileMap = Map.empty[String, String]
    profiles map (x => profileMap += (x.username -> x.password))
    val key = "Dummy implicit k"
    val encryptedProfilesFile = new File(rootFolder + "/usersdata.shadow")
    encryptedProfilesFile.getParentFile.mkdirs
    CryptoUtils.encrypt(key, profileMap, encryptedProfilesFile)
  }

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  def running(profiles: Set[Profile]): Receive = {

    case message: AuthActorMessages => message match {

      /**
        * Insert description here
        *
        * @param
        * @return
        * @throws
        */
      case AddCredentials(username, password) =>
        log.info(s"$username added")
        val salt = password.bcrypt(generateSalt)
        if (!profiles.contains(Profile(username, salt))) {
          persist(profiles + (Profile(username, salt, Set.empty[ActorbaseCollection])))
          context become running(profiles + (Profile(username, salt, Set.empty[ActorbaseCollection])))
        }

      /**
        * Change the password associated to an user given his username and
        * current password
        *
        * @param username a String representing the username of the user owner of
        * the password designatef for change
        * @param password a String representing the current password of the user that will be updated
        * @param newPassword a String representing the new password to be set for
        * the requested username
        */
      case UpdateCredentials(username, password, newPassword) =>
        val optElem = profiles find (elem => (elem.username == username) && BCrypt.checkpw(password, elem.password))
        val salt = newPassword.bcrypt(generateSalt)
        optElem map (elem => context become running (profiles - elem + elem.copy(password = salt))) getOrElse (sender ! "None")

      /**
        * Insert description here
        *
        * @param
        * @return
        * @throws
        */
      case RemoveCredentials(username) =>
        val optElem = profiles find (_.username == username)
        optElem map { x =>
          persist(profiles - x)
          context become running(profiles - x) } getOrElse log.error(s"AuthActor: $username elem not found")

      /**
        * Insert description here
        *
        * @param
        * @return
        * @throws
        */
      case Authenticate(username, password) =>
        val optElem = profiles find (elem => (elem.username == username) && BCrypt.checkpw(password, elem.password))
        optElem map (_ => if (username == "admin") sender ! "Admin" else sender ! "Common") getOrElse sender ! "None"

      /**
        * Insert description here
        *
        * @param
        * @return
        * @throws
        */
      case AddCollectionTo(username, collection) =>
        val optElem = profiles find (_.username == username)
        optElem map { x =>
          // val tempSet = x.collections + collection
          x.addCollection(collection)
          persist(profiles + x)
          context become running (profiles + x)
        } getOrElse log.error(s"AuthActor: Failed to add ${collection.getUUID} to $username")

      /**
        * Insert description here
        *
        * @param
        * @return
        * @throws
        */
      case RemoveCollectionFrom(username, collection) =>
        val optElem = profiles find (_.username == username)
        optElem map { x =>
          if (x.contains(collection)) {
            // val tempSet = x.collections - collection
            x.removeCollection(collection)
            persist(profiles + x)
            context become running (profiles + x)
          }
        } getOrElse log.error(s"AuthActor: Failed to add ${collection.getUUID} to $username")

      /**
        * Build a list of collection names and reply it to the sender
        *
        * @param owner a String representing the owner of the requested collection name list
        */
      case ListCollectionsOf(username) =>
        val optElem = profiles find (_.username == username)
        optElem map { set =>
          val names = set.getCollections map (collection => collection.getName)
          sender ! ListResponse(names.toList)
        }

      /**
        * Return all users contained in the system as a List[String]
        */
      case ListUsers =>
        var users = List.empty[String]
        profiles map (profile => users ::= profile.username)
        sender ! ListResponse(users)

    }
  }
}
