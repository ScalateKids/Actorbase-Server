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

package com.actorbase.actorsystem.storekeeper

import akka.actor.{Props, Actor, ActorLogging, ActorRef}

import com.actorbase.actorsystem.manager.Manager
import com.actorbase.actorsystem.manager.messages.DuplicationRequestSK
import com.actorbase.actorsystem.storekeeper.messages._
import com.actorbase.actorsystem.clientactor.messages.{MapResponse, Response}
import com.actorbase.actorsystem.storefinder.KeyRange

import scala.collection.immutable.TreeMap

object Storekeeper {

  def props() : Props = Props(new Storekeeper())
}

/**
  * Insert description here
  *
  * @param
  * @return
  * @throws
  */
class Storekeeper(private var data: TreeMap[String, Any] = new TreeMap[String, Any]()) extends Actor with ActorLogging {

  private var manager : ActorRef = _
  private var range : KeyRange = _
  private val maxSize: Int = 50  // this should be configurable, probably must read from file

  def receive = {
    case Init => {
      log.info("SK: init")
      // initialize manager reference, will be useful when this sk has to duplicate himself
      this.manager = manager
      this.range = range
    }

    /**
      * GetItem message, will send back a value associated with the input key
      *
      * @param key a String representing the key of the item to be returned (sta roba sarà da cambiare)
      *
       */
    case getItem: GetItem  => {
      sender ! data.get(getItem.key).getOrElse("None").asInstanceOf[Array[Byte]]
      //sender ! Response(data.get(getItem.key).getOrElse("None").toString())
    }
    case GetAllItem => {
      val items = data
      sender ! MapResponse("collectionName", items) // need marshalling
    }
    case rem: RemoveItem => {
      data -= rem.key
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
    case ins: Insert => {
      log.info("SK: Insert")
      if(data.size < maxSize-1 ) {
        insertOrUpdate( ins.update, ins.key, ins.value)
      }
      else {
        log.info("SK: Must duplicate")
        /*  what
        // ugly as fuck, to be improved
        var (halfLeft, halfRight) = data.splitAt(25)
        // save first and last key from halved collection
        var firstKey = halfRight.firstKey
        var lastKey = halfLeft.lastKey
        // save first and last value associated with first and last key
        val firstValue = halfRight.get(firstKey)
        val lastValue = halfRight.get(lastKey)
        // update first and last key
        halfRight -= firstKey
        halfLeft -= lastKey
        halfLeft += (lastKey + "a" -> lastValue)
        halfRight += (firstKey + "b" -> firstValue)
        // update data and call for manager */

        // insert the item, then we will duplicate
        insertOrUpdate( ins.update, ins.key, ins.value)
        // half the collection
        var (halfLeft, halfRight) = data.splitAt( maxSize/2 )
        // create new keyrange to be updated for SF
        val halfLeftKR = new com.actorbase.actorsystem.storefinder.KeyRange( halfLeft.firstKey, halfLeft.lastKey+"a" )
        // create new keyrange for the new storekeeper
        val halfRightKR = new com.actorbase.actorsystem.storefinder.KeyRange( halfLeft.lastKey+"b", halfRight.lastKey )
        // set the treemap to the first half
        data = halfLeft
        // send the request at manager with the treemap, old keyrangeId, new keyrange, collection of the new SK and
        // keyrange of the new sk
        manager ! DuplicationRequestSK(range, halfLeftKR, halfRight, halfRightKR)
        // update keyRangeId or himself
        range = halfLeftKR
      }
      //sender ! Response("inserted")
    }
  }

  private def insertOrUpdate(update: Boolean, key: String, value: Any): Unit = {
    if(update)
      data += (key -> value)
    else if(!update && !data.contains(key))
      data += (key -> value)
    else if(!update && data.contains(key))
      log.info("SK: Duplicate key found, cannot insert")
  }

}
