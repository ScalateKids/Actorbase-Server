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

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.routing.{ ActorRefRoutee, AddRoutee }

import com.actorbase.actorsystem.storekeeper.Storekeeper
import com.actorbase.actorsystem.messages.StorekeeperMessages.InitMn

object Manager {

  case object OneMore

  def props(collection: String, owner: String, router: ActorRef): Props = Props(classOf[Manager], collection, owner, router)

}

/**
  * Try to maintain the equilibrium inside the SKs by tracing the number of
  * entries stored in each map. If some SK actor is under heavy load, (number of
  * entries is beyond a given threshold) they will create new actor of type SK
  * to properly redistribute that load and add it to the SF router.
  */


class Manager(val collection: String, val owner: String, val router: ActorRef) extends Actor with ActorLogging {

  import Manager._

  def receive = {
    case OneMore =>
      log.info("new storekeeper added to [POOL]")
      val newSk = context.actorOf(Storekeeper.props(collection, owner))
      newSk ! InitMn(self)
      router ! AddRoutee(ActorRefRoutee(newSk))
  }
}
