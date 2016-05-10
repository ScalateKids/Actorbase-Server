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

package com.actorbase.actorsystem.utils

import java.io.{File, ByteArrayOutputStream, FileOutputStream, IOException, ObjectOutputStream}
import java.security.{InvalidKeyException, NoSuchAlgorithmException}
import javax.crypto.{BadPaddingException, Cipher, IllegalBlockSizeException, NoSuchPaddingException}
import javax.crypto.spec.SecretKeySpec

import scala.collection.immutable.TreeMap

object CryptoUtils {

  /** configuration init */
  val Algorithm = "AES"
  val Transformation = "AES"

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  def encrypt(key: String, inputData: TreeMap[String, Any], outputFile: File) = {

    val cipherMode: Int = Cipher.ENCRYPT_MODE

    /**
      * Insert description here
      *
      * @param
      * @return
      * @throws
      */
    def binarize(o: TreeMap[String, Any]): Array[Byte] = {
      val bos = new ByteArrayOutputStream()
      var out = new ObjectOutputStream(bos)
      out.writeObject(o);
      val bytes = bos.toByteArray()
      out.close();
      bos.close();
      bytes
    }

    try {

      val secretKey = new SecretKeySpec(key.getBytes(), Algorithm)
      val cipher = Cipher.getInstance(Transformation)
      cipher.init(cipherMode, secretKey)

      val outputBytes = cipher.doFinal(binarize(inputData))

      val outputStream = new FileOutputStream(outputFile)
      outputStream.write(outputBytes)

      outputStream.close()

    } catch {
      case na: NoSuchAlgorithmException => println(s"Error encrypting/decrypting file $na")
      case np: NoSuchPaddingException => println(s"Error encrypting/decrypting file $np")
      case ik: InvalidKeyException => println(s"Error encrypting/decrypting file $ik")
      case bp: BadPaddingException => println(s"Error encrypting/decrypting file $bp")
      case ib: IllegalBlockSizeException => println(s"Error encrypting/decrypting file $ib")
      case io: IOException => println(s"Error encrypting/decrypting file $io")
    }

  }

  /**
    * Insert description here
    *
    * @param
    * @return
    * @throws
    */
  def decrypt(key: String, inputData: TreeMap[String, Any], outputFile: File) = {

    val cipherMode: Int = Cipher.DECRYPT_MODE

    /**
      * Insert description here
      *
      * @param
      * @return
      * @throws
      */
    def binarize(o: TreeMap[String, Any]): Array[Byte] = {
      val bos = new ByteArrayOutputStream()
      var out = new ObjectOutputStream(bos)
      out.writeObject(o);
      val bytes = bos.toByteArray()
      out.close();
      bos.close();
      bytes
    }

    try {

      val secretKey = new SecretKeySpec(key.getBytes(), Algorithm)
      val cipher = Cipher.getInstance(Transformation)
      cipher.init(cipherMode, secretKey)

      val outputBytes = cipher.doFinal(binarize(inputData))

      val outputStream = new FileOutputStream(outputFile)
      outputStream.write(outputBytes)

      outputStream.close()

    } catch {
      case na: NoSuchAlgorithmException => println(s"Error encrypting/decrypting file $na")
      case np: NoSuchPaddingException => println(s"Error encrypting/decrypting file $np")
      case ik: InvalidKeyException => println(s"Error encrypting/decrypting file $ik")
      case bp: BadPaddingException => println(s"Error encrypting/decrypting file $bp")
      case ib: IllegalBlockSizeException => println(s"Error encrypting/decrypting file $ib")
      case io: IOException => println(s"Error encrypting/decrypting file $io")
    }

  }

}
