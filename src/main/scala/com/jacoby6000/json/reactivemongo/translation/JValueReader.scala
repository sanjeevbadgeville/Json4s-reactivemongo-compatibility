package com.jacoby6000.json.reactivemongo.translation

import com.jacoby6000.json.Serializer
import org.json4s._
import reactivemongo.bson.{BSONDocument, BSONDocumentReader}

/**
 * Created by jbarber on 2/26/15.
 */
class JValueReader extends BSONDocumentReader[JValue] {
  override def read(bson: BSONDocument): JValue = Serializer.bsonValueToJValue(bson)
}
