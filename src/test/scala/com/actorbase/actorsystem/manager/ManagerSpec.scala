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
/*
package com.actorbase.actorsystem.manager

import akka.util.Timeout
import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.actor.Actor
import akka.testkit.{TestKit, TestActorRef, TestProbe}
import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpecLike
import org.scalatest.BeforeAndAfterAll

import com.actorbase.actorsystem.utils.ActorbaseCollection
import com.actorbase.actorsystem.actors.manager.Manager
import com.actorbase.actorsystem.actors.manager.Manager.OneMore
import com.actorbase.actorsystem.messages.StorekeeperMessages.InitMn
import com.actorbase.actorsystem.actors.storekeeper.Storekeeper
import com.actorbase.actorsystem.actors.main.Main

import akka.cluster.routing.ClusterRouterPool
import akka.cluster.routing.ClusterRouterPoolSettings
import akka.routing.{ ActorRefRoutee, ConsistentHashingPool, FromConfig, Router }
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import akka.routing.Broadcast

class ManagerSpec extends TestKit(ActorSystem("testSystem"))
  with WordSpecLike
  with MustMatchers
  with BeforeAndAfterAll{

  val collName = "testColl"
  val collOwner = "testOwner"
  /*val storekeepers = system.actorOf(ClusterRouterPool(ConsistentHashingPool(0),
    ClusterRouterPoolSettings(10000, 25, true, None)).props(Storekeeper.props( collName, collOwner)) )
*/
  val mnRef = TestActorRef(new Manager( collName, collOwner, storekeepers ))
  val p = TestProbe()

  "Manager" should {
    "be created" in{
      assert(mnRef != None)
    }
  }

  // fails, it goes in timeout...
  it should {
    "create one storekeeper" in {
      p.send( mnRef, OneMore )
      p.expectMsg( InitMn( mnRef ) )
    }
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
}*/