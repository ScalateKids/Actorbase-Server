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
  * Class that models a keyrange for actorbase. This class has by 2 strings representing the lowest
  * string and the highest strings that can fit inside this range. KeyRanges can be compared
  *
  * @param minR a String representing the minimum String inside the range
  * @param maxR a String representing the maximum String inside the range
  */
class KeyRange(private val minRange: String, private val maxRange: String) extends Ordered[KeyRange] {
  // TODO DECIDERE LA CHIAVE MAXXXX ( supermaxkey should be a costant outside class probably)

  def getMinRange: String = minRange

  def getMaxRange: String = maxRange

  def contains(key: String): Boolean = key.toLowerCase >= minRange.toLowerCase && key.toLowerCase <= maxRange.toLowerCase

  override def toString: String = "from "+ minRange + " to " + maxRange

  override def compare(that: KeyRange): Int = (this.minRange, this.maxRange) compare (that.minRange, that.maxRange)
}
