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

package com.actorbase.actorsystem.restclientactor

import akka.actor.ActorRef
import spray.routing.authentication.BasicAuth
import spray.routing.authentication.UserPass
import spray.routing.directives.AuthMagnet
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.actorbase.actorsystem.restclientactor.UserApi.{User, AuthInfo}

/**
  * Insert description here
  *
  * @param
  * @return
  * @throws
  */
trait Authenticator {

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  def basicUserAuthenticator(implicit ec: ExecutionContext, main: ActorRef): AuthMagnet[AuthInfo] = {

    /**
      * Insert description here
      *
      * @param
      * @return
      * @throws
      */
    def validateUser(userPass: Option[UserPass]): Option[AuthInfo] = {
      for {
        p <- userPass
        user = User(p.user, main)
        if user.passwordMatches(p.pass)
      } yield new AuthInfo(user)
    }

    /**
      * Insert description here
      *
      * @param
      * @return
      * @throws
      */
    def authenticator(userPass: Option[UserPass]): Future[Option[AuthInfo]] = Future { validateUser(userPass) }

    BasicAuth(authenticator _, realm = "Private area")
  }
}
