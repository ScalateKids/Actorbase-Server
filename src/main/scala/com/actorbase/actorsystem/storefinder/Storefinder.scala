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

import scala.collection.immutable.TreeMap
import scala.math.Ordered.orderingToOrdered

object Storefinder {
  def props() : Props = Props(new Storefinder())
}

class Storefinder extends Actor with ActorLogging {

  // skMap maps string ranges to the sk reference
  private var skMap = new TreeMap[KeyRange, ActorRef]()
  // collection name
  private var collectionName: String = ""
  private var sfManager: ActorRef = _

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  def receive = {

    case com.actorbase.actorsystem.storefinder.messages.Init(name) => {
      log.info("SF: init")
      // initialize the collection name
      collectionName = name
      // create a manager and bind it to this actor, this Ref will be needed by the Storekeepers
      sfManager = context.actorOf(Props[Manager])

      /*
       val sk = context.actorOf(Storekeeper.props())
       // create a Manager and send his Ref to
       sk ! com.actorbase.actorsystem.storekeeper.messages.Init(context.actorOf(Props[Manager]))
       */
    }

    case DuplicateSKNotify(oldKeyRange, leftRange, newSk, rightRange) => {
      log.info("SF: DuplicateSKNotify")
      // update skMap due to a SK duplicate happened
      //scorrere skmap, trovare cosa aggiorare con leftrange
      // get old sk actorRef
      val tmpActorRef = skMap.get( oldKeyRange ).get
      // remove entry associated with that actorRef
      skMap = skMap - oldKeyRange   // non so se sia meglio così o fare una specie di update key (che non c'è)
                                    // add the entry with the oldSK and the new one
      skMap += (leftRange -> tmpActorRef)
      skMap += (rightRange -> newSk)
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
      log.info("SF: insert")
      skMap.size match {
        // empty TreeMap -> create SK and forward message to him
        case 0 => {
          val sk = context.actorOf(Storekeeper.props())
          val kr = new KeyRange("a", "z") // questo non va bene se lo SF si crea per sdoppiamentoooooooo
          skMap += (kr -> sk)
          // init the storekeeper passing the manager ref
          sk ! com.actorbase.actorsystem.storekeeper.messages.Init( sfManager, kr )
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

    case get: com.actorbase.actorsystem.storefinder.messages.GetItem => { //TODO implementare diversi tipi di getItem
      log.info(s"SF: getItem of key -> ${get.key}")
      // search for the right KeyRange to get the ActorRef of the needed SK
      for ((keyRange, sk) <- skMap){
        //log.info (keyRange.toString())
        if( keyRange.contains( get.key ) )
          sk forward com.actorbase.actorsystem.storekeeper.messages.GetItem(get.key)
      }
    }

    case com.actorbase.actorsystem.storefinder.messages.GetAllItem => {
      log.info("SF: getallitem")
      for ((keyRange, sk) <- skMap){
        sk forward com.actorbase.actorsystem.storekeeper.messages.GetAllItem
      }
    }

    case rem: com.actorbase.actorsystem.storefinder.messages.RemoveItem => {
      log.info("SF: remove")
      // search for the right KeyRange to get the ActorRef of the needed SK
      for ((keyRange, sk) <- skMap){
        //log.info (keyRange.toString())
        if( keyRange.contains( rem.key ) )
          sk forward com.actorbase.actorsystem.storekeeper.messages.RemoveItem(rem.key)
      }
    }

  }

}


/*object KeyRange{  forse no serve
 private var id = 0
 private def inc = {
 id+= 1
 id
 }
 }*/
class KeyRange(minR: String, maxR: String) extends Ordered[KeyRange] {
  //valutare se tenere così o mettere val e cambiare keyrange quando ci sono gli sdoppiamenti
  private var minRange: String = minR
  private var maxRange: String = maxR
  /* private val rangeId = KeyRange.inc

   def getId: Int = rangeId
   */
  def getMinRange: String = minRange

  def getMaxRange: String = maxRange

  def setMinRange(range: String) = minRange = range

  def setMaxRange(range: String) = maxRange = range

  // forse si può sostituire togliendo questo e facendo < getMax sullo SF (alby culalby)
  def contains(key: String): Boolean = (key >= minRange && key <= maxRange)

  override def toString: String = "from "+ minRange + " to " + maxRange

  //metodo trovato sull'internetto, da testare approfonditamente
  override def compare(that: KeyRange): Int = (this.minRange, this.maxRange) compare (that.minRange, that.maxRange)
}
