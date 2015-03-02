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

package com.reactivemongojson4splugin.reactivemongo.api

import com.reactivemongojson4splugin.json4s.BSONFormats.{BSONObjectIDFormat, JValueWriter}
import com.reactivemongojson4splugin.reactivemongo.{JSONGenericHandlers, JSONQueryBuilder}
import org.json4s._
import reactivemongo.api.collections.{GenericCollection, GenericHandlers, GenericQueryBuilder}
import reactivemongo.api.{CollectionMetaCommands, DB, FailoverStrategy}
import reactivemongo.core.commands.{GetLastError, LastError}

import scala.concurrent.{ExecutionContext, Future}

/**
 * A Collection that interacts with the Play JSON library, using `Reader` and `Writer`.
 */
case class JSONCollection(db: DB, name: String, failoverStrategy: FailoverStrategy) extends JSONCollectionLike {
  def genericQueryBuilder = JSONQueryBuilder(this, failoverStrategy)
}

trait JSONCollectionLike extends GenericCollection[JObject, Reader, Writer] with GenericHandlers[JObject, Reader, Writer] with CollectionMetaCommands with JSONGenericHandlers {

  val db: DB
  val name: String
  val failoverStrategy: FailoverStrategy

  def genericQueryBuilder: GenericQueryBuilder[JObject, Reader, Writer]

  /**
   * Inserts the document, or updates it if it already exists in the collection.
   *
   * @param doc The document to save.
   */
  def save(doc: JObject)(implicit ec: ExecutionContext): Future[LastError] =
    save(doc, GetLastError())

  /**
   * Inserts the document, or updates it if it already exists in the collection.
   *
   * @param doc The document to save.
   * @param writeConcern the [reactivemongo.core.commands.GetLastError] command message to send in order to control how the document is inserted. Defaults to GetLastError().
   */
  def save(doc: JValue, writeConcern: GetLastError)(implicit ec: ExecutionContext): Future[LastError] = {
    import reactivemongo.bson._

    doc \ "_id" match {
      case JNothing | JNull => super.insert(doc ++ JObject("_id" -> BSONObjectIDFormat.write(BSONObjectID.generate)), writeConcern)(JValueWriter, ec)
      case id => super.update(JObject("_id" -> id), doc, writeConcern, upsert = true)(JValueWriter, JValueWriter, ec)
    }
  }

  /**
   * Inserts the document, or updates it if it already exists in the collection.
   *
   * @param doc The document to save.
   * @param writeConcern the [reactivemongo.core.commands.GetLastError] command message to send in order to control how the document is inserted. Defaults to GetLastError().
   */
  def save[T: Manifest](doc: T, writeConcern: GetLastError)(implicit ec: ExecutionContext, writer: Writer[T]): Future[LastError] =
    save(writer.write(doc), writeConcern)

  def save[T: Manifest](doc: T)(implicit ec: ExecutionContext, writer: Writer[T]): Future[LastError] =
    save(doc, GetLastError())
}
