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
  * @author Scalatekids
  * @version 1.0
  * @since 1.0
  */

package com.actorbase.actorsystem.actors.authactor

import akka.actor.{ Actor, ActorLogging, Cancellable, OneForOneStrategy }
import akka.actor.SupervisorStrategy._
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish

import com.actorbase.actorsystem.messages.StorekeeperMessages.Persist
import com.actorbase.actorsystem.messages.AuthActorMessages._
import com.actorbase.actorsystem.messages.ClientActorMessages.ListResponse
import com.actorbase.actorsystem.utils.ActorbaseCollection.Permissions
import com.actorbase.actorsystem.utils.{ ActorbaseCollection, CryptoUtils }
import com.github.t3hnar.bcrypt._
import com.typesafe.config.ConfigFactory
import org.mindrot.jbcrypt.BCrypt

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import java.io.File

/**
  * Class that represents an AuthActor. This actor is a singleton cluster actor used to
  * store the users profiles data. It's also used to check the credential on the login attempts.
  * This actor is responsible to persist the users profile datas on the filesystem
  */
class AuthActor extends Actor with ActorLogging {

  private val config = ConfigFactory.load().getConfig("persistence")
  // the rootfolder in which to store the users data
  private val rootFolder = config.getString("save-folder") + "usersdata/"

  private val initDelay = 20 seconds       // delay for the first persistence message to be sent
  private val intervalDelay = 50 seconds   // interval in-between each persistence message has to be sent
  private var scheduler: Cancellable = _   // akka scheduler used to track time
                                           // activate the extension
  val mediator = DistributedPubSub(context.system).mediator

  /**
    *  Override of the preStart Actor method
    */
  override def preStart = {
    scheduler = context.system.scheduler.schedule(
      initialDelay = initDelay,
      interval = intervalDelay,
      receiver = self,
      message = PersistDB
    )
    //   persist(Set[Profile](Profile("admin", "Actorb4se", Set.empty[ActorbaseCollection])))
  }

