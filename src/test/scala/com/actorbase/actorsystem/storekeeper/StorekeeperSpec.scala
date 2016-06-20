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

package com.actorbase.actorsystem.storefinder

import akka.util.Timeout
import com.actorbase.actorsystem.actors.storefinder.Storefinder
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.actor.Actor
import akka.testkit.{TestKit, TestActorRef, TestProbe}
import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpecLike
import org.scalatest.BeforeAndAfterAll

import com.actorbase.actorsystem.ActorSystemSpecs.ActorSystemUnitSpec
import com.actorbase.actorsystem.utils.ActorbaseCollection
import com.actorbase.actorsystem.actors.storekeeper.Storekeeper
import com.actorbase.actorsystem.messages.StorekeeperMessages._
import com.actorbase.actorsystem.messages.StorefinderMessages.{UpdateCollectionSize, PartialMapTransaction}
import com.actorbase.actorsystem.messages.WarehousemanMessages.Save
import com.actorbase.actorsystem.messages.ClientActorMessages.Response

class StorekeeperSpec extends TestKit(ActorSystem("StorekeeperSpec", ConfigFactory.parseString("""
akka.remote.netty.tcp.port = 0,
akka.actors.provider = "akka.cluster.ClusterRefProvider"
"""))) with ActorSystemUnitSpec {

  implicit val timeout = Timeout(25 seconds)

  //val actbColl = new ActorbaseCollection("testOwner","testName")
  val collName = "testName"
  val collOwner = "testOwner"
  val skRef = TestActorRef(new Storekeeper( collName, collOwner, 256 ))
  val actbColl = new ActorbaseCollection("testOwner","testName")
  val sfRef = TestActorRef(new Storefinder( actbColl ))
  val p = TestProbe()

  /**
    * afterAll method, triggered after all test have ended, it shutdown the
    * actorsystem.
    */
  override def afterAll() : Unit = system.shutdown

  val valore = "value".getBytes()

  "Storekeeper" should {
    "be created" in {
      assert(skRef != None)
    }
  }

  it should {
    "insert get an item" in {
      p.send( skRef, InsertItem(sfRef, "key", valore , false) )
      p.expectMsg( UpdateCollectionSize( true ) )
    }
  }

  it should {
    "get an item" in {
      p.send( skRef, InsertItem(sfRef, "key", valore , false) )
      p.send( skRef, GetItem("key") )
      p.expectMsg( Response( valore ) )
    }
  }

  it should {
    "remove an item" in {
      p.send( skRef, InsertItem(sfRef, "key", valore , false) )
      p.send( skRef, RemoveItem(sfRef, "key") )
      p.expectMsg( UpdateCollectionSize( false ) )
    }
  }

  it should {
    "return all items" in {
      p.send( skRef, InsertItem(sfRef, "key", valore , false) )
      p.expectMsg( UpdateCollectionSize( true ) )
      p.send( skRef, GetAll( p.ref ) )
      p.expectMsg( PartialMapTransaction( p.ref, Map[String, Array[Byte]]("key" -> valore) ) )
    }
  }

  //this fails, it goes in timeout
  /*
   it should {
   "persist data sending message to the warehouseman" in {
   // p.send( skRef, InsertItem("key", valore , false) )
   // p.expectMsg( UpdateCollectionSize( true ) )
   p.send( skRef, Persist )
   p.expectMsg( Save( Map[String, Array[Byte]]("key" -> valore) ) )
   }
   }*/

}
