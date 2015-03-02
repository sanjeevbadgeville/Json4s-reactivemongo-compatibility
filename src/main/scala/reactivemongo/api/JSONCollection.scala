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

package reactivemongo.api

import com.jacoby6000.json.json4s.BSONFormats.{JValueWriter, BSONObjectIDFormat}
import org.jboss.netty.buffer.ChannelBuffer
import org.json4s._
import play.api.libs.iteratee.{Enumeratee, Iteratee, Enumerator}
import reactivemongo.api.collections.{GenericHandlers, GenericCollection, GenericQueryBuilder}
import reactivemongo.core.commands.{GetLastError, LastError}
import reactivemongo.core.netty.{ChannelBufferWritableBuffer, BufferSequence}
import reactivemongo.core.protocol._
import reactivemongo.{JSONGenericHandlers, JSONQueryBuilder}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
 * A Collection that interacts with the Play JSON library, using `Reader` and `Writer`.
 */
case class JSONCollection(
                           db: DB,
                           name: String,
                           failoverStrategy: FailoverStrategy) extends GenericCollection[JObject, Reader, Writer] with GenericHandlers[JObject, Reader, Writer] with CollectionMetaCommands with JSONGenericHandlers {
  import Extraction._
  import reactivemongo.utils.EitherMappableFuture._

  def genericQueryBuilder: JSONQueryBuilder = JSONQueryBuilder(this, failoverStrategy)

  private def writeDoc[T](doc: T)(implicit formats: Formats): ChannelBuffer = {
    val buffer = ChannelBufferWritableBuffer()
    StructureBufferWriter.write(Extraction.decompose(doc).extract[JObject], buffer).buffer
  }

  private def writeDoc[T](doc: T, writer: Writer[T]) = BufferWriterInstance(writer).write(doc, ChannelBufferWritableBuffer()).buffer



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
  def save(doc: JObject, writeConcern: GetLastError)(implicit ec: ExecutionContext): Future[LastError] = {
    import reactivemongo.bson._

    doc \ "_id" match {
      case JNothing | JNull => super.insert(doc ++ JObject("_id" -> BSONObjectIDFormat.write(BSONObjectID.generate)), writeConcern)(JValueWriter, ec)
      case id => super.update(JObject("_id" -> id).asInstanceOf[JValue], doc, writeConcern, upsert = true)(JValueWriter, JValueWriter, ec)
    }
  }

  /**
   * Inserts the document, or updates it if it already exists in the collection.
   *
   * @param doc The document to save.
   * @param writeConcern the [reactivemongo.core.commands.GetLastError] command message to send in order to control how the document is inserted. Defaults to GetLastError().
   */
  def save[T](doc: T, writeConcern: GetLastError = GetLastError())(implicit ec: ExecutionContext, writer: Writer[T]): Future[LastError] =
    save(writer.write(doc), writeConcern)

  /**
   * Find the documents matching the given criteria.
   *
   * This method accepts any query and projection object, provided that there is an implicit `Writer[S]` typeclass for handling them in the scope.
   *
   * Please take a look to the [http://www.mongodb.org/display/DOCS/Querying mongodb documentation] to know how querying works.
   *
   * @tparam S the type of the selector (the query). An implicit `Writer[S]` typeclass for handling it has to be in the scope.
   *
   * @param selector The selector query.
   *
   * @return a [GenericQueryBuilder] that you can use to to customize the query. You can obtain a cursor by calling the method [reactivemongo.api.Cursor] on this query builder.
   */
  def find[S](selector: S)(implicit formats: Formats): JSONQueryBuilder =
    genericQueryBuilder.query(decompose(selector))(JValueWriter)

  /**
   * Find the documents matching the given criteria.
   *
   * This method accepts any query and projection object, provided that there is an implicit `Writer[S]` typeclass for handling them in the scope.
   *
   * Please take a look to the [http://www.mongodb.org/display/DOCS/Querying mongodb documentation] to know how querying works.
   *
   * @tparam S the type of the selector (the query). An implicit `Writer[S]` typeclass for handling it has to be in the scope.
   * @tparam P the type of the projection object. An implicit `Writer[P]` typeclass for handling it has to be in the scope.
   *
   * @param selector The selector query.
   * @param projection Get only a subset of each matched documents. Defaults to None.
   *
   * @return a [GenericQueryBuilder] that you can use to to customize the query. You can obtain a cursor by calling the method [reactivemongo.api.Cursor] on this query builder.
   */
  def find[S, P](selector: S, projection: P)(implicit formats: Formats): JSONQueryBuilder =
    genericQueryBuilder.query(decompose(selector)).projection(decompose(projection))

  /**
   * Inserts a document into the collection and wait for the [reactivemongo.core.commands.LastError] result.
   *
   * Please read the documentation about [reactivemongo.core.commands.GetLastError] to know how to use it properly.
   *
   * @tparam T the type of the document to insert. An implicit `Writer[T]` typeclass for handling it has to be in the scope.
   *
   * @param document the document to insert.
   * @param writeConcern the [reactivemongo.core.commands.GetLastError] command message to send in order to control how the document is inserted. Defaults to GetLastError().
   *
   * @return a future [reactivemongo.core.commands.LastError] that can be used to check whether the insertion was successful.
   */
  def insert[T](document: T, writeConcern: GetLastError)(implicit formats: Formats,  ec: ExecutionContext): Future[LastError] =
    insert(decompose(document).extract[JObject],writeConcern)(formats, ec)


  /**
   * Inserts a document into the collection and wait for the [reactivemongo.core.commands.LastError] result.
   *
   * Please read the documentation about [reactivemongo.core.commands.GetLastError] to know how to use it properly.
   *
   * @param document the document to insert.
   * @param writeConcern the [reactivemongo.core.commands.GetLastError] command message to send in order to control how the document is inserted. Defaults to GetLastError().
   *
   * @return a future [reactivemongo.core.commands.LastError] that can be used to check whether the insertion was successful.
   */
  def insert(document: JObject, writeConcern: GetLastError)(implicit formats: Formats, ec: ExecutionContext): Future[LastError] = super.insert(document, writeConcern)(ec)

  /**
   * Inserts a document into the collection and wait for the [reactivemongo.core.commands.LastError] result.
   *
   * @param document the document to insert.
   *
   * @return a future [reactivemongo.core.commands.LastError] that can be used to check whether the insertion was successful.
   */
  def insert(document: JObject)(implicit formats: Formats, ec: ExecutionContext): Future[LastError] = insert(document, GetLastError())(formats, ec)

  /**
   * Updates one or more documents matching the given selector with the given modifier or update object.
   *
   * @tparam S the type of the selector object. An implicit `Writer[S]` typeclass for handling it has to be in the scope.
   * @tparam U the type of the modifier or update object. An implicit `Writer[U]` typeclass for handling it has to be in the scope.
   *
   * @param selector the selector object, for finding the documents to update.
   * @param update the modifier object (with special keys like \$set) or replacement object.
   * @param writeConcern the [reactivemongo.core.commands.GetLastError] command message to send in order to control how the documents are updated. Defaults to GetLastError().
   * @param upsert states whether the update objet should be inserted if no match found. Defaults to false.
   * @param multi states whether the update may be done on all the matching documents.
   *
   * @return a future [reactivemongo.core.commands.LastError] that can be used to check whether the update was successful.
   */
  def update[S, U](selector: S, update: U, writeConcern: GetLastError, upsert: Boolean, multi: Boolean)(implicit selectorFormats: Formats, updateWriter: Writer[U], ec: ExecutionContext): Future[LastError] = watchFailure {
    val flags = 0 | (if (upsert) UpdateFlags.Upsert else 0) | (if (multi) UpdateFlags.MultiUpdate else 0)
    val op = Update(fullCollectionName, flags)
    val bson = writeDoc(selector)(selectorFormats)
    bson.writeBytes(writeDoc(update, updateWriter))
    val checkedWriteRequest = CheckedWriteRequest(op, BufferSequence(bson), writeConcern)
    Failover(checkedWriteRequest, db.connection, failoverStrategy).future.mapEither(LastError.meaningful)
  }

  /**
   * Remove the matched document(s) from the collection and wait for the [reactivemongo.core.commands.LastError] result.
   *
   * Please read the documentation about [reactivemongo.core.commands.GetLastError] to know how to use it properly.
   *
   * @tparam T the type of the selector of documents to remove. An implicit `Writer[T]` typeclass for handling it has to be in the scope.
   *
   * @param query the selector of documents to remove.
   * @param writeConcern the [reactivemongo.core.commands.GetLastError] command message to send in order to control how the documents are removed. Defaults to GetLastError().
   * @param firstMatchOnly states whether only the first matched documents has to be removed from this collection.
   *
   * @return a Future[reactivemongo.core.commands.LastError] that can be used to check whether the removal was successful.
   */
  def remove[T](query: T, writeConcern: GetLastError, firstMatchOnly: Boolean)(implicit formats: Formats, ec: ExecutionContext): Future[LastError] = watchFailure {
    val op = Delete(fullCollectionName, if (firstMatchOnly) 1 else 0)
    val bson = writeDoc(query)(formats)
    val checkedWriteRequest = CheckedWriteRequest(op, BufferSequence(bson), writeConcern)
    Failover(checkedWriteRequest, db.connection, failoverStrategy).future.mapEither(LastError.meaningful)
  }

  def bulkInsert[T](enumerator: Enumerator[T], writeConcern: GetLastError, bulkSize: Int, bulkByteSize: Int)(implicit formats: Formats, ec: ExecutionContext): Future[Int] =
    enumerator |>>> bulkInsertIteratee(writeConcern, bulkSize, bulkByteSize)

  def bulkInsertIteratee[T](writeConcern: GetLastError, bulkSize: Int, bulkByteSize: Int)(implicit formats: Formats, ec: ExecutionContext): Iteratee[T, Int] =
    Enumeratee.map { doc: T => writeDoc(doc)(formats) } &>> bulk.iteratee(this, writeConcern, bulkSize, bulkByteSize)

  /**
   * Remove the matched document(s) from the collection without writeConcern.
   *
   * Please note that you cannot be sure that the matched documents have been effectively removed and when (hence the Unit return type).
   *
   * @tparam T the type of the selector of documents to remove. An implicit `Formats` has to be in the scope.
   *
   * @param query the selector of documents to remove.
   * @param firstMatchOnly states whether only the first matched documents has to be removed from this collection.
   */
  def uncheckedRemove[T](query: T, firstMatchOnly: Boolean)(implicit formats: Formats, ec: ExecutionContext): Unit = {
    val op = Delete(fullCollectionName, if (firstMatchOnly) 1 else 0)
    val bson = writeDoc(query)(formats)
    val message = RequestMaker(op, BufferSequence(bson))
    db.connection.send(message)
  }

  /**
   * Updates one or more documents matching the given selector with the given modifier or update object.
   *
   * Please note that you cannot be sure that the matched documents have been effectively updated and when (hence the Unit return type).
   *
   * @tparam S the type of the selector object. An implicit `Formats` has to be in the scope.
   * @tparam U the type of the modifier or update object. An implicit `Writer[U]` typeclass for handling it has to be in the scope.
   *
   * @param selector the selector object, for finding the documents to update.
   * @param update the modifier object (with special keys like \$set) or replacement object.
   * @param upsert states whether the update object should be inserted if no match found. Defaults to false.
   * @param multi states whether the update may be done on all the matching documents.
   */
  def uncheckedUpdate[S, U](selector: S, update: U, upsert: Boolean, multi: Boolean)(implicit selectorFormats: Formats, updateWriter: Writer[U]): Unit = {
    uncheckedUpdate(writeDoc(selector)(selectorFormats), writeDoc(update,updateWriter), upsert, multi)
  }

  /**
   * Updates one or more documents matching the given selector with the given modifier or update object.
   *
   * Please note that you cannot be sure that the matched documents have been effectively updated and when (hence the Unit return type).
   *
   * @tparam S the type of the selector object. An implicit `Writer[S]` typeclass for handling it has to be in the scope.
   * @tparam U the type of the modifier or update object. An implicit `Formats` has to be in the scope.
   *
   * @param selector the selector object, for finding the documents to update.
   * @param update the modifier object (with special keys like \$set) or replacement object.
   * @param upsert states whether the update object should be inserted if no match found. Defaults to false.
   * @param multi states whether the update may be done on all the matching documents.
   */
  def uncheckedUpdate[S, U](selector: S, update: U, upsert: Boolean, multi: Boolean)(implicit selectorWriter: Writer[S], updateFormats: Formats): Unit = {
    uncheckedUpdate(writeDoc(selector, selectorWriter), writeDoc(update)(updateFormats), upsert, multi)
  }

  /**
   * Updates one or more documents matching the given selector with the given modifier or update object.
   *
   * Please note that you cannot be sure that the matched documents have been effectively updated and when (hence the Unit return type).
   *
   * @tparam S the type of the selector object. An implicit `Formats` has to be in the scope. You may explicitly specify the formats to use if you wish to use different formats for the selector and the updater.
   * @tparam U the type of the modifier or update object. An implicit `Formats` has to be in the scope. You may explicitly specify the formats to use if you wish to use different formats for the selector and the updater.
   *
   * @param selector the selector object, for finding the documents to update.
   * @param update the modifier object (with special keys like \$set) or replacement object.
   * @param upsert states whether the update object should be inserted if no match found. Defaults to false.
   * @param multi states whether the update may be done on all the matching documents.
   */
  def uncheckedUpdate[S, U](selector: S, update: U, upsert: Boolean, multi: Boolean)(implicit selectorFormats: Formats, updateFormats: Formats): Unit = {
    uncheckedUpdate(writeDoc(selector)(selectorFormats), writeDoc(update)(updateFormats), upsert, multi)
  }

  /**
   * Updates one or more documents matching the given selector with the given modifier or update object.
   *
   * Please note that you cannot be sure that the matched documents have been effectively updated and when (hence the Unit return type).
   *
   * @param selectorBuffer the selector object, for finding the documents to update.
   * @param updateBuffer the modifier object (with special keys like \$set) or replacement object.
   * @param upsert states whether the update object should be inserted if no match found. Defaults to false.
   * @param multi states whether the update may be done on all the matching documents.
   */
  def uncheckedUpdate(selectorBuffer: ChannelBuffer, updateBuffer: ChannelBuffer, upsert: Boolean, multi: Boolean): Unit = {
    val flags = 0 | (if (upsert) UpdateFlags.Upsert else 0) | (if (multi) UpdateFlags.MultiUpdate else 0)
    val op = Update(fullCollectionName, flags)
    selectorBuffer.writeBytes(updateBuffer)
    val message = RequestMaker(op, BufferSequence(selectorBuffer))
    db.connection.send(message)
  }


  /**
   * Inserts a document into the collection without writeConcern.
   *
   * Please note that you cannot be sure that the document has been effectively written and when (hence the Unit return type).
   *
   * @tparam T the type of the document to insert. An implicit `Writer[T]` typeclass for handling it has to be in the scope.
   *
   * @param document the document to insert.
   */
  def uncheckedInsert[T](document: T)(implicit formats: Formats): Unit = {
    val op = Insert(0, fullCollectionName)
    val bson = writeDoc(document)(formats)
    val message = RequestMaker(op, BufferSequence(bson))
    db.connection.send(message)
  }
}
