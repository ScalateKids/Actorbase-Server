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

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorLogging
import akka.actor.Props
import com.actorbase.actorsystem.storefinder.messages.DuplicateRequest
import com.actorbase.actorsystem.storekeeper.messages._
import com.actorbase.actorsystem.storekeeper.Storekeeper

import scala.collection.immutable.TreeMap

object Storefinder {
  def props() : Props = Props(new Storefinder())
}

class Storefinder extends Actor with ActorLogging {

  // skMap maps string ranges to the sk reference
  var skMap = new TreeMap[KeyRange, ActorRef]()

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  def receive = {
    case com.actorbase.actorsystem.storefinder.messages.Init => {
      log.info("SF: init")
      val sk = context.actorOf(Storekeeper.props())
      sk ! com.actorbase.actorsystem.storekeeper.messages.Init
    }

    case DuplicateRequest => {  //cambiare nome in DuplicateNotify o qualcosa del genere?
      val sk = context.actorOf(Storekeeper.props())
      log.info("uno storekeeper è stato sdoppiato (not really but still, that's the idea)")
    }

    // INSERT item
    case ins: com.actorbase.actorsystem.storefinder.messages.Insert => {
      log.info("SF: insert")
      skMap.size match {
          // empty TreeMap -> create SK and forward message to him
        case 0 => {
          val sk = context.actorOf(Storekeeper.props())
          skMap += (new KeyRange("aaa","zzz") -> sk)    // questo non va bene se lo SF si crea per sdoppiamentoooooooo
          sk ! com.actorbase.actorsystem.storekeeper.messages.Insert(ins.key, ins.value, ins.ref)
        }
          // TreeMap not empty -> search which SK has the right KeyRange for the item to insert
        case _ => {
          for ((keyRange, sk) <- skMap){
            //log.info (keyRange.toString())
            if( keyRange.contains( ins.key ) )
              sk ! com.actorbase.actorsystem.storekeeper.messages.Insert(ins.key, ins.value, ins.ref)
          }
        }
      }
    }

    case get: com.actorbase.actorsystem.storefinder.messages.GetItem => { //TODO implementare diversi tipi di getItem
      log.info("SF: getItem")
      /*val sk = context.actorOf(Storekeeper.props())
       if(get.key == "") {
       log.info("SF: get all storekeeper")
       sk.!(GetAllItem)
       }
       else {
       log.info("SF: get one item")
       sk ! com.actorbase.actorsystem.storekeeper.messages.GetItem(get.key)
       }*/
      // search for the right KeyRange to get the ActorRef of the needed SK
      for ((keyRange, sk) <- skMap){
        //log.info (keyRange.toString())
        if( keyRange.contains( get.key ) )
          sk ! com.actorbase.actorsystem.storekeeper.messages.GetItem(get.key, get.ref)
      }
    }

    case rem: com.actorbase.actorsystem.storefinder.messages.RemoveItem => {
      log.info("SF: remove")
      /*val sk = context.actorOf(Storekeeper.props())
       sk ! com.actorbase.actorsystem.storekeeper.messages.RemoveItem(rem.key)*/
      // search for the right KeyRange to get the ActorRef of the needed SK
      for ((keyRange, sk) <- skMap){
        //log.info (keyRange.toString())
        if( keyRange.contains( rem.key ) )
          sk ! com.actorbase.actorsystem.storekeeper.messages.RemoveItem(rem.key, rem.ref)
      }
    }

  }

}


class KeyRange(minR: String, maxR: String) extends Ordered[KeyRange]{

  private var minRange: String = minR
  private var maxRange: String = maxR

  def getMinRange: String = {
    minRange
  }

  def getMaxRange: String = {
    maxRange
  }

  def setMinRange(range: String) = {
    minRange = range
  }

  def setMaxRange(range: String) = {
    maxRange = range
  }

  def contains(key: String): Boolean = {  // forse si può sostituire togliendo questo e facendo < getMax sullo SF (marculo)
    if(key >= minRange && key <= maxRange)
      return true
    else
      return false
  }

  override def toString: String = {
    return "from "+ minRange + " to " + maxRange
  }

  // TODO tutti da fare
  override def <(range: KeyRange): Boolean = {
    return true
  }
  override def >(range: KeyRange): Boolean = {
    return true
  }
  override def <=(range: KeyRange): Boolean = {
    return true
  }
  override def >=(range: KeyRange): Boolean = {
    return true
  }
  override def compareTo(range: KeyRange): Int = {
    return 1
  }
  override def compare(that: KeyRange): Int =  {
    return -1 }
}
