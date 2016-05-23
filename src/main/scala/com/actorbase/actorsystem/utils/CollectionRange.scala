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

import scala.math.Ordered.orderingToOrdered

/**
  * Class representing a piece of an actorbase collection. Used by MainActors.
  * More precisely it represents the range of Keys of one collection mapped on a StoreFinder
  *
  * @param collection. A Collection type representing the actorbase collection
  * @param range. A KeyRange representing the upper and lower bounds of keys possible in this CollectionRange
  */
class CollectionRange(private var collection: ActorbaseCollection, private var range: KeyRange) extends Ordered[CollectionRange] {

  /**
    * @return a String representing the collection name
    */
  def getCollectionName: String = collection.getName

  /**
    * @return a String representing the username of the owner of the collection
    */
  def getCollectionOwner: String = collection.getOwner

  /**
    * @return a String representing the UUIID of the requested collection
    */
  def getCollectionUUID: String = collection.getUUID

  /**
    * @return a String representing the minimum Key mappable in this collectionRange
    */
  def getMinRange: String = range.getMinRange

  /**
    * @return a String representing the maximum Key mappable in this collectionRange
    */
  def getMaxRange: String = range.getMaxRange

  /**
    * @return a KeyRange representing the minimum and maximum Key mappable in this collectionRange
    */
  def getKeyRange: KeyRange = range

  /**
    * @return a Collection representing the collection represented by this collectionRange
    */
  def getCollection: ActorbaseCollection = collection


  /**
    * @params key a String representing a key, the method will return true if the key is inside the KeyRange of this
    *        CollectionRange, false otherwise
    * @return a Boolean. True if the key is inside the KeyRange of this CollectionRange, false otherwise
    */
  def contains(key: String): Boolean = range.contains(key)

  /**
    * @return a String representig the name and the KeyRange of this CollectionRange
    */
  override def toString: String = "collection "+collection.getName+" with range from "+ getMinRange + " to " + getMaxRange

  /**
    * Check if the collection represented by the CollectionRange passed as input is the same collection
    * of this CollectionRange
    *
    * @param that a CollectionRange
    * @return Boolean. True if the collection represented by the CollectionRange passed as param is the same
    *         as the collection represented by this object
    */
  def isSameCollection(that: CollectionRange): Boolean = {
    (this.getCollectionName + this.getCollectionOwner == that.getCollectionName + that.getCollectionOwner)
  }

  /**
    * Check if the collection represented by the Collection passed as input is the same collection
    * of this CollectionRange
    *
    * @param coll a Collection
    * @return Boolean. True if the collection represented by the Collection passed as param is the same
    *         as the collection represented by this object
    */
  def isSameCollection(coll: ActorbaseCollection): Boolean = {
    (this.getCollectionName + this.getCollectionOwner == coll.getName + coll.getOwner)
  }

  /**
    * Check if the collection represented by the collection name and owner passed as input is the same collection
    * of this CollectionRange
    *
    * @param that a String, the collection name
    * @param owner a String, the owner of a collection
    * @return Boolean. True if the collection represented by the input parameters is the same as the collection
    *         represented by this object
    */
  def isSameCollection(name: String, owner: String): Boolean = {
    (this.getCollectionName == name && this.getCollectionOwner == owner)
  }

  /**
    * compare KeyRange of this CollectionRange with the passed param
    *
    * @param that a CollectionRange to compare with the KeyRange of this object
    * @return Int. -1 if this KeyRange is < of the param one. 0 if they are the same. +1 if this KeyRange is > of
    *         the param one
    */
  def compareKeyRanges(that: CollectionRange): Int = {
    this.getKeyRange.compare(that.getKeyRange)
  }

  /**
    * compare this CollectionRange with the passed param
    *
    * @param that a CollectionRange to compare with this object
    * @return Int.
    *         -1 if the this.collectionName is < of that.collectionName OR if the collections are the same
    *         but this.KeyRange < that.KeyRange
    *         0 if the collections are the same and also the KeyRanges are the same
    *         +1 if the this.collectionName is > of that.collectionName OR if the collections are the same
    *         but this.KeyRange > that.KeyRange
    */
  override def compare(that: CollectionRange): Int = {
    // first check if the collection is the same, if not return the collection compare
    val comparedCollect = this.getCollection.compare(that.getCollection)
    if( comparedCollect != 0)
      // they are not the same collection, just return the compare between the two collections
      comparedCollect
    else
      // they are the same collection, need to return the comparison between keyranges
      this.getKeyRange.compare(that.getKeyRange)
  }

}
