package reactivemongo

import org.json4s.Reader
import reactivemongo.api.collections.BufferReader
import reactivemongo.bson.buffer.ReadableBuffer

/**
 * Created by jbarber on 2/28/15.
 */
case class JSONDocumentReaderAsBufferReader[T](reader: Reader[T]) extends BufferReader[T] {
    def read(buffer: ReadableBuffer) = reader.read(JSONGenericHandlers.StructureBufferReader.read(buffer))
}
