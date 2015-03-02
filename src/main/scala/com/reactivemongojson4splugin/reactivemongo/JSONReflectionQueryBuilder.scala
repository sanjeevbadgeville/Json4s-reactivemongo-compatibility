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

import com.reactivemongojson4splugin.reactivemongo.api.ReflectiveCursor
import org.json4s._
import reactivemongo.api._
import reactivemongo.core.netty.{BufferSequence, ChannelBufferWritableBuffer}
import reactivemongo.core.protocol.{Query, QueryFlags}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by jbarber on 2/28/15.
 */
case class JSONReflectionQueryBuilder(
                                       collection: Collection,
                                       failover: FailoverStrategy,
                                       queryOption: Option[JObject] = None,
                                       sortOption: Option[JObject] = None,
                                       projectionOption: Option[JObject] = None,
                                       hintOption: Option[JObject] = None,
                                       explainFlag: Boolean = false,
                                       snapshotFlag: Boolean = false,
                                       commentString: Option[String] = None,
                                       options: QueryOpts = QueryOpts()) extends JSONQueryBuilderLike {

  import org.json4s.Extraction._

  implicit val formats = DefaultFormats
  implicit val bsonHandlers = ImplicitBSONHandlers


  type Self = JSONReflectionQueryBuilder


  def copy(queryOption: Option[JObject], sortOption: Option[JObject], projectionOption: Option[JObject], hintOption: Option[JObject], explainFlag: Boolean, snapshotFlag: Boolean, commentString: Option[String], options: QueryOpts, failover: FailoverStrategy): JSONReflectionQueryBuilder =
    JSONReflectionQueryBuilder(collection, failover, queryOption, sortOption, projectionOption, hintOption, explainFlag, snapshotFlag, commentString, options)

  /**
   * Sends this query and gets a future `Option[JValue]`.
   *
   * An implicit `ExecutionContext` must be present in the scope.
   */
  def one(implicit ec: ExecutionContext): Future[Option[JValue]] = copy(options = options.batchSize(1)).cursor(structureReader, ec).headOption

  /**
   * Sends this query and retrieves a JValue from the db representing the record, then extracts it into type T wrapped in an option.
   *
   * An implicit Formats capable of extracting T must be in scope.
   */
  def one[T: Manifest](implicit formats: Formats, ec: ExecutionContext): Future[Option[T]] = one(ec).collect {
    case Some(jv) => Some(jv.extract[T])
    case None => None
  }

  /**
   * Makes a [[Cursor]] of this query, which can be enumerated.
   *
   * An implicit `Formats` must be present in the scope.
   *
   * @param readPreference The ReadPreference for this request. If the ReadPreference implies that this request might be run on a Secondary, the slaveOk flag will be set.
   */
  def cursor[T](readPreference: ReadPreference)(implicit formats: Formats, ev1: Manifest[T]): ReflectiveCursor[T] = {
    val documents = BufferSequence {
      val buffer = writeStructureIntoBuffer(merge, ChannelBufferWritableBuffer())
      projectionOption.map { projection =>
        writeStructureIntoBuffer(projection, buffer)
      }.getOrElse(buffer).buffer
    }

    val flags = if (readPreference.slaveOk) options.flagsN | QueryFlags.SlaveOk else options.flagsN

    val op = Query(flags, collection.fullCollectionName, options.skipN, options.batchSizeN)

    new ReflectiveCursor[T](op, documents, readPreference, collection.db.connection, failover)(ev1, StructureBufferReader, formats)
  }

  /**
   * Sends this query and gets a [[Cursor]] of instances of `T`.
   *
   * An implicit `Reader[T]` must be present in the scope.
   */
  def cursor[T](implicit formats: Formats, ec: ExecutionContext, ev1: Manifest[T]): ReflectiveCursor[T] = cursor(ReadPreference.primary)(formats, ev1)

  /**
   * Sets the projection document (for [[http://www.mongodb.org/display/DOCS/Retrieving+a+Subset+of+Fields retrieving only a subset of fields]]).
   *
   * @tparam Pjn The type of the projection. An implicit `Writer][Pjn]` typeclass for handling it has to be in the scope.
   */
  def projection[Pjn: Manifest](p: Pjn)(implicit formats: Formats): Self = copy(projectionOption = Some(
    decompose(p).extract[JObject]))
}