  /**
    * Override of the supervisionStrategy Actor method
    */
  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: Exception => Resume
    }

  /**
    * Override of the receive Actor method. Set the actor in the running state
    * with a default Set of Profiles, letting him receive a variety of messages
    * explained in the running method scaladoc
    */
  override def receive = running(Set[Profile](Profile("admin", "Actorb4se".bcrypt(generateSalt), Set.empty[ActorbaseCollection]),
    Profile("anonymous", "Anonym0us".bcrypt(generateSalt), Set.empty[ActorbaseCollection])))

  /**
    * Method used to persist the users data to filesystem
    *
    * @param profiles: a Set of Profile containing all the profiles of the users to persist on filesystem
    * @return no return value
    */
  def persist(profiles: Set[Profile]): Unit = {
    var profileMap = Map.empty[String, String]
    var contributorMap = Map.empty[String, Tuple2[ActorbaseCollection.Permissions, Set[ActorbaseCollection]]]
    profiles map  { x =>
      profileMap += (x.username -> x.password)
      var colls = Set.empty[ActorbaseCollection]
      x.getCollections filter (c => c.containsReadContributor(x.username)) map { rc =>
        colls += rc
        contributorMap += (x.username -> (ActorbaseCollection.Read -> colls))
      }
      colls = colls.empty
      x.getCollections filter (c => c.containsReadWriteContributor(x.username)) map { rc =>
        contributorMap += (x.username -> (ActorbaseCollection.ReadWrite -> colls))
      }
    }
    val key = config getString("encryption-key")
    val encryptedProfilesFile = new File(rootFolder + "/usersdata.shadow")
    encryptedProfilesFile.getParentFile.mkdirs
    val encryptedContributorsFile = new File(rootFolder + "/contributors.shadow")
    encryptedContributorsFile.getParentFile.mkdirs
    CryptoUtils.encrypt(key, profileMap, encryptedProfilesFile)
    CryptoUtils.encrypt(key, contributorMap, encryptedContributorsFile)
  }

  /**
    * Running state of the actor, while in this state the actor can receive this messages:<br>
    * _AddCredentials: when the actor receives this message it tries to register a user to the system adding
    *                  the Profile passed as message parameter to his data structure.<br>
    * _UpdateCredentials: when the actor receives this message it tries to update a Profile <br>
    * _RemoveCredentials: when the actor receives this message it tries to remove the Profile
    *                     passed as parameter of the message<br>
    * _Authenticate: when the actor receives this message it checks it the credentials passed as parameter of
    *                the message are valid credentials already registered on the system<br>
    * _AddCollectionTo: when the actor receives this message it tries to add the collection passed as message
    *                   parameter to the user passed<br>
    * _RemoveCollectionFrom: when the actor receives this message it tries to remove the collection passed as message
    *                        parameter from the user passed<br>
    * _ListCollectionOf: when the actor receives this message it returns all the collections name<br>
    * _ListUsers: when the actor receives this message it returns the list of all the users registrated on the system.
    *
    * @param profiles: Set of Profiles representing the users stored in this actor
    */
  def running(profiles: Set[Profile]): Receive = {

    case message: AuthActorMessages => message match {

      case Clean =>
        context become running(profiles.empty)

      case Save =>
        persist(profiles)

      case Init(username, password) =>
        context become running (profiles + Profile(username, password, Set.empty[ActorbaseCollection]))

      /**
        * Insert description here
        *
        * @param
        * @return
        * @throws
        */
      case AddCredentials(username, password) =>
        val passwordCheck = """^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$""".r
        val check = passwordCheck findFirstIn password
        check map { p =>
          val salt = password.bcrypt(generateSalt)
          if (!profiles.contains(Profile(username, salt))) {
            sender ! "OK"
            persist(profiles + Profile(username, salt, Set.empty[ActorbaseCollection]))
            context become running (profiles + Profile(username, salt, Set.empty[ActorbaseCollection]))
          } else sender ! "OK"
        } getOrElse sender ! "WrongCredentials"

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
        val passwordCheck = """^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$""".r
        val check = passwordCheck findFirstIn newPassword
        check map { p =>
          val optElem = profiles find (elem => (elem.username == username) && BCrypt.checkpw(password, elem.password))
          val salt = newPassword.bcrypt(generateSalt)
          optElem map { elem =>
            sender ! "OK"
            sender ! Stop
            persist(profiles - elem + elem.copy(password = salt))
            context become running (profiles - elem + elem.copy(password = salt))
          } getOrElse sender ! "UndefinedUsername"
        } getOrElse sender ! "WrongNewPassword"

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
          sender ! "OK"
          context become running(profiles - x)
        } getOrElse sender ! "UndefinedUser"

      /**
        * Insert description here
        *
        * @param
        * @return
        * @throws
        */
      case Authenticate(username, password) =>
        val optElem = profiles find (elem => (elem.username == username) && BCrypt.checkpw(password, elem.password))
        optElem map (_ => sender ! Some(username)) getOrElse sender ! Some("None")

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
            x.removeCollection(collection)
            persist(profiles + x)
            context become running (profiles + x)
          }
        } getOrElse log.error(s"AuthActor: Failed to remove ${collection.getUUID} from $username")

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
        * Build a list of UUIDs of collections owned by the username
        * that request them
        */
      case ListUUIDsOwnedBy(username) =>
        val optElem = profiles find (_.username == username)
        optElem map { set =>
          val uuids =
            if (username == "admin") set.getCollections map (collection => collection.getUUID)
            else set.getCollections map (collection => if (collection.getOwner == username) collection.getUUID else "")
          sender ! ListResponse(uuids.toList)
        }

      /**
        * Return all users contained in the system as a List[String]
        */
      case ListUsers =>
        var users = List.empty[String]
        profiles map (profile => users ::= profile.username)
        sender ! ListResponse(users)

      case PersistDB =>
        mediator ! Publish("persist-data", Persist)
    }
  }
}
