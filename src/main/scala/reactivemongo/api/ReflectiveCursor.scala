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

import org.jboss.netty.buffer.ChannelBuffer
import org.json4s.{Reader, Formats}
import org.json4s.JsonAST.JObject
import play.api.libs.iteratee._
import reactivemongo.utils.ExtendedFutures
import reactivemongo.{JSONGenericHandlers, JSONDocumentReaderAsBufferReader}
import reactivemongo.api.collections.BufferReader
import reactivemongo.core.iteratees.{CustomEnumeratee, CustomEnumerator}
import reactivemongo.core.netty.{ChannelBufferReadableBuffer, BufferSequence}
import reactivemongo.core.protocol._

import scala.annotation.tailrec
import scala.collection.generic.CanBuildFrom
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class ReflectiveCursor[T](
                        query: Query,
                        documents: BufferSequence,
                        readPreference: ReadPreference,
                        mongoConnection: MongoConnection,
                        failoverStrategy: FailoverStrategy)(implicit ev1: Manifest[T], reader: BufferReader[JObject], formats: Formats) extends Cursor[T] {
  import Cursor.logger


  private def next(response: Response)(implicit ctx: ExecutionContext): Option[Future[Response]] = {
    if (response.reply.cursorID != 0) {
      val op = GetMore(query.fullCollectionName, query.numberToReturn, response.reply.cursorID)
      logger.trace("[Cursor] Calling next on " + response.reply.cursorID + ", op=" + op)
      Some(Failover(RequestMaker(op).copy(channelIdHint = Some(response.info.channelId)), mongoConnection, failoverStrategy).future)
    } else {
      logger.error("[Cursor] Call to next() but cursorID is 0, there is probably a bug")
      None
    }
  }

  @inline
  private def hasNext(response: Response): Boolean = response.reply.cursorID != 0

  @inline
  private def hasNext(response: Response, maxDocs: Int): Boolean = {
    response.reply.cursorID != 0 && (response.reply.numberReturned + response.reply.startingFrom) < maxDocs
  }

  @inline
  private def makeIterator(response: Response) = ReflectiveReplyDocumentIterator(response.reply, response.documents)(ev1, reader, formats)

  @inline
  private def makeRequest(implicit ctx: ExecutionContext): Future[Response] =
    Failover(RequestMaker(query, documents, readPreference), mongoConnection, failoverStrategy).future

  @inline
  private def isTailable = (query.flags & QueryFlags.TailableCursor) == QueryFlags.TailableCursor

  def simpleCursorEnumerateResponses(maxDocs: Int = Int.MaxValue)(implicit ctx: ExecutionContext): Enumerator[Response] = {
    Enumerator.flatten(makeRequest.map(new CustomEnumerator.SEnumerator(_)(
      next = response => if (hasNext(response, maxDocs)) next(response) else None,
      cleanUp = response =>
        if (response.reply.cursorID != 0) {
          logger.debug(s"[Cursor] Clean up ${response.reply.cursorID}, sending KillCursor")
          mongoConnection.send(RequestMaker(KillCursors(Set(response.reply.cursorID))))
        } else logger.trace(s"[Cursor] Cursor exhausted (${response.reply.cursorID})"))))
  }

  def tailableCursorEnumerateResponses(maxDocs: Int = Int.MaxValue)(implicit ctx: ExecutionContext): Enumerator[Response] = {
    Enumerator.flatten(makeRequest.map { response =>
      new CustomEnumerator.SEnumerator((response, maxDocs))(
        next = current => {
          if (maxDocs - current._1.reply.numberReturned > 0) {
            val nextResponse =
              if (hasNext(current._1)) {
                next(current._1)
              } else {
                logger.debug("[Tailable Cursor] Current cursor exhausted, renewing...")
                Some(ExtendedFutures.DelayedFuture(500, mongoConnection.actorSystem).flatMap(_ => makeRequest))
              }
            nextResponse.map(_.map((_, maxDocs - current._1.reply.numberReturned)))
          } else None
        },
        cleanUp = current =>
          if (current._1.reply.cursorID != 0) {
            logger.debug(s"[Tailable Cursor] Closing  cursor ${current._1.reply.cursorID}, cleanup")
            mongoConnection.send(RequestMaker(KillCursors(Set(current._1.reply.cursorID))))
          } else logger.trace(s"[Tailable Cursor] Cursor exhausted (${current._1.reply.cursorID})"))
    }).map(_._1)
  }

  def rawEnumerateResponses(maxDocs: Int = Int.MaxValue)(implicit ctx: ExecutionContext): Enumerator[Response] =
    if (isTailable) tailableCursorEnumerateResponses(maxDocs) else simpleCursorEnumerateResponses(maxDocs)

  def enumerateResponses(maxDocs: Int = Int.MaxValue, stopOnError: Boolean = false)(implicit ctx: ExecutionContext): Enumerator[Response] =
    rawEnumerateResponses(maxDocs) &> {
      if (stopOnError)
        CustomEnumeratee.stopOnError
      else CustomEnumeratee.recover {
        new CustomEnumeratee.RecoverFromErrorFunction {
          def apply[E, A](throwable: Throwable, input: Input[E], continue: () => Iteratee[E, A]): Iteratee[E, A] = throwable match {
            case e: ReplyDocumentIteratorExhaustedException =>
              val errstr = "ReplyDocumentIterator exhausted! " +
                "Was this enumerator applied to many iteratees concurrently? " +
                "Stopping to prevent infinite recovery."
              logger.error(errstr, e)
              Error(errstr, input)
            case e =>
              logger.debug("There was an exception during the stream, dropping it since stopOnError is false", e)
              continue()
          }
        }
      }
    }

  def enumerateBulks(maxDocs: Int = Int.MaxValue, stopOnError: Boolean = false)(implicit ctx: ExecutionContext): Enumerator[Iterator[T]] =
    enumerateResponses(maxDocs, stopOnError) &> Enumeratee.map(response => ReflectiveReplyDocumentIterator(response.reply, response.documents))

  def enumerate(maxDocs: Int = Int.MaxValue, stopOnError: Boolean = false)(implicit ctx: ExecutionContext): Enumerator[T] = {
    @tailrec
    def next(it: Iterator[JObject], stopOnError: Boolean): Option[Try[JObject]] = {
      if (it.hasNext) {
        val tried = Try(it.next())
        if (tried.isFailure && !stopOnError)
          next(it, stopOnError)
        else Some(tried)
      } else None
    }
    enumerateResponses(maxDocs, stopOnError) &> Enumeratee.mapFlatten { response =>
      val iterator = ReplyDocumentIterator(response.reply, response.documents)
      if(!iterator.hasNext)
        Enumerator.empty
      else
        CustomEnumerator.SEnumerator(iterator.next()) { _ =>
          next(iterator, stopOnError).map {
            case Success(mt) => Future.successful(mt)
            case Failure(e)  => Future.failed(e)
          }
        }.map(_.extract[T])
    }
  }

  def collect[M[_]](upTo: Int = Int.MaxValue, stopOnError: Boolean = true)(implicit cbf: CanBuildFrom[M[_], T, M[T]], ctx: ExecutionContext): Future[M[T]] = {
    (enumerateResponses(upTo, stopOnError) |>>> Iteratee.fold(cbf.apply()) { (builder, response) =>

      def tried[U](it: Iterator[U]) = new Iterator[Try[U]] { def hasNext = it.hasNext; def next() = Try(it.next()) }

      logger.trace(s"[collect] got response $response")

      val filteredIterator =
        if (!stopOnError)
          tried(makeIterator(response)).filter(_.isSuccess).map(_.get)
        else makeIterator(response)
      val iterator =
        if (upTo < response.reply.numberReturned + response.reply.startingFrom)
          filteredIterator.take(upTo - response.reply.startingFrom)
        else filteredIterator
      builder ++= iterator
    }).map(_.result())
  }
}

private[reactivemongo] case class ReflectiveReplyDocumentIterator[T: Manifest](private val reply: Reply, private val buffer: ChannelBuffer)(implicit reader: BufferReader[JObject], formats: Formats) extends Iterator[T] {
  def hasNext = buffer.readable
  def next =
    try {
      reader.read(ChannelBufferReadableBuffer(buffer.readBytes(buffer.getInt(buffer.readerIndex)))).extract[T]
    } catch {
      case e: IndexOutOfBoundsException =>
        /*
         * If this happens, the buffer is exhausted, and there is probably a bug.
         * It may happen if an enumerator relying on it is concurrently applied to many iteratees – which should not be done!
         */
        throw new ReplyDocumentIteratorExhaustedException(e)
    }
}