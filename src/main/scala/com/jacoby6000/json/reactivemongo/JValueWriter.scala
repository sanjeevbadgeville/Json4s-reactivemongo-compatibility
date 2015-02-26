package com.jacoby6000.json.reactivemongo

import com.jacoby6000.json.Serializer
import org.json4s._
import reactivemongo.bson.{BSONDocument, BSONDocumentWriter}

/**
 * Created by jbarber on 2/26/15.
 */
class JValueWriter extends BSONDocumentWriter[JValue] {
  def write(t: JValue): BSONDocument = Serializer.deserialize(t).asInstanceOf[BSONDocument]

}
