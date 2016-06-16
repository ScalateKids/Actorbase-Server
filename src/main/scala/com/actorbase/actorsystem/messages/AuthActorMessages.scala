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

package com.actorbase.actorsystem.messages.AuthActorMessages

import com.actorbase.actorsystem.utils.ActorbaseCollection

/*
 * Trait that contains all the messages processable from the AuthActor
 */
sealed trait AuthActorMessages

/*
 * Message used to ask to the AuthActor to return a list containing all the users of the system
 */
case object ListUsers extends AuthActorMessages

case object Save extends AuthActorMessages

case object Clean extends AuthActorMessages

final case class AddCredentials(username: String, password: String) extends AuthActorMessages

final case class UpdateCredentials(username: String, password: String, newPassword: String) extends AuthActorMessages

final case class RemoveCredentials(username: String) extends AuthActorMessages

final case class Authenticate(username: String, password: String) extends AuthActorMessages

final case class AddCollectionTo(username: String, collection: ActorbaseCollection) extends AuthActorMessages

final case class RemoveCollectionFrom(username: String, collection: ActorbaseCollection) extends AuthActorMessages

final case class ListCollectionsOf(username: String) extends AuthActorMessages

final case class Init(username: String, password: String) extends AuthActorMessages
