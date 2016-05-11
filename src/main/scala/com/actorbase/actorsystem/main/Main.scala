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

package com.actorbase.actorsystem.main

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import spray.json.DefaultJsonProtocol._
import scala.collection.mutable.Map

import scala.collection.immutable.TreeMap
import com.actorbase.actorsystem.storefinder.Storefinder
import com.actorbase.actorsystem.storefinder.messages._
import com.actorbase.actorsystem.userfinder.Userfinder
import com.actorbase.actorsystem.userfinder.messages._
import com.actorbase.actorsystem.userkeeper.Userkeeper
import com.actorbase.actorsystem.userkeeper.Userkeeper.GetPassword
import com.actorbase.actorsystem.main.messages._
import com.actorbase.actorsystem.utils.{KeyRange, ActorbaseCollection, CollectionRange}

import com.github.t3hnar.bcrypt._
import org.mindrot.jbcrypt.BCrypt
import akka.actor.Stash

import java.io._

/**
  * Insert description here
  *
  * @param
  * @return
  * @throws
  */
object Main {

  case class Response(response: String)

  case object Response {
    implicit val goJson = jsonFormat1(Response.apply)
  }

  case class AddUser(username: String, password: String)

  case class Testsk()

  case class Login(username: String)

  case class Testsf(key: String)

  case object BinTest

  case class Insert(owner: String, name: String, key: String, value: Any, update: Boolean = false)

  case class GetItemFrom(collection: ActorbaseCollection, key: String = "")

  case class GetItemFromResponse(clientRef: ActorRef, collection: ActorbaseCollection, items: TreeMap[String, Any])

  // manca getcollection?

  case class RemoveItemFrom(collection: String, key: String)

  case class AddContributor(username: String, permission: Boolean = false , collection: String)

  case class RemoveContributor(username: String, permission: Boolean = false , collection: String)

  case class CreateCollection(name: String, owner: String) // basta così al momento

  case class RemoveCollection( name: String, owner: String)

  // case class UpdateCollectionSize(collection: ActorbaseCollection, increment: Boolean = true)

  case object InitUsers

}

/**
  * Insert description here
  *
  * @param
  * @return
  * @throws
  */
class Main extends Actor with ActorLogging with Stash {
  import Main._

  private val ufRef: ActorRef = context.actorOf(Userfinder.props, "Userfinder") //TODO tutti devono avere lo stesso riferimento
  private var sfMap = new TreeMap[CollectionRange, ActorRef]()
  private var counter = 0 // this is for debug purposes
  private var getMap = new TreeMap[ActorRef, TreeMap[String, Any]]()
  private var requestMap = new TreeMap[ActorRef, Map[ActorbaseCollection, Map[String, Any]]]() // a bit clunky

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  def receive = waitingForRequests()  // start in this state

  /**
    * Method that create a collection in Actorbase.
    *
    * @param name the name of the collection
    * @param owner the owner of the collection
    * @return an ActorRef pointing to the Storefinder just created that maps the collection
    */
  private def createCollection(name: String, owner: String): ActorRef = {
    log.info(s"creating for $owner")
    ufRef ! InsertTo(owner, "pass") // DEBUG: to be removed
    var collection = new ActorbaseCollection(name, owner)
    val sf = context.actorOf(Storefinder.props(collection).withDispatcher("control-aware-dispatcher") )
    var newCollectionRange = new CollectionRange(collection, new KeyRange("a", "z")) //TODO CAMBIARE Z CON MAX
    ufRef ! AddCollectionTo(owner, false, collection)
    sfMap += (newCollectionRange -> sf)
    sf
  }

