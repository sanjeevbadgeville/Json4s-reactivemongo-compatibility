/*
 * Copyright 2012-2013 Stephane Godbillon (@sgodbillon)
 * Copyright 2015 Jacob Barber (@Jacoby6000)
 *
 * Changes made by Jacob Barber to adapt the Play-json implementations of the reactivemongo Play Framework plugin to be
 * used with json4s.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.reactivemongojson4splugin.reactivemongo

import org.json4s.JsonAST.JValue
import org.json4s._
import reactivemongo.bson._
import reactivemongo.bson.utils.Converters

/**
 * JSON Formats for BSONValues.
 */
object BSONFormats {
  implicit val formats = DefaultFormats
  implicit val ser = DefaultJsonFormats

  /**
   * I'm not sure why but I'm getting an error unless this is here.
   */
  implicit object JValueWriter extends Writer[JValue] {
    def write(jv: JValue) = jv
  }

  trait PartialFormat[T <: BSONValue] extends JsonFormat[T] {
    def partialReads: PartialFunction[JValue, Option[T]]

    def partialWrites: PartialFunction[BSONValue, JValue]

    def write(t: T): JValue = partialWrites(t)

    def read(json: JValue) = partialReads.lift(json).getOrElse(throw new MappingException("unhandled json value in json: " + json)).get
  }

  implicit object BSONDoubleFormat extends PartialFormat[BSONDouble] {
    val partialReads: PartialFunction[JValue, Option[BSONDouble]] = {
      case JDouble(f) => Some(BSONDouble(f.toDouble))
      case JObject(("$double", JDouble(v)) +: Nil) => Some(BSONDouble(v.toDouble))
    }
    val partialWrites: PartialFunction[BSONValue, JValue] = {
      case double: BSONDouble => JDouble(double.value)
    }
  }

  implicit object BSONStringFormat extends PartialFormat[BSONString] {
    def partialReads: PartialFunction[JValue, Option[BSONString]] = {
      case JString(str) => Some(BSONString(str))
    }

    val partialWrites: PartialFunction[BSONValue, JValue] = {
      case str: BSONString => JString(str.value)
    }
  }

  class BSONDocumentFormat(toBSON: JValue => Option[BSONValue], toJSON: BSONValue => JValue) extends PartialFormat[BSONDocument] {
    def partialReads: PartialFunction[JValue, Option[BSONDocument]] = {
      case obj: JObject =>
        Some(BSONDocument(obj.obj.map { tuple =>
          mapKey(tuple._1) -> mapValue(tuple)
        }))
    }

    def mapValue(tuple: (String, JValue)): BSONValue = {
      toBSON(tuple._2) match {
        case Some(bson) => bson
        case None => throw new scala.RuntimeException("Failed to read bson document key " + tuple._1 + " with value " + tuple._2.toString)
      }
    }

    private def mapKey(key: String) = key match {
      case "id" => "_id"
      case x => x
    }

    private def reverseMapKey(key: String) = key match {
      case "_id" => "id"
      case x => x
    }

    val partialWrites: PartialFunction[BSONValue, JValue] = {
      case doc: BSONDocument => new JObject(doc.elements.map { elem =>
        reverseMapKey(elem._1) -> toJSON(elem._2)
      }.toList)
    }
  }

  implicit object BSONDocumentFormat extends BSONDocumentFormat(toBSON, toJSON)

  class BSONArrayFormat(toBSON: JValue => Option[BSONValue], toJSON: BSONValue => JValue) extends PartialFormat[BSONArray] {
    def partialReads: PartialFunction[JValue, Option[BSONArray]] = {
      case arr: JArray =>
        Some(BSONArray(arr.arr.map { value =>
          toBSON(value) match {
            case Some(bson) => bson
            case None => throw new RuntimeException("Failed to read bson document array value " + value.toString)
          }
        }))
    }

    def partialWrites: PartialFunction[BSONValue, JValue] = {
      case array: BSONArray => {
        JArray(array.values.map { value =>
          toJSON(value)
        }.toList)
      }
    }
  }

  implicit object BSONArrayFormat extends BSONArrayFormat(toBSON, toJSON)

