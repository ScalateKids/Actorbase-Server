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

package com.actorbase.actorsystem.warehouseman

import com.actorbase.actorsystem.warehouseman.messages._
import com.actorbase.actorsystem.ActorSystemSpecs.ActorSystemUnitSpec

import akka.actor.ActorSystem
import akka.pattern.ask
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.collection.immutable.TreeMap

import java.io.File

class WarehousemanSpec extends ActorSystemUnitSpec {

  val system = ActorSystem("WarehousemanSpec")

  /**
    * afterAll method, triggered after all test have ended, it shutdown the
    * actorsystem.
    */
  override def afterAll() : Unit = system.shutdown

  /**
    * User credentials tests
    */
  "A warehouseman" should {
    "save encrypted data" in {

      var map: TreeMap[String, Any] = new TreeMap[String, Any]()
      map += ("key0" -> "zero")
      map += ("key1" -> "one")
      map += ("key2" -> "two")
      val warehouseman = system.actorOf(Warehouseman.props("shard"))
      Await.result(warehouseman.ask(Save(map))(5 seconds).mapTo[Int], Duration.Inf)
      new File("shard").exists should be(true)

    }
  }
}
