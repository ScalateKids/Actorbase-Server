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

package com.actorbase.actorsystem.manager

import akka.actor.{Props, Actor, ActorLogging, ActorRef}

import com.actorbase.actorsystem.storefinder.Storefinder
import com.actorbase.actorsystem.storekeeper.Storekeeper
import com.actorbase.actorsystem.manager.messages._

import scala.collection.immutable.TreeMap

object Manager {
  def props() : Props = Props(new Manager())
}

/**
  * Insert description here
  *
  * @param
  * @return
  * @throws
  */


class Manager extends Actor with ActorLogging {

  def receive = {
    case DuplicationRequestSK(oldKeyRange, leftRange, map, rightRange) => {
      log.info("Manager: Duplication request SK")
      // create a SK with the map received and init him
      val newSk = context.actorOf(Props(new Storekeeper( self, map, rightRange)))

      // initialize the new Sk sending self
      //newSk ! com.actorbase.actorsystem.storekeeper.messages.Init( self, rightRange )

      // should notify storefinder with new actorref and update of the keyrange
      //parent ! modifica sf keyrangeleft e right
      context.parent ! com.actorbase.actorsystem.storefinder.messages.DuplicateSKNotify( oldKeyRange, leftRange, newSk, rightRange)
    }
    case DuplicationRequestSF(oldKeyRange, leftRange, map, rightRange) => {
      log.info("Manager: Duplication request SF")
      // create a SF with the map received and init him
      val newSf = context.actorOf(Props(new Storefinder(map)))

      // initialize the new Sk sending self
      newSf ! com.actorbase.actorsystem.storekeeper.messages.Init( self, rightRange )

      // should notify storefinder with new actorref and update of the keyrange
      //parent ! modifica sf keyrangeleft e right
      context.parent ! com.actorbase.actorsystem.storefinder.messages.DuplicateSKNotify( oldKeyRange, leftRange, newSf, rightRange)
    }
  }
}
