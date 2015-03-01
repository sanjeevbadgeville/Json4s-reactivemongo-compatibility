package com.jacoby6000.json

/**
 * Created by jbarber on 2/26/15.
 *
 */

import org.json4s.Extraction
import reactivemongo.api.{JSONCollection, DefaultDB}
import org.json4s.JsonAST._
import scala.concurrent.ExecutionContext

class PersonDao(db: DefaultDB)(implicit val ec: ExecutionContext) {
  val collection = db.collection[JSONCollection]("persons")
  import com.jacoby6000.json.json4s.BSONFormats._

  def get(id: String) = {
    collection.find(JObject("_id" -> JString(id))).extractOne[Person]
  }

  def insert(person: Person) = {
    collection.save(Extraction.decompose(person))
  }
}