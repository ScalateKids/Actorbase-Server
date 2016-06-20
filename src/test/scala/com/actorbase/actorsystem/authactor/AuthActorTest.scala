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
/*
package com.actorbase.actorsystem.authactor

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._
import scala.concurrent.Await
import akka.pattern.ask
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

import com.actorbase.actorsystem.ActorSystemSpecs.ActorSystemUnitSpec
*/
// /**
//   * Userkeeper specification tests
//   *
//   * @param
//   * @return
//   * @throws
//   */
// class UserkeeperSpec extends ActorSystemUnitSpec {

//   val system = ActorSystem("UserkeeperSpec", ConfigFactory.parseString("""
// akka.remote.netty.tcp.port = 0,
// akka.actors.provider = "akka.cluster.ClusterRefProvider"
// """))

//   /**
//     * afterAll method, triggered after all test have ended, it shutdown the
//     * actorsystem.
//     */
//   override def afterAll() : Unit = system.shutdown

//   /**
//     * User credentials tests
//     */
//   "Concerning password, an Userkeeper" should {

//     val actorRef = system.actorOf(Userkeeper.props("user", "pass"))

//     "set the password: pass" in {

//       val password = Await.result(actorRef.ask(GetPassword)(5 seconds).mapTo[Option[String]].map{ pass => pass }, Duration.Inf)
//       password.getOrElse("No-Password-Received") should be("pass")
//     }

//     "set the password: newPass using ChangePassword" in {

//       actorRef ! ChangePassword("newPass")
//       val password = Await.result(actorRef.ask(GetPassword)(5 seconds).mapTo[Option[String]].map{ results => results }, Duration.Inf)
//       password.getOrElse("No-Password-Received") should be("newPass")
//     }

//   }

//   /**
//     * Read-write permission collections tests
//     */
//   "Adding a read-write collection to an Userkeeper" should {

//     val actorRef = system.actorOf(Userkeeper.props("user", "pass"))

//     "increment the size of the read-write buffer" in {

//       actorRef ! AddCollection(true, "collection_1")
//       val collections = Await.result(actorRef.ask(GetCollections(true))(5 seconds).mapTo[ListBuffer[String]].map{ results => results }, Duration.Inf)
//       collections.size should be(1)
//     }

//     "add the collection to the read-write buffer" in {

//       actorRef ! AddCollection(true, "collection_2")
//       val collections = Await.result(actorRef.ask(GetCollections(true))(5 seconds).mapTo[ListBuffer[String]].map{ results => results }, Duration.Inf)
//       collections.contains("collection_2") should be(true)
//     }

//   }

//   /**
//     * Read-only permission collections tests
//     */
//   "Adding a read-only collection to an Userkeeper" should {

//     val actorRef = system.actorOf(Userkeeper.props("user", "pass"))

//     "increment the size of the read-only buffer" in {

//       actorRef ! AddCollection(false, "collection_1")
//       val collections = Await.result(actorRef.ask(GetCollections(false))(5 seconds).mapTo[ListBuffer[String]].map{ results => results }, Duration.Inf)
//       collections.size should be(1)
//     }

//     "add the collection to the read-only buffer" in {

//       actorRef ! AddCollection(false, "collection_2")
//       val collections = Await.result(actorRef.ask(GetCollections(false))(5 seconds).mapTo[ListBuffer[String]].map{ results => results }, Duration.Inf)
//       collections.contains("collection_2") should be(true)
//     }

//   }

// }
