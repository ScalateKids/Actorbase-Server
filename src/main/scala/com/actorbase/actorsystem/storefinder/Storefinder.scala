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

// TEMPORARY BRIDGE BETWEEN MAIN AND STOREKEEPER

package com.actorbase.actorsystem.storefinder

import akka.actor.{Actor, ActorRef, ActorLogging, Props}
import akka.actor.Stash

import com.actorbase.actorsystem.storefinder.messages._
import com.actorbase.actorsystem.storekeeper.messages._
import com.actorbase.actorsystem.storekeeper.Storekeeper
import com.actorbase.actorsystem.warehouseman.Warehouseman
import com.actorbase.actorsystem.warehouseman.messages.RemoveSfFolder

import com.actorbase.actorsystem.utils.{ActorbaseCollection, CollectionRange, KeyRange}

import scala.collection.immutable.{TreeMap}

object Storefinder {

  def props( collection: ActorbaseCollection ) : Props = Props(new Storefinder( collection ))

  def props( collection: ActorbaseCollection, map: TreeMap[KeyRange, ActorRef],
    keyrange: KeyRange) : Props = Props(new Storefinder( collection, map, keyrange ))
}

/**
  *
  * @param skMap TreeMap[KeyRange, ActorRef]. This collection represent a map from keyranges to an ActorRef of a
  *              Storekeeper
  * @param range String that represent the range of the keys mappable in this storefinder
  * @param
  */
