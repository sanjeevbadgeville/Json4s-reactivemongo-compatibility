package reactivemongo

import org.json4s._
import reactivemongo.api.collections.GenericCollectionProducer
import reactivemongo.api.{JSONCollection, DB, FailoverStrategy}

/**
 * A Collection that interacts with the Json4s library, using `Reader` and `Writer`.
 */
object `package` {
  implicit object JSONCollectionProducer extends GenericCollectionProducer[JObject, Reader, Writer, JSONCollection] {
    def apply(db: DB, name: String, failoverStrategy: FailoverStrategy) = new JSONCollection(db, name, failoverStrategy)
  }
}
