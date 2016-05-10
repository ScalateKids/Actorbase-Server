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

import akka.actor.{Actor, ActorRef, ActorLogging, Props}

/*import com.actorbase.actorsystem.manager.Manager
import com.actorbase.actorsystem.manager.messages.DuplicationRequestSK
*/
import com.actorbase.actorsystem.storekeeper.messages._

import com.actorbase.actorsystem.clientactor.messages.{MapResponse, Response}

import com.actorbase.actorsystem.utils.KeyRange

import scala.collection.immutable.TreeMap

import com.actorbase.actorsystem.warehouseman.Warehouseman

object Storekeeper {
  //def props() : Props = Props( new Storekeeper())
  def props(data: TreeMap[String, Any], range: KeyRange ) : Props = Props( new Storekeeper(data, range))
  def props() : Props = Props( new Storekeeper())
}

/**
  *
  * @param data
  * @param manager
  * @param range
  * @param maxSize
  */
class Storekeeper(private var data: TreeMap[String, Any] = new TreeMap[String, Any](),
                  private var range: KeyRange = new KeyRange("a","z")) extends Actor with ActorLogging {

  private val maxSize: Int = 2  // this should be configurable, probably must read from file

  def receive = {
    /**
      * ???
      */
    case Init => {
      log.info("SK: init")
    }

    /**
      * GetItem message, this actor will send back a value associated with the input key
      *
      * @param key a String representing the key of the item to be returned (sta roba sarà da cambiare)
      *
       */
    case getItem: GetItem  =>
      // sender ! data.get(getItem.key).getOrElse("None").asInstanceOf[Array[Byte]]
      sender ! Response(data.get(getItem.key).getOrElse("None").toString())

    /**
      * GetAllItem message, this actor will send back the collection name and all the collection.
       */
    case GetAllItem(clientRef) =>
      // TODO
      log.info("SK GetAlLItems")
      val items = data  // non si può mandargli data?
      sender ! com.actorbase.actorsystem.clientactor.messages.MapResponse("customers", items)//com.actorbase.actorsystem.storefinder.messages.TakeMyItems(clientRef, items)

    /**
      * RemoveItem message, when the actor receive this message it will erase the item associated with the
      * key in input. This method doesn't throw an exception if the item is not present.
      */
    case rem: RemoveItem => data -= rem.key

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
    case ins: Insert =>
      log.info("SK: Inserting "+ins.key+" this SK range is "+range.toString)
      //log.info("storekeeper range "+range)
      if(data.size < maxSize-1 ) {
        insertOrUpdate( ins.update, ins.key, ins.value)
      }
      else {
        log.info("SK: Must duplicate")
        // insert the item, then we will duplicate
        insertOrUpdate( ins.update, ins.key, ins.value)
        // half the collection
        var (halfLeft, halfRight) = data.splitAt( maxSize/2 )
        // create new keyrange to be updated for SF
        val halfLeftKR = new KeyRange( range.getMinRange, halfLeft.lastKey+"a" )
        // create new keyrange for the new storekeeper
        val halfRightKR = new KeyRange( halfLeft.lastKey+"aa", range.getMaxRange/*halfRight.lastKey*/ )
        // set the treemap to the first half
        data = halfLeft
        // send the request at manager with the treemap, old keyrangeId, new keyrange, collection of the new SK and
        // keyrange of the new sk
        context.parent ! com.actorbase.actorsystem.storefinder.messages.DuplicationRequestSK(range, halfLeftKR, halfRight, halfRightKR)
        // update keyRangeId or himself
        range = halfLeftKR
      }
      context.parent ! com.actorbase.actorsystem.main.messages.Ack
      //sender ! Response("inserted")
     // logAllItems

    /**
      * UpdateManager message, used to update the storekeeper manager, this is usefull when the Storefinder duplicate
      * himself, if the manager is not updated when this Storekeeper duplicates the manager ref is
      * wrong and bad things happens
      *
      * @param newManager ActorRef pointing the to new right actor manager (the maganer responsible of
      *                   the Storefinder mapping the range of this Storekeeper)
      */
    //case UpdateManager( newManager ) => manager = newManager

    // debug
    case DebugMaa(mainRange, sfRange) =>
      /*for( (key, value) <- data){
        log.info("DEBUG S-KEEPER (main "+mainRange+") ["+sfRange+"] "+key+" -> "+value+" size of this SK is "+data.size)
      }*/
      log.info("SK size is "+data.size)

  }

  /**
    * private method that insert an item to the collection, can allow the update of the item or not
    * changing the param update
    *
    * @param update boolean. 1 if the insert allow an update, 0 otherwise
    * @param key String representing the key of the item
    * @param value Any representing the value of the item
    */
  private def insertOrUpdate(update: Boolean, key: String, value: Any): Unit = {
    if(update)
      data += (key -> value)
    else if(!update && !data.contains(key))
      data += (key -> value)
    else if(!update && data.contains(key))
      log.info("SK: Duplicate key found, cannot insert")
  }

  /**
    * private method just for testing porpose, just log.info all the collection (key -> value)
    **/
  private def logAllItems(): Unit = {
    var itemslog: String = ""
    for( (key, value) <- data){
      itemslog += "key "+key+" -> "+value+" "
    }
    log.info(itemslog)
  }

}
