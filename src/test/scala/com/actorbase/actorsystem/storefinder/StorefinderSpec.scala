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
package com.actorbase.actorsystem.storefinder

import akka.util.Timeout
import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.actor.Actor
import akka.testkit.{TestKit, TestActorRef, TestProbe}
import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpecLike

import com.actorbase.actorsystem.utils.ActorbaseCollection
import com.actorbase.actorsystem.actors.storefinder.Storefinder
import com.actorbase.actorsystem.messages.StorefinderMessages._
import com.actorbase.actorsystem.messages.ClientActorMessages._
import com.actorbase.actorsystem.messages.MainMessages.CompleteTransaction

class StorefinderSpec extends TestKit(ActorSystem("testSystem"))
  with WordSpecLike
  with MustMatchers {

  implicit val timeout = Timeout(25 seconds)

  val actbColl = new ActorbaseCollection("testOwner","testName")
  val sfRef = TestActorRef(new Storefinder( actbColl ))
  val p = TestProbe()

  "Storefinder" should {
    "be created" in{
      assert(sfRef != None)
    }
  }

  it should {
    "insert and get an item" in {
      val value = "value".getBytes()
      p.send( sfRef, Insert("key", value , false) )
      p.send( sfRef, Get("key") )
      p.expectMsg( Response( value ) )
    }
  }

  /*  TODO SBAGLIATO
  it should {
    "get all items" in {
      val value = "value".getBytes()
      p.send( sfRef, Insert("key", value , false) )
      p.send( sfRef, GetAllItems )
      val m: Map[String, Array[Byte]] = Map("key" -> value )
      p.expectMsg( 5 seconds, MapResponse( "testName",  m ) )
    }
  }
  */

  // None.get non è uguale a None.get ritornato dallo SK
  it should {
    "remove an item" in {
      val value = "value".getBytes()
      val none = "None".getBytes
      p.send( sfRef, Insert("key", value , false) )
      p.send( sfRef, Remove("key"))
      p.send( sfRef, Get("key") )
      p.expectMsg( 5 seconds, Response( none ) )
    }
  }

  // non è giusto, non controlla niente
  it should {
    "update the collection size" in {
      //val size = sfRef.underlyingActor.collection.size
      p.send( sfRef, UpdateCollectionSize( true ) )
      //assert ( size = sfRef.underlyingActor.collection.size -1 )
    }
  }

}*/