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

// TEMPORARY BRIDGE BETWEEN MAIN AND STOREKEEPER

package com.actorbase.actorsystem.storefinder

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props

import com.actorbase.actorsystem.storefinder.messages._
import com.actorbase.actorsystem.storekeeper.Storekeeper

object Storefinder {
  def props() : Props = Props(new Storefinder())
}

class Storefinder extends Actor with ActorLogging{

  def receive = {
    case CreateSk => {
      val sk = context.actorOf(Storekeeper.props())
      sk ! com.actorbase.actorsystem.storekeeper.messages.Init()
      sk ! com.actorbase.actorsystem.storekeeper.messages.Insert("lol", "lel")
      sk ! com.actorbase.actorsystem.storekeeper.messages.GetAllItem()
      sk ! com.actorbase.actorsystem.storekeeper.messages.GetItem("lol")
      sk ! com.actorbase.actorsystem.storekeeper.messages.RemoveItem("lel")
    }
  }

}