  /**
    * This method defines the type of messages that this actor can receive while in waitingForRequests status
    */
  private def waitingForRequests(): Actor.Receive = {

    /**
      * Login message, this is received when a user tries to authenticate into the system.
      *
      * @param username the username inserted to authenticate
      */
    case Login(username) => ufRef forward GetPasswordOf(username)

    /**
      * This message will probably populate username/password after disk read
      */
    case InitUsers =>
      ufRef ! InsertTo("user", "pass")
      ufRef ! InsertTo("user2", "pass2")

    /**
      * Add a new user sending the username and hashing a password with Blowfish
      * salt
      *
      * @param username a String representing the username of the newly added User
      * @param password a String representing the associated password to the newly
      * added User
      */
    case AddUser(username, password) => ufRef ! InsertTo(username, password.bcrypt(generateSalt))

    /**
      * Insert message, insert a key/value into a designed collection, searching
      * if sfMap contains the collection I need, if it's present search for the
      * right keyrange
      *
      * @param owner a String representing the owner of the collection
      * @param name a String representing the collection name
      * @param key a String representing the new key to be inserted
      * @param value a Any object type representing the value to be inserted
      * with associated key, default to Array[Byte] type
      * @param update a Boolean flag, define the insert behavior (with or without
      * updating the value)
      */
    case Insert(owner, name, key, value, update) =>
      val rangeRef = sfMap.find(x => (x._1.isSameCollection(name, owner) && x._1.getKeyRange.contains(key)))
      rangeRef match {
        case Some(t) =>
          t._2 forward com.actorbase.actorsystem.storefinder.messages.Insert(key, value, update)
          context.become(processingRequest)
        case None =>
          log.info("MAIN: collection range not found, creating collection..")
          createCollection(name, owner) forward com.actorbase.actorsystem.storefinder.messages.Insert(key, value, update) // STUB needed for stress-test
          context.become(processingRequest)
      }
      // var inserted: Boolean = false
      // for ((collectionRange, sfRef) <- sfMap) {
      //   if (collectionRange.isSameCollection(name, owner) && collectionRange.getKeyRange.contains(key) ) {
      //     // right collection and right keyrange (right collectionRange), let's insert here
      //     log.info("inserting "+key+" in the range "+collectionRange.toString)

      //     counter += 1
      //     if (counter == 1000) {
      //       println("inserting " + key + " in the range " + collectionRange.toString)
      //       counter = 0
      //     }

      //     inserted = true
      //     sfRef forward com.actorbase.actorsystem.storefinder.messages.Insert(key, value, update)

      //     // TODO uscire dal for

      //     // change context because of an Insert is happening
      //     context.become(processingRequest())
      //   }
      // }
      // if (!inserted) {
      //   // TODO possibile problema futuro, al primo insert nessuno ha la collection e come si capisce chi deve crearla?
      //   log.info("item has not been inserted, must forward to siblings")
      //   createCollection(name, owner) forward com.actorbase.actorsystem.storefinder.messages.Insert(key, value, update) // STUB needed for stress-test
      //                                                                                                                   // change context of an Insert is happening
      //   context.become(processingRequest)
      //   //item has not been inserted, must send the message to the brothers
      //   //TODO mandare agli altri main
      // }

    /**
      * Create a collection in the system
      *
      * @param name a String representing the name of the collection
      * @param owner a String representing the owner of the collection
      */
    case CreateCollection(name, owner) =>
      createCollection(name, owner)
      // TODO avvisare lo userkeeper che a sua volta deve avvisare il client

    /**
      * Remove a collection from the system
      *
      * @param name a String representing the name of the collection
      * @param owner a String representing the owner of the collection
      */
    case RemoveCollection(name, owner) =>
      //TODO questo messaggio dovrà rimuovere i file relativi agli storefinder tramite uso di warehouseman

    /**
      * Get item from collection message, given a key of type String, retrieve
      * a value from a specified collection
      *
      * @param collection a String representing the collection name
      * @param key a String representing the key to be retrieved
      */
    case GetItemFrom(collection, key) =>
      if (key.nonEmpty)
        sfMap.filterKeys(_.contains(key)).head._2 forward GetItem(key) // STUB needed for stress-test
      else {
        var collectionMap = Map[ActorbaseCollection, Map[String, Any]]()
        var items = Map[String, Any]()
        collectionMap += (collection -> items)
        requestMap += (sender -> collectionMap)
        sfMap.filterKeys(_.getCollectionName == collection.getName).foreach(kv => kv._2 ! GetAllItem(sender))
      }

    /**
      * Await for storefinder response of all storekeeper, expecting
      * a given number of response, equals to the number of key-value pairs
      * of the collection requested
      *
      * @param clientRef the reference of the client demanding the collection
      * @param collection an ActorbaseCollection item containing the number of response
      * expected for the requested collection at the current state
      * @param items a TreeMap[String, Any] representing a shard of the requested collection, represent a storekeeper payload
      * @return
      * @throws
      */
    case GetItemFromResponse(clientRef, collection, items) =>
      val clientMapPair = requestMap.find(_._1 == clientRef)
      clientMapPair match {
        case Some(refPair) =>
          log.info("GetItemFromResponse: refPairFound")
          refPair._2.find(_._1.compare(collection) == 0) match {
            case Some(colMap) =>
              log.info("GetItemFromResponse: collectionMapFound")
              refPair._2 -= collection
              items.foreach(kv => colMap._2 += (kv._1 -> kv._2))
              refPair._2.+(collection -> colMap._2)
              log.info(s"GetItemFromResponse: ${colMap._2.size} - ${collection.getSize}")
              if (colMap._2.size == collection.getSize)
                clientRef ! com.actorbase.actorsystem.clientactor.messages.MapResponse(collection.getName, colMap._2.toMap)
            case None => log.info("GetItemFromResponse: collectionMap not found")
          }
        case None => log.info("GetItemFromResponse: refPair not found")
      }

    /**
      * Remove item from collection  message, given a key of type String,
      * delete key-value pair from a specified collection
      *
      * @param collection a String representing the collection name
      * @param key a String representing the key to be deleted
      *
      */
    case RemoveItemFrom(collection, key) =>
      // TODO
      //sfMap.get(collection).get forward RemoveItem(key)

    /**
      * Add Contributor from collection, given username of Contributor and read
      * ore readWrite permission
      *
      * @param username a String to identify the contributor to add
      * @param permission a boolean representing the permission : (true = readWrite , false = readOnly)
      * @param collection a String representing the collection name
      *
      */
    case AddContributor(username , permission, collection) =>
      // need controls
      ufRef ! AddCollectionTo(username, permission, new ActorbaseCollection(collection, username))

    /**
      * Remove Contributor from collection, given username of Contributor , and permission
      *
      * @param username a String to identify the contributor to remove
      * @param permission a boolean representing the permission : (true = readWrite , false = readOnly)
      * @param collection a String representing the collection name
      *
      */
    case RemoveContributor(username, permission, collection) =>
      // need controls
      ufRef ! RemoveCollectionFrom(username, permission, new ActorbaseCollection(collection, username))

    case com.actorbase.actorsystem.main.messages.UpdateCollectionSize(collection, increment) =>
      log.info(s"MAIN: Update size ${collection.getOwner}")
      ufRef ! UpdateCollectionSizeTo(collection, increment)

    case DebugMaps => // debug purposes
      for( (collRange, sfRef )<- sfMap) {
        log.info("DEBUG MAIN "+collRange.toString)
        sfRef forward DebugMap( collRange.getKeyRange )
      }
  }

