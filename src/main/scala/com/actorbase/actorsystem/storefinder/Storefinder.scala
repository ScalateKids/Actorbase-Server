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

  // collection name

  // initialize his manager
  //private val sfManager: ActorRef = context.actorOf(Manager.props())
  // maybe move this things to a costructor or something like a init?

  /*
  private val sfManager: ActorRef = context.actorOf(Props(new Manager( self )))
  updateManagerOfSK()*/

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
          val sk = context.actorOf(Storekeeper.props( /*sfManager, */new TreeMap[String, Any](), kr ).withDispatcher("control-aware-dispatcher"))
          // update map
          skMap += (kr -> sk)
          // forward the request to the sk just created
          sk forward com.actorbase.actorsystem.storekeeper.messages.Insert(ins.key, ins.value, ins.update)
          // TEST ACKNOWLEDGE
          context.become({
            case com.actorbase.actorsystem.main.messages.Ack =>
              log.info("SF: ack")
              context.unbecome() // resets the latest 'become'
              unstashAll()
              context.parent ! com.actorbase.actorsystem.main.messages.Ack
            case _ =>
              log.info("SF stashing")
              stash()
          }, discardOld = false) // push on top instead of replace
        }
        // TreeMap not empty -> search which SK has the right KeyRange for the item to insert
        case _ => {
          for ((keyRange, sk) <- skMap){
            //log.info (keyRange.toString())
            if( keyRange.contains( ins.key ) ) {
              sk forward com.actorbase.actorsystem.storekeeper.messages.Insert(ins.key, ins.value, ins.update)
              // TEST ACKNOWLEDGE
              context.become({
                case com.actorbase.actorsystem.main.messages.Ack =>
                  log.info("SF: ack")
                  context.unbecome() // resets the latest 'become'
                  unstashAll()
                  context.parent ! com.actorbase.actorsystem.main.messages.Ack

                case DuplicationRequestSK(oldKeyRange, leftRange, map, rightRange) =>  //TODO CODICE MOLTO REPLICATO FROM SK
                  log.info("SF: DuplicateSKNotify "+oldKeyRange+" left "+leftRange+" right "+rightRange)
                  // need to update skMap due to a SK duplicate happened

                  val newSk = context.actorOf(Props(new Storekeeper( map, rightRange)).withDispatcher("control-aware-dispatcher") )

                  // get old sk actorRef
                  val tmpActorRef = skMap.get(oldKeyRange).get
                  // remove entry associated with that actorRef
                  skMap = skMap - oldKeyRange // non so se sia meglio così o fare una specie di update key (che non c'è)
                  // add the entry with the oldSK and the new one
                  skMap += (leftRange -> tmpActorRef)
                  skMap += (rightRange -> newSk)

                  // if I'm close to the max size i should duplicate
                  if(skMap.size == maxSize-1 ){
                    log.info("SF: Must duplicate")
                    // half the collection
                    var (halfLeft, halfRight) = skMap.splitAt( maxSize/2 )

                    // create new keyrange to be updated for SF
                    val halfLeftCollRange = new CollectionRange( collection, new KeyRange(halfLeft.firstKey.getMinRange, halfLeft.lastKey.getMaxRange/*+"a"*/) )
                    // create new keyrange for the new storefinder
                    val halfRightCollRange = new CollectionRange( collection, new KeyRange(halfRight.firstKey.getMinRange, halfRight.lastKey.getMaxRange) )

                    // send the request at manager with the old CollectionRange (the one who's duplicating), the new
                    // collectionrange, the treemap of the new SF to be created, the collectionRange of the SF to be created
                    // and the main parent reference
                    //        sfManager ! DuplicationRequestSF( new CollectionRange(collection, range), halfLeftCollRange, halfRight, halfRightCollRange, mainParent )

                    context.parent ! com.actorbase.actorsystem.main.messages.DuplicationRequestSF( new CollectionRange(collection, range), halfLeftCollRange, halfRight, halfRightCollRange)
                    // update keyRangeId or himself and set the treemap to the first half
                    skMap = halfLeft
                    range = new KeyRange(halfLeft.firstKey.getMinRange, halfLeft.lastKey.getMaxRange/*+"a"*/)
                  }
                  context.parent ! com.actorbase.actorsystem.main.messages.Ack

                case _ =>
                  log.info("SF stashing")
                  stash()
              }, discardOld = false) // push on top instead of replace
            }
          }
        }
      }
    }

    /**
      *
      */
    /*case DuplicationRequestSK(oldKeyRange, leftRange, map, rightRange) => {  //TODO CODICE MOLTO REPLICATO FROM SK
      log.info("SF: DuplicateSKNotify "+oldKeyRange+" left "+leftRange+" right "+rightRange)
      // need to update skMap due to a SK duplicate happened

      val newSk = context.actorOf(Props(new Storekeeper( map, rightRange)).withDispatcher("control-aware-dispatcher") )

      // get old sk actorRef
      val tmpActorRef = skMap.get(oldKeyRange).get
      // remove entry associated with that actorRef
      skMap = skMap - oldKeyRange // non so se sia meglio così o fare una specie di update key (che non c'è)
      // add the entry with the oldSK and the new one
      skMap += (leftRange -> tmpActorRef)
      skMap += (rightRange -> newSk)

      // if I'm close to the max size i should duplicate
      if(skMap.size == maxSize-1 ){
        log.info("SF: Must duplicate")
        // half the collection
        var (halfLeft, halfRight) = skMap.splitAt( maxSize/2 )

        // create new keyrange to be updated for SF
        val halfLeftCollRange = new CollectionRange( collection, new KeyRange(halfLeft.firstKey.getMinRange, halfLeft.lastKey.getMaxRange/*+"a"*/) )
        // create new keyrange for the new storefinder
        val halfRightCollRange = new CollectionRange( collection, new KeyRange(halfRight.firstKey.getMinRange, halfRight.lastKey.getMaxRange) )

        // send the request at manager with the old CollectionRange (the one who's duplicating), the new
        // collectionrange, the treemap of the new SF to be created, the collectionRange of the SF to be created
        // and the main parent reference
//        sfManager ! DuplicationRequestSF( new CollectionRange(collection, range), halfLeftCollRange, halfRight, halfRightCollRange, mainParent )

        context.parent ! com.actorbase.actorsystem.main.messages.Ack
        context.parent ! com.actorbase.actorsystem.main.messages.DuplicationRequestSF( new CollectionRange(collection, range), halfLeftCollRange, halfRight, halfRightCollRange)
        // update keyRangeId or himself and set the treemap to the first half
        skMap = halfLeft
        range = new KeyRange(halfLeft.firstKey.getMinRange, halfLeft.lastKey.getMaxRange/*+"a"*/)
      }
      else
        context.parent ! com.actorbase.actorsystem.main.messages.Ack
    }*/

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
    case com.actorbase.actorsystem.storefinder.messages.GetAllItem(clientRef) => {
      log.info("SF: getallitem")
      for ((keyRange, sk) <- skMap){
        sk ! com.actorbase.actorsystem.storekeeper.messages.GetAllItem( clientRef )
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
    case DebugMap( mainRange ) => {
      var i = 0
      for( (range, skRef) <- skMap){
        //log.info("DEBUG S-FINDER "+"(main"+mainRange+") "+range.toString/*+" size of this SF is "+skMap.size*/)
        skRef forward DebugMaa(mainRange, range)
        i += 1
      }
    }
  }

  /*def updateManagerOfSK(): Unit = {
    for((r, skref) <- skMap){
      println("updating sk manager")
      skref ! UpdateManager( sfManager )
    }
  }*/

}