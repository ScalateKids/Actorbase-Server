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

package com.actorbase.actorsystem.utils

import com.actorbase.actorsystem.utils.{Collection, KeyRange}

/**
  * descript
  *
  * @param
  * @param
  */
class CollectionRange(private var collection: Collection, private var range: KeyRange){

  def getCollectionName: String = collection.getName

  def getCollectionOwner: String = collection.getOwner

  def getMinRange: String = range.getMinRange

  def getMaxRange: String = range.getMaxRange

  def getKeyRange: KeyRange = range

  def contains(key: String): Boolean = range.contains(key)

  override def toString: String = "collection "+collection.getName+" with range from "+ getMinRange + " to " + getMaxRange

  // TODO DA TESTARE
  def isSameCollection(that: collectionRange): Boolean = {
    (this.getCollectionName + this.getCollectionOwner == that.getCollectionName + that.getCollectionOwner)
  }

  // TODO DA TESTARE
  def isSameCollection(name: String, owner: String): Boolean ={
    (this.getCollectionName == name && this.getCollectionOwner == owner)
  }

  // TODO DA TESTARE
  def compareKeyRanges(that: collectionRange): Int = {
    this.getKeyRange.compare(that.getKeyRange)
  }

  // TODO DA TESTARE
  /*override def compare(that: CollectionRange): Int = {
    return isSameCollection(that) && this.range compare(that.getKeyRange)
  }*/

}