  /**
    * This method defines the type of messages that this actor can receive while in processingRequest status
    */
  private def processingRequest(): Actor.Receive = {

    /**
      * Ack (Acknowledge) message represent received when a blocking request has finished and the actor can
      * return to the waitingForRequests state
      */
    case Ack =>
      log.info("MAIN: ack")
      unstashAll()
      context.become(waitingForRequests)

    /**
      * A message that means that a Storefinder must duplicate.
      *
      * @param oldCollRange a CollectionRange representing the CollectionRange that needs to be duplicated
      * @param leftCollRange a CollectionRange representing the new CollectionRange of the duplicated one
      * @param map a TreeMap[KeyRange, ActorRef] that contains the data that needs to be used by the
      *            Storefinder that needs to be created
      * @param rightCollRange a CollectionRange representing the CollectionRange of the Storefinder that needs to
      *                       be created
      */
    case DuplicationRequestSF( oldCollRange, leftCollRange, map, rightCollRange ) =>
      log.info("MAIN: duplicateSFnotify "+oldCollRange+" leftcollrange "+leftCollRange+" rightcollrange "+rightCollRange)
      // update sfMap due to a SF duplicate happened
      val newSf = context.actorOf(Props(new Storefinder(oldCollRange.getCollection, map, rightCollRange.getKeyRange)).withDispatcher("control-aware-dispatcher") )
      // get old sk actorRef
      val tmpActorRef = sfMap.get(oldCollRange).get // refactor to getOrElse or match case
                                                    // remove entry associated with that actorRef
      sfMap -= oldCollRange // non so se sia meglio così o fare una specie di update key (che non c'è)
                            // add the entry with the oldSK and the new one
      sfMap += (leftCollRange -> tmpActorRef)
      sfMap += (rightCollRange -> newSf)

    /**
      * Any other message can't be processed while in this state so we just stash it
      */
    case _ =>
      stash()
  }
}
