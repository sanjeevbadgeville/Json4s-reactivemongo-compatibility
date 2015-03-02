package com.jacoby6000.json

/**
 * Created by jbarber on 2/26/15.
 *
 */

import org.json4s.{DefaultFormats, Formats, Extraction}
import reactivemongo.JSONGenericHandlers
import reactivemongo.api.{JSONCollection, DefaultDB}
import org.json4s.JsonAST._
import scala.concurrent.ExecutionContext

class PersonDao(db: DefaultDB)(implicit val ec: ExecutionContext) extends JSONGenericHandlers {
  val collection = db.collection[JSONCollection]("persons")
  import com.jacoby6000.json.json4s.BSONFormats._

  class AutoReader[T: Manifest](implicit formats: Formats) extends org.json4s.Reader[T] {
    def read(value: JValue): T = value.extract[T]
  }

  class AutoWriter[T: Manifest](implicit formats: Formats) extends org.json4s.Writer[T] {
    def write(value: T): JValue = Extraction.decompose(value)
  }

  def get(id: String) = {
    collection.find(JObject("_id" -> JString(id))).one[Person]
  }

  def insert(person: Person) = {
    collection.save(Extraction.decompose(person))
  }
}

