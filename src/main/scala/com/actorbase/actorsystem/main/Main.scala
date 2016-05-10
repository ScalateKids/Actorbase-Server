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

import akka.actor.{Actor, ActorLogging, ActorRef}
import spray.json.DefaultJsonProtocol._
import scala.collection.immutable.TreeMap

import com.actorbase.actorsystem.storefinder.Storefinder
import com.actorbase.actorsystem.storefinder.messages._

import com.actorbase.actorsystem.userfinder.Userfinder
import com.actorbase.actorsystem.userfinder.messages._

import com.actorbase.actorsystem.userkeeper.Userkeeper
import com.actorbase.actorsystem.userkeeper.Userkeeper.GetPassword

//impor per testing di ninja
import com.actorbase.actorsystem.ninja.Ninja
import com.actorbase.actorsystem.ninja.messages._

import com.actorbase.actorsystem.main.messages._

import com.actorbase.actorsystem.utils.{KeyRange, ActorbaseCollection, CollectionRange}

import com.github.t3hnar.bcrypt._
import org.mindrot.jbcrypt.BCrypt

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

  case class GetItemFrom(collection: String, key: String = "")

  // manca getcollection?

  case class RemoveItemFrom(collection: String, key: String)

  case class AddContributor(username: String, permission: Boolean = false , collection: String)

  case class RemoveContributor(username: String, permission: Boolean = false , collection: String)

  case class CreateCollection(name: String, owner: String) // basta così al momento

  case class RemoveCollection( name: String, owner: String)

  case object InitUsers

}

/**
  * Insert description here
  *
  * @param
  * @return
  * @throws
  */
class Main extends Actor with ActorLogging {
  import Main._

  private val ufRef: ActorRef = context.actorOf(Userfinder.props, "Userfinder") //TODO tutti devono avere lo stesso riferimento

  private var sfMap = new TreeMap[CollectionRange, ActorRef]()

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  def receive = {

    /**
      *
      */
    case Login(username) => ufRef forward GetPasswordOf(username)

    /** This message will probably populate username/password after disk read */
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
      * Insert message, insert a key/value into a designed collection
      *
      * @param collection a String representing the collection name
      * @param key a String representing the new key to be inserted
      * @param value a Any object type representing the value to be inserted
      * with associated key, default to Array[Byte] type
      * @param update a Boolean flag, define the insert behavior (with or without
      * updating the value)
      *
      */
    case Insert(owner, name, key, value, update) => {
      // search if sfMap contains the collection I need, if it's present search for the right keyrange
      var inserted: Boolean = false
      for( (collectionRange, sfRef) <- sfMap){
        if( collectionRange.isSameCollection(name, owner) && collectionRange.getKeyRange.contains(key) ){
          // right collection and right keyrange (right collectionRange), let's insert here
          log.info("inserting in the range "+collectionRange.toString)
          inserted = true
          sfRef forward com.actorbase.actorsystem.storefinder.messages.Insert( key, value, update )
          // TODO uscire dal for

          // TEST ACKNOWLEDGE
          /*context.become({
            case Ack =>
              log.info("MAIN: ack")
              context.unbecome() // resets the latest 'become'
          }, discardOld = false) // push on top instead of replace*/
        }
      }
      if( !inserted ){
        // TODO possibile problema futuro, al primo insert nessuno ha la collection e come si capisce chi deve crearla?
        log.info("item has not been inserted, must forward to siblings")
        createCollection(name, owner) forward com.actorbase.actorsystem.storefinder.messages.Insert(key, value, update) // STUB needed for stress-test
        //item has not been inserted, must send the message to the brothers
        //TODO mandare agli altri main
      }
    }

    /**
      *
      */
    case DuplicateSFNotify( oldCollRange, leftCollRange, newSf, rightCollRange ) => {
      log.info("MAIN: duplicateSFnotify ")
      // update sfMap due to a SF duplicate happened

      // get old sk actorRef
      val tmpActorRef = sfMap.get(oldCollRange).get
      // remove entry associated with that actorRef
      sfMap = sfMap - oldCollRange // non so se sia meglio così o fare una specie di update key (che non c'è)
      // add the entry with the oldSK and the new one
      sfMap += (leftCollRange -> tmpActorRef)
      sfMap += (rightCollRange -> newSf)
    }

    /**
      *
      */
    case CreateCollection(name, owner) => {
      createCollection(name, owner)
      // TODO avvisare lo userkeeper che a sua volta deve avvisare il client
    }

    /**
      *
      */
    case RemoveCollection(name, owner) => {
      //TODO da implementare
    }

    /**
      * Get item from collection  message, given a key of type String, retrieve
      * a value from a specified collection
      *
      * @param collection a String representing the collection name
      * @param key a String representing the key to be retrieved
      *
      */
    case GetItemFrom(collection, key) =>
      if (key.nonEmpty)
        sfMap.filterKeys(_.contains(key)).head._2 forward GetItem(key) // STUB needed for stress-test
      else sfMap.filterKeys(_.getCollectionName == collection).foreach(kv => kv._2 ! GetAllItem(sender))
      // TODO
/*      if(key == "")
        sfMap.get(collection).get forward GetAllItem
      else
        sfMap.get(collection).get forward GetItem(key)
 */

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
      * Add Contributor from collection , given username of Contributor and read
      * ore readWrite permission
      *
      * @param username a String to identify the contributor to add
      * @param permission a boolean representing the permission : (true = readWrite , false = readOnly)
      * @param collection a String representing the collection name
      *
      */
    case AddContributor(username , permission, collection) =>
      // need controls
      ufRef ! AddCollectionTo(username,permission,collection)

    /**
      * Remove Contributor from collection , given username of Contributor , and permission
      *
      * @param username a String to identify the contributor to remove
      * @param permission a boolean representing the permission : (true = readWrite , false = readOnly)
      * @param collection a String representing the collection name
      *
      */
    case RemoveContributor(username,permission,collection) =>
      // need controls
      ufRef ! RemoveCollectionFrom(username,permission,collection)

    case DebugMaps => // debug purposes
      for( (collRange, sfRef )<- sfMap){
        log.info("DEBUG MAIN "+collRange.toString)
        sfRef forward DebugMap( collRange.getKeyRange )
      }
  }

  /**
    *
    * @param name
    * @param owner
    */
  private def createCollection(name: String, owner: String): ActorRef = {
    val sf = context.actorOf(Storefinder.props( self, new ActorbaseCollection(name, owner ) ).withDispatcher("control-aware-dispatcher"))
    var newCollectionRange = new CollectionRange( new ActorbaseCollection(name, owner), new KeyRange("a", "z")) //TODO CAMBIARE Z CON MAX
    sfMap += (newCollectionRange -> sf)
    sf
  }
}
