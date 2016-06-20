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
package com.actorbase.actorsystem

import akka.util.Timeout
import com.actorbase.actorsystem.utils.ActorbaseCollection
import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestActorRef, ImplicitSender, TestProbe}
import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpecLike
import org.scalatest.BeforeAndAfterAll

import com.actorbase.actorsystem.actors.main.Main
import com.actorbase.actorsystem.actors.clientactor.ClientActor
import com.actorbase.actorsystem.actors.storekeeper.Storekeeper
import com.actorbase.actorsystem.actors.storefinder.Storefinder
import com.actorbase.actorsystem.actors.warehouseman.Warehouseman
import com.actorbase.actorsystem.actors.manager.Manager
import com.actorbase.actorsystem.actors.authactor.AuthActor

import com.actorbase.actorsystem.utils.ActorbaseCollection._

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
    with ImplicitSender
    with BeforeAndAfterAll {

  implicit val timeout = Timeout(5 seconds)

  //implicit val system = ActorSystem()

  val p = TestProbe()

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

/*  "Main actor" should{

    val authProxy = TestActorRef[AuthActor]

    val mainActorRef = TestActorRef( new Main(authProxy) )

    val testColl = new ActorbaseCollection("testCollection", "anonymous")

    "should be created" in {
      assert(mainActorRef != None)
    }

    "create a new collection" in {
      val size = mainActorRef.underlyingActor.getSize
      p.send(mainActorRef, CreateCollection("admin", testColl))
      p.expectMsg("OK")
    }

    "insert and retrieve an item" in {
      val value = "testValue".getBytes
      p.send( mainActorRef, InsertTo("anonymous", testColl, "testKey",  value, false))
      p.send( mainActorRef, GetFrom("anonymous", testColl, "testKey"))
      p.expectMsg("OK")
    }

    "remove an item" in {
      val value = "testValue".getBytes
      p.send(mainActorRef, RemoveFrom("anonymous", testColl.getUUID, "testKey"))
      p.expectMsg("OK")
    }

    "add a contributor to a collection" in {
      p.send( authProxy, AddCredentials("pluto", "p4sswordPluto"))
      p.send( mainActorRef, AddContributor("anonymous", "pluto", ReadWrite, testColl.getUUID))
      p.expectMsg("OK")
    }

    "remove a contributor from a collection" in {   // this is not responding, can't except messages
      p.send( mainActorRef, RemoveContributor("anonymous", "pluto", testColl.getUUID ))
    }

    "receive the message CompleteTransaction" in {
      p.send( mainActorRef, CompleteTransaction( authProxy, testColl, Map[String, Array[Byte]]("key" -> "value".getBytes ) ) )
    }
  }


  "Storefinder Actor" should {

    val actbColl = new ActorbaseCollection("testCollection", "anonymous")

    val sfRef = TestActorRef(new Storefinder( actbColl ))

    "be created" in{
      assert(sfRef != None)
    }

    "insert and get an item" in {
      val value = "value".getBytes()
      p.send( sfRef, Insert("key", value , false) )
      p.send( sfRef, Get("key") )
      p.expectMsg("OK")
    }

    "get all items" in {  // response is null, can't expect anything
      p.send( sfRef, GetAllItems )
    }

    "remove an item" in {
      val value = "value".getBytes()
      p.send( sfRef, Remove("key"))
      p.expectMsg("OK")
    }

    "update the collection size" in { // response is null, can't expect anything
      p.send( sfRef, UpdateCollectionSize( true ) )
    }

    "receive the message PartialMapTransaction" in {  // response is null, can't expect anything
      p.send( sfRef, PartialMapTransaction( sfRef, Map[String, Array[Byte]]("key" -> "value".getBytes ) ) )
    }

  }


  "Storekeeper Actor" should{
    val collName = "testName"
    val collOwner = "testOwner"
    val skRef = TestActorRef(new Storekeeper( collName, collOwner, 10 ))
    val p = TestProbe()

    val valore = "value".getBytes()

    val actbColl = new ActorbaseCollection("testOwner","testName")
    val sfRef = TestActorRef(new Storefinder( actbColl ))

    "be created" in {
      assert(skRef != None)
    }

    "insert an item" in {
      p.send( skRef, InsertItem(sfRef, "key", valore , false) )
      p.expectMsg("OK")
    }

   /* "get an item" in { lancia eccezioni
      p.send( skRef, GetItem("key") )
    //  println("response is "+p.receiveOne(5 seconds)+"\n")
    }*/

    "remove an item" in {
      p.send( skRef, RemoveItem(sfRef, "key") )
      p.expectMsg("OK")
    }

    "return all items" in {
      p.send( skRef, InsertItem(sfRef, "key", valore , false) )
      p.send( skRef, GetAll( p.ref ) )
      p.expectMsg("OK")
    }

    "persist data sending message to the warehouseman" in {
      p.send( skRef, Persist )
      p.expectMsg( PartialMapTransaction( p.ref, Map[String, Array[Byte]]("key" -> valore)) )
    }

    "able to receive a message Initmn to initialize his manager" in {   // response is null, can't expect anything
      p.send( skRef, InitMn( sfRef))
      //println("response is "+p.receiveOne(5 seconds)+"\n")
    }

  }