  implicit object BSONObjectIDFormat extends PartialFormat[BSONObjectID] {
    def partialReads: PartialFunction[JValue, Option[BSONObjectID]] = {
      case JObject(("$oid", JString(v)) +: Nil) => Some(BSONObjectID(v))
    }

    val partialWrites: PartialFunction[BSONValue, JValue] = {
      case oid: BSONObjectID => JObject("$oid" -> JString(oid.stringify))
    }
  }

  implicit object BSONBooleanFormat extends PartialFormat[BSONBoolean] {
    def partialReads: PartialFunction[JValue, Option[BSONBoolean]] = {
      case JBool(v) => Some(BSONBoolean(v))
    }

    val partialWrites: PartialFunction[BSONValue, JValue] = {
      case boolean: BSONBoolean => JBool(boolean.value)
    }
  }

  implicit object BSONDateTimeFormat extends PartialFormat[BSONDateTime] {
    def partialReads: PartialFunction[JValue, Option[BSONDateTime]] = {
      case JObject(("$date", JInt(v)) +: Nil) => Some(BSONDateTime(v.toLong))
    }

    val partialWrites: PartialFunction[BSONValue, JValue] = {
      case dt: BSONDateTime => JObject("$date" -> JInt(dt.value))
    }
  }

  implicit object BSONTimestampFormat extends PartialFormat[BSONTimestamp] {
    def partialReads: PartialFunction[JValue, Option[BSONTimestamp]] = {
      case JObject(("$time", JInt(v)) +: Nil) => Some(BSONTimestamp(v.toLong))
    }

    val partialWrites: PartialFunction[BSONValue, JValue] = {
      case ts: BSONTimestamp => JObject("$time" -> JInt(ts.value.toInt), "i" -> JInt(ts.value >>> 4))
    }
  }

  implicit object BSONRegexFormat extends PartialFormat[BSONRegex] {
    def partialReads: PartialFunction[JValue, Option[BSONRegex]] = {
      case js: JObject if js.values.size == 1 && js.values.head._1 == "$regex" =>
        js.values.head._2 match {
          case s: String => Some(BSONRegex(s, ""))
          case x => throw new RuntimeException("String expected for key $regex")
        }
      case js: JObject if js.values.size == 2 && js.values.exists(_._1 == "$regex") && js.values.exists(_._1 == "$options") =>
        val rxOpt = (js \ "$regex").extractOpt[String]
        val optsOpt = (js \ "$options").extractOpt[String]
        (rxOpt, optsOpt) match {
          case (Some(rx), Some(opts)) => Some(BSONRegex(rx, opts))
          case (None, Some(_)) => throw new RuntimeException("String expected for key $regex")
          case (Some(_), None) => throw new RuntimeException("String expected for key $options")
          case _ => throw new RuntimeException("String expected for keys $regex and $options")
        }
    }

    val partialWrites: PartialFunction[BSONValue, JValue] = {
      case rx: BSONRegex =>
        if (rx.flags.isEmpty)
          JObject("$regex" -> JString(rx.value))
        else JObject("$regex" -> JString(rx.value), "$options" -> JString(rx.flags))
    }
  }

  implicit object BSONNullFormat extends PartialFormat[BSONNull.type] {
    def partialReads: PartialFunction[JValue, Option[BSONNull.type]] = {
      case JNull => Some(BSONNull)
    }

    val partialWrites: PartialFunction[BSONValue, JValue] = {
      case BSONNull => JNull
    }
  }

  implicit object BSONUndefinedFormat extends PartialFormat[BSONUndefined.type] {
    def partialReads: PartialFunction[JValue, Option[BSONUndefined.type]] = {
      case JObject(("$undefined", JString(_)) +: Nil) => Some(BSONUndefined)
    }

    val partialWrites: PartialFunction[BSONValue, JValue] = {
      case oid: BSONObjectID => JObject("$undefined" -> JNull)
    }
  }

  implicit object BSONIntegerFormat extends PartialFormat[BSONInteger] {
    def partialReads: PartialFunction[JValue, Option[BSONInteger]] = {
      case JObject(("$int", JInt(i)) +: Nil) => Some(BSONInteger(i.toInt))
      case JInt(i) => Some(BSONInteger(i.toInt))
    }

    val partialWrites: PartialFunction[BSONValue, JValue] = {
      case int: BSONInteger => JInt(int.value)
    }
  }

