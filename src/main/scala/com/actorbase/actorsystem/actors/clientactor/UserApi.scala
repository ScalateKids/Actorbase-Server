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

package com.actorbase.actorsystem.actors.clientactor

import com.github.t3hnar.bcrypt._
import org.mindrot.jbcrypt.BCrypt

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Insert description here
  *
  * @param
  * @return
  * @throws
  */
object UserApi {

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  case class User(login: String, hashedPassword: Option[String] = None) {

    /**
      * Basic password matching, will be implemented at least with
      * bcrypt for hashing the password
      *
      * @param password a String representing the password to be tested
      * @return a Boolean, true if the password matches, false otherwise
      * @throws
      */
    def passwordMatches(password: String): Boolean = hashedPassword.exists(hash => BCrypt.checkpw(password, hash))

  }

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  case class AuthInfo(val user: User) {

    /**
      * Verify wether the user has admin privileges for restricted area
      * operations
      *
      * @return true if the user has admin privileges, false otherwise
      * @throws
      */
    def hasAdminPermissions = true
  }

}