*/

  /*"Warehouseman Actor" should {

    import java.io.File
    import akka.pattern.ask
    import scala.concurrent.Await

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
      val map = Map[String, Array[Byte]]("key0" -> "zero".getBytes(), "key1" -> "one".getBytes(), "key2" -> "two".getBytes())
      Await.result(wareRef.ask(com.actorbase.actorsystem.messages.WarehousemanMessages.Save(map))(5 seconds).mapTo[Int], Duration.Inf)
      val nfiles = new File("actorbasedata/testUuid/").list.size
      assert( nfiles == 1) //should be(true)
    }

    "read and decrypt data" in {
      val dir = new File("actorbasedata/testUuid/")
      val f = Option(dir.listFiles).map(_.toList).getOrElse(Nil)
      val map = Await.result(wareRef.ask(com.actorbase.actorsystem.messages.WarehousemanMessages.Read(f.head))(5 seconds).mapTo[Map[String, Any]], Duration.Inf)
      assert(map.size == 3) // should have size 3
    }

    "receive the message clean" in {
      p.send( wareRef, com.actorbase.actorsystem.messages.WarehousemanMessages.Clean )
    }

  }


  "AuthActor" should {

    val authRef = TestActorRef[AuthActor]

    val actbColl = new ActorbaseCollection("testOwner","testName")

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


  "Manager Actor" should{

    val collName = "testColl"
    val collOwner = "testOwner"
    /*val storekeepers = system.actorOf(ClusterRouterPool(ConsistentHashingPool(0),
     ClusterRouterPoolSettings(10000, 25, true, None)).props(Storekeeper.props( collName, collOwner)) )
     */
    val skRef = TestActorRef(new Storekeeper( collName, collOwner, 10 ))

    val mnRef = TestActorRef(new Manager( collName, collOwner, skRef ))

    "be created" in{
      assert(mnRef != None)
    }

    "create one storekeeper" in {
      import com.actorbase.actorsystem.actors.manager.Manager.OneMore
      p.send( mnRef, OneMore )
    }
  }


  "ClientActor" should {

    val authProxy = TestActorRef[AuthActor]
    val mainActorRef = TestActorRef( new Main(authProxy) )
    val clientActorRef = TestActorRef( new ClientActor(mainActorRef, authProxy))

    val ab = "value".getBytes

    "be created" in {
      assert(clientActorRef != None)
    }

    "receive the message Response" in {
      p.send( clientActorRef, Response( ab ) )
    }

    "receive the message MapResponse" in {
      p.send( clientActorRef, MapResponse("user", "user", Map[String, Any]("key" -> "value")) )
    }

    "receive the message ListResponse" in {
      p.send( clientActorRef, ListResponse(List[String]("item1", "item2", "item3") ) )
    }
  }*/

}
*/