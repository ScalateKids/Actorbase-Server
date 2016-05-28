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

package com.actorbase.actorsystem.authactor

import akka.actor.{Actor, ActorLogging}

import com.actorbase.actorsystem.messages.AuthActorMessages._
import com.actorbase.actorsystem.utils.CryptoUtils
import com.github.t3hnar.bcrypt._
// import org.mindrot.jbcrypt.BCrypt

import java.io.File

class AuthActor extends Actor with ActorLogging {

  private val rootFolder = "actorbasedata/usersdata/"

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  override def receive = running(Map[String, String]("admin" -> "actorbase"))

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  def persist(credentials: Map[String, String]): Unit = {
    val key = "Dummy implicit k"
    val encryptedCredentialsFile = new File(rootFolder + "/usersdata.shadow")
    encryptedCredentialsFile.getParentFile.mkdirs
    CryptoUtils.encrypt(key, credentials, encryptedCredentialsFile)
  }

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  def running(credentials: Map[String, String]): Receive = {

    case message: AuthActorMessages => message match {

      /**
        * Insert description here
        *
        * @param
        * @return
        * @throws
        */
      case AddCredentials(username, password) =>
        log.info(s"$username added")
        val salt = password.bcrypt(generateSalt)
        persist(credentials + (username -> salt))
        context become running(credentials + (username -> salt))

      /**
        * Insert description here
        *
        * @param
        * @return
        * @throws
        */
      case RemoveCredentials(username) =>
        log.info(s"$username removed")
        persist(credentials - username)
        context become running(credentials - username)

      /**
        * Insert description here
        *
        * @param
        * @return
        * @throws
        */
      case Authenticate(username, password) =>
        if (credentials.contains(username))
          credentials get username map (pwd => if (pwd == password) sender ! "OK" else sender ! "None") getOrElse(sender ! "None")
        else sender ! "None"

    }
  }
}
