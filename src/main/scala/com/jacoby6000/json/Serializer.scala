package com.jacoby6000.json

import _root_.reactivemongo.api.{DefaultDB, MongoConnection}
import _root_.reactivemongo.bson._
import org.json4s._
import org.json4s.JsonAST.JValue
import scala.util.Try

/**
 * Created by jbarber on 2/26/15.
 */
object Serializer {

  def jValueToBSONValue: PartialFunction[JValue, BSONValue] = {
    case obj: JObject =>
      jObjectToBSONValue(obj)
    case arr: JArray =>
      jArrayToBSONValue(arr)

    case int: JInt =>
      jIntToBSONValue(int)
    case decimal: JDecimal =>
      jDecimalToBSONValue(decimal)
    case double: JDouble =>
      jDoubleToBSONValue(double)

    case bool: JBool  =>
      jBoolToBSONValue(bool)
    case str: JString =>
      jStringToBSONValue(str)
    case JNothing =>
      BSONNull
    case JNull =>
      BSONNull

  }

  def jBoolToBSONValue(bool: JBool): BSONBoolean = {
    BSONBoolean(bool.value)
  }

  def jDoubleToBSONValue(double: JDouble): BSONDouble = {
    BSONDouble(double.num)
  }

  def jIntToBSONValue(int: JInt): BSONLong = {
    BSONLong(int.num.toLong)
  }

  def jDecimalToBSONValue(decimal: JDecimal): BSONDouble = {
    BSONDouble(decimal.num.toDouble)
  }

  def jStringToBSONValue(str: JString): BSONString = {
    BSONString(str.s)
  }

  def jArrayToBSONValue(arr: JArray): BSONArray = {
    BSONArray(arr.productIterator.toStream.map { x =>
      Try(x match {
        case item: JValue =>
          jValueToBSONValue(item)
        case x: Any =>
          throw new MappingException("Failed to serialize " + x.toString)
      })
    })
  }

  def jObjectToBSONValue(obj: JObject): BSONDocument = {
    BSONDocument(obj.values.map(x => x.copy(
      x._1,
      x._2 match {
        case v1: JValue => jValueToBSONValue(v1)
        case _ => throw new MappingException("No usable value for " + x._1)
      })))
  }

  def bsonValueToJValue: PartialFunction[Any, JValue] = {
    case doc: BSONDocument =>
      bsonDocumentToJValue(doc)
    case arr: BSONArray =>
      bsonArrayToJValue(arr)
    case int: BSONInteger =>
      bsonIntegerToJValue(int)
    case double: BSONDouble =>
      bsonDoubleToJValue(double)
    case long: BSONLong =>
      bsonLongToJValue(long)
    case bool: BSONBoolean =>
      bsonBooleanToJValue(bool)
    case BSONNull =>
      JNull
    case obj: BSONObjectID =>
      bsonObjectIdToJValue(obj)
    case str: BSONString =>
      bsonStringToJValue(str)
  }

  def bsonStringToJValue(str: BSONString): JsonAST.JString = {
    JString(str.value)
  }

  def bsonObjectIdToJValue(obj: BSONObjectID): JsonAST.JString = {
    JString(obj.toString())
  }

  def bsonBooleanToJValue(bool: BSONBoolean): JsonAST.JBool = {
    JBool(bool.value)
  }

  def bsonDoubleToJValue(double: BSONDouble): JsonAST.JDouble = {
    JDouble(double.value)
  }

  def bsonLongToJValue(long: BSONLong): JsonAST.JInt = {
    JInt(long.value)
  }

  def bsonIntegerToJValue(int: BSONInteger): JInt = {
    JInt(int.value)
  }

  def bsonArrayToJValue(arr: BSONArray): JArray = {
    JArray(arr.values.map(bsonValueToJValue).toList)
  }

  def bsonDocumentToJValue(doc: BSONDocument): JObject = {
    JObject.apply(
      doc.stream.map(t => t.map(x =>
        JField(x._1, bsonValueToJValue(x._2))
      ).getOrElse(throw new MappingException("Failed to serialize value " + t.get._1))).toList
    )
  }
}