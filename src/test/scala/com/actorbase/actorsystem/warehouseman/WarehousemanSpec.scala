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

import java.io.File
import scala.util.Success
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import akka.pattern.ask
import akka.util.Timeout
import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestActorRef, ImplicitSender, TestProbe}

import com.typesafe.config.ConfigFactory
import com.actorbase.actorsystem.ActorSystemSpecs.ActorSystemUnitSpec
import com.actorbase.actorsystem.actors.warehouseman.Warehouseman
import com.actorbase.actorsystem.messages.WarehousemanMessages._

class WarehousemanSpec extends TestKit(ActorSystem("WarehousemanSpec",
  ConfigFactory.parseString("""
akka.remote.netty.tcp.port = 0,
akka.actor.provider = "akka.cluster.ClusterActorRefProvider",
akka.loglevel = "OFF"
                            """))) with ActorSystemUnitSpec with ImplicitSender {

  implicit val timeout = Timeout(25 seconds)

  /**
    * afterAll method, triggered after all test have ended, it shutdown the
    * actorsystem.
    */
  override def afterAll() : Unit = system.shutdown

  "Warehouseman Actor" should {

    val collUuid = "testUuid"
    val wareRef = TestActorRef(new Warehouseman( collUuid ))
    val p = TestProbe()

    "be created" in {
      assert(wareRef != None)
    }

    "save encrypted data" in {
      def delete(file: File) {
        if (file.isDirectory)
          Option(file.listFiles).map(_.toList).getOrElse(Nil).foreach(delete(_))
        file.delete
      }
      delete( new File("actorbasedata/testUuid/") )
      val map = Map[String, Array[Byte]]("key0" -> "zero".getBytes("UTF-8"), "key1" -> "one".getBytes("UTF-8"), "key2" -> "two".getBytes("UTF-8"))
      val future = (wareRef ? com.actorbase.actorsystem.messages.WarehousemanMessages.Save(map)).mapTo[Int]
      future onSuccess {
        case s =>
          val nfiles = new File("actorbasedata/testUuid/").list.size
          assert( nfiles == 1) //should be(true)
      }
    }

    "read and decrypt data" in {
      val dir = new File("actorbasedata/testUuid/")
      val f = Option(dir.listFiles).map(_.toList).getOrElse(Nil)
      val map = (wareRef ? com.actorbase.actorsystem.messages.WarehousemanMessages.Read(f.head)).mapTo[Map[String, Any]]
      map onSuccess {
        case m =>
          assert(m.size == 3) // should have size 3
      }
    }

    "receive the message clean" in {
      p.send( wareRef, com.actorbase.actorsystem.messages.WarehousemanMessages.Clean )
    }
  }


}
