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

package com.actorbase.actorsystem.authactor

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._
import scala.concurrent.Await
import akka.pattern.ask
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

import com.actorbase.actorsystem.utils.ActorbaseCollection
import com.actorbase.actorsystem.actors.authactor.AuthActor
import com.actorbase.actorsystem.messages.AuthActorMessages._

import com.actorbase.actorsystem.ActorSystemSpecs.ActorSystemUnitSpec

import akka.testkit.{TestKit, TestActorRef, ImplicitSender, TestProbe}

/**
   * Userkeeper specification tests
   *
   * @param
   * @return
   * @throws
   */
class AuthActorSpec extends TestKit(ActorSystem("AuthActorSpec",
  ConfigFactory.parseString("""
akka.remote.netty.tcp.port = 0,
akka.actor.provider = "akka.cluster.ClusterActorRefProvider",
akka.loglevel = "OFF"
                            """))) with ActorSystemUnitSpec with ImplicitSender {

  /**
   * afterAll method, triggered after all test have ended, it shutdown the
   * actorsystem.
   */
  override def afterAll() : Unit = system.shutdown

  "AuthActor" should {

    val authRef = TestActorRef[AuthActor]
    val actbColl = new ActorbaseCollection("testOwner","testName")
    val p = TestProbe()

    "be created" in {
      assert(authRef != None)
    }

    "receive the message AddCredential" in {
      p.send( authRef, AddCredentials("pippo", "Pluto7632"))
    }

    "receive the message Authenticate" in {
      p.send( authRef, Authenticate("pippo", "Pluto7632"))
    }

    "receive the message AddCollectionTo" in {
      p.send( authRef, AddCollectionTo("pippo", actbColl))
    }

    "receive the message RemoveColletionFrom" in {
      p.send( authRef, RemoveCollectionFrom("pippo", actbColl))
    }

    "receive the message UpdateCredential" in {
      p.send( authRef, UpdateCredentials("pippo", "Pluto7632", "Pluto7633"))
    }

    "receive the message RemoveCredential" in {
      p.send( authRef, RemoveCredentials("pippo"))
    }
  }

}
