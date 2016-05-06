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
import com.actorbase.actorsystem.storefinder.messages._
import com.actorbase.actorsystem.storekeeper.messages._
import com.actorbase.actorsystem.storekeeper.Storekeeper
import com.actorbase.actorsystem.manager.Manager
import com.actorbase.actorsystem.manager.messages.DuplicationRequestSF
import com.actorbase.actorsystem.utils.{KeyRange, ActorbaseCollection, CollectionRange}

import scala.collection.immutable.TreeMap
import scala.math.Ordered.orderingToOrdered

object Storefinder {
  def props( mainParent: ActorRef, collection: ActorbaseCollection ) : Props = Props(new Storefinder( mainParent, collection ))
}

/**
  *
  * @param skMap TreeMap[KeyRange, ActorRef]. This collection represent a map from keyranges to an ActorRef of a
  *              Storekeeper
  * @param range String that represent the range of the keys mappable in this storefinder
  * @param sfManager
  */
class Storefinder(private val mainParent: ActorRef,
                  private var collection: ActorbaseCollection,
                  private var skMap : TreeMap[KeyRange, ActorRef] = new TreeMap[KeyRange, ActorRef](),
                  private var range: KeyRange = new KeyRange("a", "z") ) extends Actor with ActorLogging {

  // collection name

  // initialize his manager
  private val sfManager: ActorRef = context.actorOf(Manager.props())
  private val maxSize: Int = 4

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
    case com.actorbase.actorsystem.storefinder.messages.Init(name, manager, range) => {
      log.info("SF: init")
    }

    /**
      *
      */
    case DuplicateSKNotify(oldKeyRange, leftRange, newSk, rightRange) => {  //TODO CODICE MOLTO REPLICATO FROM SK
      log.info("SF: DuplicateSKNotify "+" oldKeyRange "+oldKeyRange+" leftRange "+leftRange+" rightRange "+rightRange)
      for( (range, ref) <- skMap){
        log.info(range.toString)
      }
      // update skMap due to a SK duplicate happened
      //scorrere skmap, trovare cosa aggiorare con leftrange
      // get old sk actorRef
      val tmpActorRef = skMap.get(oldKeyRange).get
      // remove entry associated with that actorRef
      skMap = skMap - oldKeyRange // non so se sia meglio così o fare una specie di update key (che non c'è)
      // add the entry with the oldSK and the new one
      skMap += (leftRange -> tmpActorRef)
      skMap += (rightRange -> newSk)

      if(skMap.size == maxSize-1 ){
        log.info("SF: Must duplicate")
        //TODO
        // half the collection
        var (halfLeft, halfRight) = skMap.splitAt( maxSize/2 )
        log.info("SF duplication halfleft first key"+halfLeft.firstKey+" last key "+halfLeft.lastKey)
        log.info("SF duplication halfright first key"+halfRight.firstKey+" last key "+halfRight.lastKey)
        // create new keyrange to be updated for SF
        val halfLeftCollRange = new CollectionRange( collection, new KeyRange(halfLeft.firstKey.getMinRange, halfLeft.lastKey.getMaxRange/*+"a"*/) )
        // create new keyrange for the new storekeeper
        // old was val halfRightCollRange = new CollectionRange( collection, new KeyRange(halfLeft.lastKey.getMinRange/*+"aa"*/, halfRight.lastKey.getMaxRange) )
        val halfRightCollRange = new CollectionRange( collection, new KeyRange(halfRight.firstKey.getMinRange, halfRight.lastKey.getMaxRange) )

        // send the request at manager with the treemap, old keyrangeId, new keyrange, collection of the new SK and
        // keyrange of the new sk
        log.info("left SF key range "+halfLeftCollRange+" right SF key range "+halfRightCollRange+" maps are below")
        log.info("left is ")
        for( (range, ref) <- halfLeft){
          log.info(range.toString)
        }
        log.info("right is ")
        for( (range, ref) <- halfRight){
          log.info(range.toString)
        }

        //x debug
        val range2 = new KeyRange(range.getMinRange, range.getMaxRange)
        sfManager ! DuplicationRequestSF( new CollectionRange(collection, range2), halfLeftCollRange, halfRight, halfRightCollRange, mainParent )

        // update keyRangeId or himself and set the treemap to the first half
        skMap = halfLeft
        range = new KeyRange(halfLeft.firstKey.getMinRange, halfLeft.lastKey.getMaxRange/*+"a"*/)
      }
    }

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
    case ins: com.actorbase.actorsystem.storefinder.messages.Insert => {
      log.info("SF: inserting "+ins.key+" - KeyRange of this SF is "+range)
      skMap.size match {
        // empty TreeMap -> create SK and forward message to him
        case 0 => {
          val kr = new KeyRange("a", "z") // pensare se questo vabene nel caso di sdoppiamentooo
          val sk = context.actorOf(Storekeeper.props( sfManager, new TreeMap[String, Any](), kr ))
          // update map
          skMap += (kr -> sk)
          // forward the request to the sk just created
          sk forward com.actorbase.actorsystem.storekeeper.messages.Insert(ins.key, ins.value, ins.update)
        }
        // TreeMap not empty -> search which SK has the right KeyRange for the item to insert
        case _ => {
          for ((keyRange, sk) <- skMap){
            //log.info (keyRange.toString())
            if( keyRange.contains( ins.key ) )
              sk forward com.actorbase.actorsystem.storekeeper.messages.Insert(ins.key, ins.value, ins.update)
          }
        }
      }
    }

    /**
      *
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
      *
      */
    case com.actorbase.actorsystem.storefinder.messages.GetAllItem => {
      log.info("SF: getallitem")
      for ((keyRange, sk) <- skMap){
        sk forward com.actorbase.actorsystem.storekeeper.messages.GetAllItem
      }
    }

    /**
      *
      */
    case rem: com.actorbase.actorsystem.storefinder.messages.RemoveItem => {
      log.info("SF: remove")
      // search for the right KeyRange to get the ActorRef of the needed SK
      for ((keyRange, sk) <- skMap){
        //log.info (keyRange.toString())
        if( keyRange.contains( rem.key ) )
          sk forward com.actorbase.actorsystem.storekeeper.messages.RemoveItem(rem.key)
      }
    }

      // debug purposes
    case DebugMap => {
      var i = 0
      for( (range, skRef) <- skMap){
        log.info("DEBUG S-FINDER "+i+" "+range.toString)
        skRef forward DebugMaa(i)
        i += 1
      }
    }
  }

}