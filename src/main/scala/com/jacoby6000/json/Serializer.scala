package com.jacoby6000.json

import _root_.reactivemongo.bson.BSONArray
import _root_.reactivemongo.bson.BSONBoolean
import _root_.reactivemongo.bson.BSONDocument
import _root_.reactivemongo.bson.BSONDouble
import _root_.reactivemongo.bson.BSONInteger
import _root_.reactivemongo.bson.BSONLong
import _root_.reactivemongo.bson.BSONNull
import _root_.reactivemongo.bson.BSONObjectID
import _root_.reactivemongo.bson.BSONString
import _root_.reactivemongo.bson.BSONValue
import org.json4s.JsonAST
import org.json4s.JsonAST.JValue
import org.json4s.MappingException
import org.json4s._

import scala.util.Try

/**
 * Created by jbarber on 2/26/15.
 */
object Serializer {

  def deserialize: PartialFunction[JValue, BSONValue] = {
    case obj: JObject =>
      deserializeJObject(obj)
    case arr: JArray =>
      deserializeJArray(arr)

    case int: JInt =>
      deserializeJInt(int)
    case decimal: JDecimal =>
      deserializeJDecimal(decimal)
    case double: JDouble =>
      deserializeJDouble(double)

    case bool: JBool  =>
      deserializeJBool(bool)
    case str: JString =>
      deserializeJString(str)

    case x: JValue  if x == JNothing || x == JNull =>
      BSONNull

  }

  def deserializeJBool(bool: JBool): BSONBoolean = {
    BSONBoolean(bool.value)
  }

  def deserializeJDouble(double: JDouble): BSONDouble = {
    BSONDouble(double.num)
  }

  def deserializeJInt(int: JInt): BSONLong = {
    BSONLong(int.num.toLong)
  }

  def deserializeJDecimal(decimal: JDecimal): BSONDouble = {
    BSONDouble(decimal.num.toDouble)
  }

  def deserializeJString(str: JString): BSONString = {
    BSONString(str.s)
  }

  def deserializeJArray(arr: JArray): BSONArray = {
    BSONArray(arr.productIterator.toStream.map { x =>
      Try(x match {
        case item: JValue =>
          deserialize(item)
        case x: Any =>
          throw new MappingException("Failed to serialize " + x.toString)
      })
    })
  }

  def deserializeJObject(obj: JObject): BSONDocument = {
    BSONDocument(obj.values.map(x => x.copy(
      x._1,
      x._2 match {
        case v1: JValue => deserialize(v1)
        case _ => throw new MappingException("No usable value for " + x._1)
      })))
  }

  def serialize: PartialFunction[Any, JValue] = {
    case BSONDocument(list) =>
      serializeBSONDocument(list)
    case arr: BSONArray =>
      serializeBSONArray(arr)
    case int: BSONInteger =>
      serializeBSONInteger(int)
    case double: BSONDouble =>
      JDouble(double.value)
    case long: BSONLong =>
      JInt(long.value)
    case bool: BSONBoolean =>
      JBool(bool.value)
    case BSONNull =>
      JNull
    case obj: BSONObjectID =>
      JString(obj.toString())
    case str: BSONString =>
      JString(str.value)
  }

  def serializeBSONInteger(int: BSONInteger): JsonAST.JInt = {
    JInt(int.value)
  }

  def serializeBSONArray(arr: BSONArray): JsonAST.JArray = {
    JArray(arr.values.map(serialize).toList)
  }

  def serializeBSONDocument(list: Stream[Try[(String, BSONValue)]]): JsonAST.JObject = {
    JObject.apply(
      list.map(t => t.map(x =>
        JField(x._1, serialize(x._2))
      ).getOrElse(throw new MappingException("Failed to serialize value " + t.get._1))).toList
    )
  }
}