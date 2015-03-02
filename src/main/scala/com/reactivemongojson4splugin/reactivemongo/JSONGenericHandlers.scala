/*
 * Copyright 2012-2013 Stephane Godbillon (@sgodbillon)
 * Copyright 2015 Jacob Barber (@Jacoby6000)
 *
 * Changes made by Jacob Barber to adapt the Play-json implementations of the reactivemongo Play Framework plugin to be
 * used with json4s.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.reactivemongojson4splugin.reactivemongo

import org.json4s._
import reactivemongo.api.collections._
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.buffer.ReadableBuffer

/**
 * Created by jbarber on 2/28/15.
 */
object JSONGenericHandlers extends JSONGenericHandlers

trait JSONGenericHandlers extends GenericHandlers[JObject, Reader, Writer] {

  import com.reactivemongojson4splugin.json4s.BSONFormats._

  object StructureBufferReader extends BufferReader[JObject] {
    def writeBuffer(buffer: ReadableBuffer) = BSONDocumentFormat.write(BSONDocument.read(buffer))

    def read(buffer: ReadableBuffer) = {
      writeBuffer(buffer).extract[JObject]
    }
  }

  object StructureBufferWriter extends BufferWriter[JObject] {
    def write[B <: reactivemongo.bson.buffer.WritableBuffer](document: JObject, buffer: B): B = {
      BSONDocument.write(document.extract[BSONDocument], buffer)
      buffer
    }
  }

  case class BSONStructureReader[T](reader: Reader[T]) extends GenericReader[JObject, T] {
    def read(doc: JObject) = reader.read(doc)
  }

  case class BSONStructureWriter[T](writer: Writer[T]) extends GenericWriter[T, JObject] {
    def write(t: T) = writer.write(t).extract[JObject]
  }

  def StructureReader[T](reader: Reader[T]) = BSONStructureReader(reader)
  def StructureWriter[T](writer: Writer[T]): GenericWriter[T, JObject] = BSONStructureWriter(writer)
}