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

package com.actorbase.actorsystem

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
import com.actorbase.actorsystem.actors.storekeeper.Storekeeper
import com.actorbase.actorsystem.actors.storefinder.Storefinder
import com.actorbase.actorsystem.actors.warehouseman.Warehouseman
import com.actorbase.actorsystem.actors.manager.Manager
import com.actorbase.actorsystem.actors.authactor.AuthActor

import com.actorbase.actorsystem.messages._
import com.actorbase.actorsystem.messages.MainMessages._
import com.actorbase.actorsystem.messages.AuthActorMessages._
import com.actorbase.actorsystem.messages.StorekeeperMessages._
import com.actorbase.actorsystem.messages.StorefinderMessages._
//import com.actorbase.actorsystem.messages.ManagerMessages._
import com.actorbase.actorsystem.messages.ClientActorMessages._
import com.actorbase.actorsystem.messages.WarehousemanMessages._

class ActorsSpecs extends TestKit(ActorSystem("testSystem"))
  with WordSpecLike
  with MustMatchers
  with ImplicitSender{

  implicit val timeout = Timeout(5 seconds)

  //implicit val system = ActorSystem()

  val p = TestProbe()

  "Main actor" should{

    val mainActorRef = TestActorRef[Main]

    /*"list all collections" in {
      p.send( mainActorRef, ListCollections("test") )
      p.expectMsg( ListResponse(List()) )
    }*/

    "insert and retrieve an item" in {
      val testColl = new ActorbaseCollection("testCollection", "anonymous")
      val value = "testValue".getBytes
      p.send( mainActorRef, InsertTo("admin", testColl, "testKey",  value, false))
      p.send( mainActorRef, GetFrom("admin", testColl, "testKey"))
      p.expectMsg(Response(value))
    }
  
    "create a new collection" in {
      val testCreate = new ActorbaseCollection("testColl", "anonymous")
      val size = mainActorRef.underlyingActor.getSize
      p.send(mainActorRef, CreateCollection(testCreate))
      assert(mainActorRef.underlyingActor.getSize === size+1)
    }
  
    "remove an item" in {
      val testColl = new ActorbaseCollection("testCollection", "anonymous")
      val value = "testValue".getBytes
      p.send( mainActorRef, InsertTo("admin", testColl, "testKey",  value, false))
      p.send( mainActorRef, GetFrom("admin", testColl, "testKey"))
      p.send(mainActorRef, RemoveFrom(testColl.getUUID, "testKey"))
      p.send( mainActorRef, GetFrom("admin", testColl, "testKey"))
      val testMessage = p.receiveOne(25 seconds)
      assert( value != testMessage.asInstanceOf[Response].response)
    }
  
  /*it should{
    "add a contributor" in {
    p.send( mainActorRef, addContributor(new ActorbaseCollection("testCollection", "anonymous"), "testContributor"))
    p.send( mainActorRef, InsertTo("admin", new ActorbaseCollection("testCollection", "anonymous"), "test"))
    //get e controlla e fine test
  }
  } TODO quando sarà fatta*/
  }


  "Storefinder Actor" should {
    
    val actbColl = new ActorbaseCollection("testOwner","testName")
    val sfRef = TestActorRef(new Storefinder( actbColl ))
    val p = TestProbe()

    "be created" in{
      assert(sfRef != None)
    }
  
    "insert and get an item" in {
      val value = "value".getBytes()
      p.send( sfRef, Insert("key", value , false) )
      p.send( sfRef, Get("key") )
      p.expectMsg( Response( value ) )
    }

  /*  TODO SBAGLIATO
    "get all items" in {
      val value = "value".getBytes()
      p.send( sfRef, Insert("key", value , false) )
      p.send( sfRef, GetAllItems )
      val m: Map[String, Array[Byte]] = Map("key" -> value )
      p.expectMsg( 5 seconds, MapResponse( "testName",  m ) )
    }
  */

  // None.get non è uguale a None.get ritornato dallo SK
    "remove an item" in {
      val value = "value".getBytes()
      p.send( sfRef, Insert("key", value , false) )
      p.send( sfRef, Remove("key"))
      p.send( sfRef, Get("key") )
      val testMessage = p.receiveOne(3 seconds)
      assert( value != testMessage.asInstanceOf[Response].response )
      //p.expectMsg( 5 seconds, Response( none ) )
    }

  // non è giusto, non controlla niente
    "update the collection size" in {
      //val size = sfRef.underlyingActor.collection.size
      p.send( sfRef, UpdateCollectionSize( true ) )
      //assert ( size = sfRef.underlyingActor.collection.size -1 )
    }
  
  }

 /* "Manager Actor" should{
/*
    val collName = "testColl"
    val collOwner = "testOwner"
    /*val storekeepers = system.actorOf(ClusterRouterPool(ConsistentHashingPool(0),
      ClusterRouterPoolSettings(10000, 25, true, None)).props(Storekeeper.props( collName, collOwner)) )
  */
   // val mnRef = TestActorRef(new Manager( collName, collOwner, storekeepers ))
    val p = TestProbe()
*/
   -/* "be created" in{
      assert(mnRef != None)
    }*/
  /*
    "create one storekeeper" in {
      p.send( mnRef, OneMore )
      p.expectMsg( InitMn( mnRef ) )
    }*/    
  }
  */




}