class Storefinder(private var collection: ActorbaseCollection,
  private var skMap : TreeMap[KeyRange, ActorRef] = new TreeMap[KeyRange, ActorRef](),
  private var range: KeyRange = new KeyRange("a", "z") ) extends Actor with ActorLogging with Stash{

  // firstly we need to update the owner of the SK of his map, this is necessary when the SF is created due
  // to a duplication
  updateOwnerOfSK()
  private val maxSize: Int = 64
  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  def receive = waitingForRequests

  /**
    * Method that update the owner of all the Storekeepers mapped by this Storefinder, used when a Storefinder
    * is created due to a duplication
    */
  private def updateOwnerOfSK(): Unit = {
    for((r, skref) <- skMap){
      skref ! com.actorbase.actorsystem.storekeeper.messages.updateOwnerOfSK( self, range )
    }
  }


  /**
    * This method defines the type of messages that this actor can receive while in waitingForRequests status
    */
  private def waitingForRequests(): Actor.Receive = {

    /**
      *
      */
    case com.actorbase.actorsystem.storefinder.messages.Init(name, manager, range) =>
      log.info("SF: init")


    /**
      * Insert message, insert a key/value into a designed collection
      *
      * @param key a String representing the new key to be inserted
      * @param value a Any object type representing the value to be inserted
      * with associated key, default to Array[Byte] type
      * @param update a Boolean flag, define the insert behavior (with or without
      * updating the value)
      *
      */
    case ins: com.actorbase.actorsystem.storefinder.messages.Insert =>
      log.info("SF: inserting "+ins.key+" - KeyRange of this SF is "+range+" size of this SF is "+skMap.size)
      skMap.size match {
        // empty TreeMap -> create SK and forward message to him
        case 0 =>
          val kr = new KeyRange("a", "z") // pensare se questo vabene nel caso di sdoppiamentooo
          val sk = context.actorOf(Storekeeper.props( self, collection, range, new TreeMap[String, Any](), kr ).withDispatcher("control-aware-dispatcher"))
          // update map
          skMap += (kr -> sk)
          // forward the request to the sk just created
          sk forward com.actorbase.actorsystem.storekeeper.messages.Insert(ins.key, ins.value, ins.update)
          context.become(processingRequest)
        // TreeMap not empty -> search which SK has the right KeyRange for the item to insert
        case _ =>
          skMap.find(_._1.contains(ins.key)).head._2 forward com.actorbase.actorsystem.storekeeper.messages.Insert(ins.key, ins.value, ins.update)
          context.become(processingRequest)
          // for ((keyRange, sk) <- skMap){
          //   //log.info (keyRange.toString())
          //   if( keyRange.contains( ins.key ) ) {
          //     sk forward com.actorbase.actorsystem.storekeeper.messages.Insert(ins.key, ins.value, ins.update)
          //     context.become(processingRequest)
          //   }
          // }
      }

    /**
      * Message that search for a given key
      */
    case get: com.actorbase.actorsystem.storefinder.messages.GetItem => { //TODO implementare diversi tipi di getItem
      log.info(s"SF: getItem of key -> ${get.key}")
      // search for the right KeyRange to get the ActorRef of the needed SK
      for ((keyRange, sk) <- skMap){
        //log.info (keyRange.toString())
        if( keyRange.contains( get.key ) )
          sk forward com.actorbase.actorsystem.storekeeper.messages.GetItem(get.key)
      }
    }

    /**
      * Message that returns the entire collection mapped by this Storefinder
      */
    case com.actorbase.actorsystem.storefinder.messages.GetAllItem(clientRef) =>
      log.info("SF: getallitem")
      for ((keyRange, sk) <- skMap) {
        sk ! com.actorbase.actorsystem.storekeeper.messages.GetAllItem( clientRef )
      }

    /**
      * Message that removes an item with the given key
      *
      * @param key a String representing the key of the item to be removed
      */
    case rem: com.actorbase.actorsystem.storefinder.messages.RemoveItem =>
      log.info("SF: remove")
      // search for the right KeyRange to get the ActorRef of the needed SK
      for ((keyRange, sk) <- skMap) {
        //log.info (keyRange.toString())
        if( keyRange.contains( rem.key ) )
          sk forward com.actorbase.actorsystem.storekeeper.messages.RemoveItem(rem.key)
      }

    /**
      *
      */
    case UpdateCollectionSize(increment) =>
      log.info(s"SF: Update size ${collection.getOwner}")
      context.parent ! com.actorbase.actorsystem.main.messages.UpdateCollectionSize(collection, increment)

    /**
      *
      */
    case GetAllItemResponse(clientRef, items) =>
      context.parent ! com.actorbase.actorsystem.main.Main.GetItemFromResponse(clientRef, collection, items)

    // debug purposes
    case DebugMap( mainRange ) =>
      var i = 0
      for((range, skRef) <- skMap) {
        //log.info("DEBUG S-FINDER "+"(main"+mainRange+") "+range.toString/*+" size of this SF is "+skMap.size*/)
        skRef forward DebugMaa(mainRange, range)
        i += 1
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
    case com.actorbase.actorsystem.main.messages.Ack =>
      log.info("SF: ack")
      unstashAll()
      context.become(waitingForRequests) // resets the latest 'become'
      context.parent ! com.actorbase.actorsystem.main.messages.Ack


    /**
      * A message that means that a Storekeeper must duplicate.
      *
      * @param oldKeyRange a KeyRange representing the KeyRange that needs to be duplicated
      * @param leftKeyRange a KeyRange representing the new KeyRange of the duplicated one
      * @param map a TreeMap[String, Any] that contains the data that needs to be used by the
      *            Storekeeper that needs to be created
      * @param rightKeyRange a KeyRange representing the KeyRange of the Storekeeper that needs to
      *                       be created
      */
    case DuplicationRequestSK(oldKeyRange, leftRange, map, rightRange) =>  //TODO CODICE MOLTO REPLICATO FROM SK
      log.info("SF: DuplicateSKNotify "+oldKeyRange+" left "+leftRange+" right "+rightRange)
      // need to update skMap due to a SK duplicate happened

      val newSk = context.actorOf(Props(new Storekeeper(self, collection, range, map, rightRange)).withDispatcher("control-aware-dispatcher") )

      // get old sk actorRef
      val tmpActorRef = skMap.get(oldKeyRange).get
      // remove entry associated with that actorRef
      skMap = skMap - oldKeyRange // non so se sia meglio così o fare una specie di update key (che non c'è)
                                  // add the entry with the oldSK and the new one
      skMap += (leftRange -> tmpActorRef)
      skMap += (rightRange -> newSk)

      // if I'm at max size i should duplicate
      if(skMap.size == maxSize ){
        log.info("SF: Must duplicate")
        // half the collection
        var (halfLeft, halfRight) = skMap.splitAt( maxSize/2 )

        // create new keyrange to be updated for SF
        val halfLeftCollRange = new CollectionRange( collection, new KeyRange(halfLeft.firstKey.getMinRange, halfLeft.lastKey.getMaxRange/*+"a"*/) )
        // create new keyrange for the new storefinder
        val halfRightCollRange = new CollectionRange( collection, new KeyRange(halfRight.firstKey.getMinRange, halfRight.lastKey.getMaxRange) )

        context.parent ! com.actorbase.actorsystem.main.messages.DuplicationRequestSF( new CollectionRange(collection, range), halfLeftCollRange, halfRight, halfRightCollRange)

        // Create a warehouseman just to remove the old folder
        context.actorOf(Warehouseman.props( collection.getName+"-"+collection.getOwner )) ! RemoveSfFolder( range )

        // Update keyRangeId or himself and set the treemap to the first half
        skMap = halfLeft
        range = new KeyRange(halfLeft.firstKey.getMinRange, halfLeft.lastKey.getMaxRange/*+"a"*/)

        // updateownerofSK is useful to delete all the junky files created by previous saves.
        updateOwnerOfSK()

        context.parent ! com.actorbase.actorsystem.main.messages.Ack
      }

    /**
      * Any other message can't be processed while in this state so we just stash it
      */
    case _ =>
      log.info("SF stashing")
      stash()
  }
}
