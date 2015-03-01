package reactivemongo

import org.json4s._
import reactivemongo.api.collections._
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.buffer.ReadableBuffer

/**
 * Created by jbarber on 2/28/15.
 */
object JSONGenericHandlers extends JSONGenericHandlers

trait JSONGenericHandlers extends GenericHandlers[JObject, Reader, Writer] {
  import com.jacoby6000.json.json4s.BSONFormats._

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