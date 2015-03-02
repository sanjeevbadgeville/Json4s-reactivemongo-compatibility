package com.jacoby6000.json

/**
 * Created by jbarber on 2/26/15.
 *
 */

import org.json4s.Extraction
import reactivemongo.JSONGenericHandlers
import reactivemongo.api.{JSONReflectionCollection, DefaultDB}
import org.json4s.JsonAST._
import reactivemongo.core.commands.GetLastError
import scala.concurrent.ExecutionContext

class PersonDao(db: DefaultDB)(implicit val ec: ExecutionContext) extends JSONGenericHandlers {
  val collection = db.collection[JSONReflectionCollection]("persons")
  import com.jacoby6000.json.json4s.BSONFormats._

  def get(id: String) = {
    collection.find(JObject("_id" -> JString(id))).one[Person]
  }

  def insert(person: Person) = {
    collection.save(person, GetLastError())
  }
}

