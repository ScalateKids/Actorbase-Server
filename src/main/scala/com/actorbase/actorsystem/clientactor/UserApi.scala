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

package com.actorbase.actorsystem.clientactor

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
      * @param
      * @return
      * @throws
      */
    def passwordMatches(password: String): Boolean = hashedPassword.get == password

  }

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  case object User {

    /**
      * Mock password retrieving, will send a message to the Main
      * actor and get the real password from the Userkeeper
      *
      * @param
      * @return
      * @throws
      */
    def apply(login: String): User = new User(login, Some("pass"))
  }

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  case class AuthInfo(val user: User)

}
