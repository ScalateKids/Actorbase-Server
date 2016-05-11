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

package com.actorbase.actorsystem.warehouseman

import akka.actor.{Actor, ActorLogging, Props}

import java.io._

import com.actorbase.actorsystem.warehouseman.messages._
import com.actorbase.actorsystem.utils.CryptoUtils

object Warehouseman {

  def props(s: String) : Props = Props(new Warehouseman(s))

}

class Warehouseman(collectionShard: String = "shard") extends Actor with ActorLogging {

  private val rootFolder = "actorbasedata/"

  def receive = {

    case Init => log.info("warehouseman: init")

    /**
      * Save a shard of a collection represented by the TreeMap stored by a
      * Storekeeper
      *
      * @param map a TreeMap representing Storekeeper data
      */
    case Save(sfRange, range, map) =>
      log.info("warehouseman: save")
      val key = "Dummy implicit k"
      val encryptedShardFile = new File(rootFolder+collectionShard+"-"+sfRange.getMinRange+"-"+sfRange.getMaxRange+"/"+range.getMinRange+"-"+range.getMaxRange+".actb")
      encryptedShardFile.getParentFile.mkdirs
      CryptoUtils.encrypt(key, map, encryptedShardFile)
      sender ! 0 // ok reply

    /**
      * Delete a file with the Range with the keys passed in
      *
      * @param range a KeyRange representing the range of the file to delete
      */
    case Clean(sfRange, range) =>
      val filePath = new File(rootFolder+collectionShard+"-"+sfRange.getMinRange+"-"+sfRange.getMaxRange+"/"+range.getMinRange+"-"+range.getMaxRange+".actb").delete()

    /**
      * Read a file from filesystem and decrypt the content
      * extracting the map shard contained
      *
      * @param f an encrypted file containing the shard of the collection
      */
    case Read(f) =>
      log.info("warehouseman: read")
      val key = "Dummy implicit k"
      val m = CryptoUtils.decrypt(key, f)
      sender ! m // ok reply
  }
}
