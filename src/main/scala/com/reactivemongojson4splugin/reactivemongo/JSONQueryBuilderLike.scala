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
import reactivemongo.api._
import reactivemongo.api.collections.{BufferReader, GenericQueryBuilder}
import reactivemongo.bson.buffer.WritableBuffer

/**
 * Created by jbarber on 2/28/15.
 */
trait JSONQueryBuilderLike extends GenericQueryBuilder[JObject, Reader, Writer] with JSONGenericHandlers {

  val collection: Collection
  val failover: FailoverStrategy
  val queryOption: Option[JObject]
  val sortOption: Option[JObject]
  val projectionOption: Option[JObject]
  val hintOption: Option[JObject]
  val explainFlag: Boolean
  val snapshotFlag: Boolean
  val commentString: Option[String]
  val options: QueryOpts

  import reactivemongo.utils.option

  implicit val formats: Formats
  implicit val bsonHandlers: ImplicitBSONHandlers

  private def empty: JObject = JObject()


  protected def writeStructureIntoBuffer[B <: WritableBuffer, T: Manifest](document: JObject, buffer: B): B = {
    JSONGenericHandlers.StructureBufferWriter.write(document, buffer)
  }

  object structureReader extends Reader[JObject] {
    def read(json: JValue): JObject = json.extract[JObject]
  }

  def convert[T](reader: Reader[T]): BufferReader[T] = JSONDocumentReaderAsBufferReader(reader)

  def merge: JObject = {
    if (!sortOption.isDefined && !hintOption.isDefined && !explainFlag && !snapshotFlag && !commentString.isDefined)
      queryOption.getOrElse(JObject())
    else {
      (JObject("$query" -> (queryOption.getOrElse(empty): JObject)) ++
        sortOption.map(o => JObject("$orderby" -> o)).getOrElse(empty) ++
        hintOption.map(o => JObject("$hint" -> o)).getOrElse(empty) ++
        commentString.map(o => JObject("$comment" -> JString(o))).getOrElse(empty) ++
        option(explainFlag, JBool(true)).map(o => JObject("$explain" -> o)).getOrElse(empty) ++
        option(snapshotFlag, JBool(true)).map(o => JObject("$snapshot" -> o)).getOrElse(empty)).extract[JObject]
    }
  }

}
