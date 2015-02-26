package com.jacoby6000.json.reactivemongo.translation

import com.jacoby6000.json.Serializer
import org.json4s._
import reactivemongo.bson.{BSONDocument, BSONDocumentWriter}

/**
 * Created by jbarber on 2/26/15.
 */
object JValueWriter extends BSONDocumentWriter[JValue] {
  def write(t: JValue): BSONDocument = Serializer.jValueToBSONValue(t).asInstanceOf[BSONDocument]
}