  implicit object BSONLongFormat extends PartialFormat[BSONLong] {
    def partialReads: PartialFunction[JValue, Option[BSONLong]] = {
      case JObject(("$long", JInt(long)) +: Nil) => Some(BSONLong(long.toLong))
      case JInt(long) => Some(BSONLong(long.toLong))
    }

    val partialWrites: PartialFunction[BSONValue, JValue] = {
      case long: BSONLong => JInt(long.value)
    }
  }

  implicit object BSONBinaryFormat extends PartialFormat[BSONBinary] {
    def partialReads: PartialFunction[JValue, Option[BSONBinary]] = {
      case JString(str) => try {
        Some(BSONBinary(Converters.str2Hex(str), Subtype.UserDefinedSubtype))
      } catch {
        case e: Throwable => throw new RuntimeException(s"error deserializing hex ${e.getMessage}")
      }
      case obj: JObject if obj.obj.exists {
        case (str, _: JString) if str == "$binary" => true
        case _ => false
      } => try {
        Some(BSONBinary(Converters.str2Hex((obj \ "$binary").extract[String]), Subtype.UserDefinedSubtype))
      } catch {
        case e: Throwable => throw new RuntimeException(s"error deserializing hex ${e.getMessage}")
      }
    }

    val partialWrites: PartialFunction[BSONValue, JValue] = {
      case binary: BSONBinary => {
        val remaining = binary.value.readable()
        JObject(
          "$binary" -> JString(Converters.hex2Str(binary.value.slice(remaining).readArray(remaining))),
          "$type" -> JString(Converters.hex2Str(Array(binary.subtype.value.toByte))))
      }
    }
  }

  implicit object BSONSymbolFormat extends PartialFormat[BSONSymbol] {
    def partialReads: PartialFunction[JValue, Option[BSONSymbol]] = {
      case JObject(("$symbol", JString(v)) +: Nil) => Some(BSONSymbol(v))
    }

    val partialWrites: PartialFunction[BSONValue, JValue] = {
      case BSONSymbol(s) => JObject("$symbol" -> JString(s))
    }
  }

  def toBSON(json: JValue): Option[BSONValue] = {
    BSONStringFormat.partialReads.
      orElse(BSONObjectIDFormat.partialReads).
      orElse(BSONDateTimeFormat.partialReads).
      orElse(BSONTimestampFormat.partialReads).
      orElse(BSONBinaryFormat.partialReads).
      orElse(BSONRegexFormat.partialReads).
      orElse(BSONDoubleFormat.partialReads).
      orElse(BSONIntegerFormat.partialReads).
      orElse(BSONLongFormat.partialReads).
      orElse(BSONBooleanFormat.partialReads).
      orElse(BSONNullFormat.partialReads).
      orElse(BSONUndefinedFormat.partialReads).
      orElse(BSONSymbolFormat.partialReads).
      orElse(BSONArrayFormat.partialReads).
      orElse(BSONDocumentFormat.partialReads).
      lift(json).getOrElse(throw new RuntimeException(s"unhandled json value: $json"))
  }

  def toJSON(bson: BSONValue): JValue = BSONObjectIDFormat.partialWrites.
    orElse(BSONDateTimeFormat.partialWrites).
    orElse(BSONTimestampFormat.partialWrites).
    orElse(BSONBinaryFormat.partialWrites).
    orElse(BSONRegexFormat.partialWrites).
    orElse(BSONDoubleFormat.partialWrites).
    orElse(BSONIntegerFormat.partialWrites).
    orElse(BSONLongFormat.partialWrites).
    orElse(BSONBooleanFormat.partialWrites).
    orElse(BSONNullFormat.partialWrites).
    orElse(BSONUndefinedFormat.partialWrites).
    orElse(BSONStringFormat.partialWrites).
    orElse(BSONSymbolFormat.partialWrites).
    orElse(BSONArrayFormat.partialWrites).
    orElse(BSONDocumentFormat.partialWrites).
    lift(bson).getOrElse(throw new RuntimeException(s"unhandled json value: $bson"))
}



