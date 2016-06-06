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

package com.actorbase.actorsystem.main

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.TestActorRef
import akka.util.Timeout
import com.actorbase.actorsystem.utils.ActorbaseCollection
import scala.concurrent.duration._
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures

import com.actorbase.actorsystem.storekeeper.Storekeeper
import com.actorbase.actorsystem.messages.StorekeeperMessages._

class StorekeeperSpec extends FlatSpec with Matchers {

  implicit val timeout = Timeout(5 seconds)

  implicit val system = ActorSystem()

  it should "add a key-value pair to the underlying map" in {
    val skActorRef = TestActorRef(new Storekeeper("testcollection", "testowner"))
    skActorRef ! InsertItem("key", "value".getBytes, false)
    val future = skActorRef ? GetItem("key")

    ScalaFutures.whenReady(future) {
      response => response.asInstanceOf[String] should be("""""")
    }
  }
}
