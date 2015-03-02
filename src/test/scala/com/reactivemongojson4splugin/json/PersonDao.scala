package com.reactivemongojson4splugin.json

/**
 * Created by jbarber on 2/26/15.
 *
 */

import com.reactivemongojson4splugin.reactivemongo.JSONGenericHandlers
import com.reactivemongojson4splugin.reactivemongo.api.JSONReflectionCollection
import org.json4s.JsonAST._
import reactivemongo.api.DefaultDB
import reactivemongo.core.commands.GetLastError

import scala.concurrent.ExecutionContext

class PersonDao(db: DefaultDB)(implicit val ec: ExecutionContext) extends JSONGenericHandlers {
  val collection = db.collection[JSONReflectionCollection]("persons")

  import com.reactivemongojson4splugin.json4s.BSONFormats._

  def get(id: String) = {
    collection.find(JObject("_id" -> JString(id))).one[Person]
  }

  def insert(person: Person) = {
    collection.save(person, GetLastError())
  }
}

