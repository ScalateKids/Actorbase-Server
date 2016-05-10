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

import com.actorbase.actorsystem.main.messages.DuplicateSFNotify

import scala.collection.immutable.TreeMap

object Manager {

  def props( sfRef: ActorRef ): Props = Props(new Manager( sfRef ))

}

/**
  * Insert description here
  *
  * @param
  * @return
  * @throws
  */


class Manager(private val parentRef: ActorRef) extends Actor with ActorLogging {

  def receive = {

    /**
      * DuplicationRequestSK message. When the actor receive this message it means that a Storekeeper is full.
      * This actor has to create a new Storekeeper using the map in input and then notify the Storefinder that the
      * duplication happened sending the new ActorRef and new KeyRanges.
      *
      * @param oldKeyRange a KeyRange representing the KeyRange that was duplicated. this is needed by the
      *                    Storefinder to understand which KeyRange needs to be updated
      * @param leftRange a KeyRange representing the new KeyRange of the Storekeeper that was duplicated
      * @param map a TreeMap[String, Any] representing the collection for the Storekeeper that has to be created
      * @param rightRange a KeyRange representing the KeyRange of the Storekeeper that has to be created
      *
      */
    case DuplicationRequestSK(oldKeyRange, leftRange, map, rightRange) => {
      log.info("Manager "+self.path.name+": Duplication request SK")
      // create a SK with the self refence, the map received and the KeyRange he represents
      val newSk = context.actorOf(Props(new Storekeeper( self, map, rightRange)).withDispatcher("control-aware-dispatcher"))

      // notify the SF that a duplication has happened, he needs to update his data structure
      parentRef ! com.actorbase.actorsystem.storefinder.messages.DuplicateSKNotify( oldKeyRange, leftRange, newSk, rightRange)
    }

    /**
      *
       */
    case DuplicationRequestSF(oldCollRange, leftCollRange, map, rightCollRange, mainActor) => {
      log.info("Manager: Duplication request SF ")
      // create che storefinder with ( mainactor ref, actorbaseCollection, Map[KeyRange, skRef], KeyRange )
      val newSf = context.actorOf(Props(new Storefinder(mainActor, oldCollRange.getCollection, map, rightCollRange.getKeyRange)).withDispatcher("control-aware-dispatcher"))

      // notify the mainActor that a duplication has happened, he needs to update his data structure
      mainActor ! DuplicateSFNotify( oldCollRange, leftCollRange, newSf, rightCollRange )
    }
  }
}
