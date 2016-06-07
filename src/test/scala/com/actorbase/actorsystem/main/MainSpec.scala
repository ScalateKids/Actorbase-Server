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

package com.actorbase.actorsystem.main

import akka.util.Timeout
import com.actorbase.actorsystem.utils.ActorbaseCollection
import scala.concurrent.duration._
import org.scalatest.FlatSpec

import akka.actor.ActorSystem
import akka.actor.Actor
import akka.testkit.{TestKit, TestActorRef, ImplicitSender, TestProbe}
import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpecLike
import org.scalatest.BeforeAndAfterAll

import com.actorbase.actorsystem.actors.main.Main
import com.actorbase.actorsystem.messages.MainMessages._
import com.actorbase.actorsystem.messages.StorefinderMessages._
import com.actorbase.actorsystem.messages.ClientActorMessages._

class MainSpec extends TestKit(ActorSystem("testSystem"))
  with WordSpecLike
  with MustMatchers
  with ImplicitSender  {

  implicit val timeout = Timeout(25 seconds)

  //implicit val system = ActorSystem()

  val mainActorRef = TestActorRef[Main]

  val p = TestProbe()

  "main" should{
    "list all collections" in {
      p.send( mainActorRef, ListCollections("test") )
      p.expectMsg( ListResponse(List()) )
    }
  }

  it should{
    "insert and retrieve an item" in {
      val testColl = new ActorbaseCollection("testCollection", "anonymous")
      val value = "testValue".getBytes
      p.send( mainActorRef, InsertTo(testColl, "testKey",  value, false))
      p.send( mainActorRef, GetFrom(testColl, "testKey"))
      p.expectMsg(Response(value))
    }
  }
  
  it should{
    "create a new collection" in {
	  val testCreate = new ActorbaseCollection("testColl", "anonymous")
	  val size = mainActorRef.underlyingActor.getSize
	  p.send(mainActorRef, CreateCollection(testCreate))
	  assert(mainActorRef.underlyingActor.getSize === size+1)
	}
  }
  
  it should{
    "remove an item" in {
	  val testColl = new ActorbaseCollection("testCollection", "anonymous")
      val value = "testValue".getBytes
      p.send( mainActorRef, InsertTo(testColl, "testKey",  value, false))
      p.send( mainActorRef, GetFrom(testColl, "testKey"))
	  p.send(mainActorRef, RemoveFrom(testColl.getUUID, "testKey"))
      p.send( mainActorRef, GetFrom(testColl, "testKey"))
      val testMessage = p.receiveOne(25 seconds)
	  assert( value != testMessage.asInstanceOf[Response].response)
	}
  }
  
  /*it should{
    "add a contributor" in {
	  p.send( mainActorRef, addContributor(new ActorbaseCollection("testCollection", "anonymous"), "testContributor"))
	  p.send( mainActorRef, InsertTo(new ActorbaseCollection("testCollection", "anonymous"), "test"))
	  //get e controlla e fine test
	}
  } TODO quando sarà fatta*/
}
