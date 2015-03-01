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

package reactivemongo

import com.jacoby6000.json.json4s.BSONFormats
import org.json4s._
import reactivemongo.bson.{BSONDocumentWriter, BSONDocumentReader, BSONDocument}

trait LowerImplicitBSONHandlers {
  implicit object JValueWriter extends BSONDocumentWriter[JValue] {
    def write(JValue: JValue) = BSONFormats.BSONDocumentFormat.read(JValue)
  }
  implicit object JValueReader extends BSONDocumentReader[JValue] {
    def read(document: BSONDocument): JValue = BSONFormats.BSONDocumentFormat.write(document)
  }
}

trait ImplicitBSONHandlers extends LowerImplicitBSONHandlers {
  implicit val formats = DefaultFormats

  implicit object JObjectWriter extends BSONDocumentWriter[JObject] {
    def write(obj: JObject): BSONDocument =
      BSONFormats.BSONDocumentFormat.read(obj)
  }

  implicit object JObjectReader extends BSONDocumentReader[JObject] {
    def read(document: BSONDocument): JObject =
      BSONFormats.BSONDocumentFormat.write(document).extract[JObject]
  }
}

/**
 * Implicit BSON Handlers (BSONDocumentReader/BSONDocumentWriter for JsObject)
 */
object ImplicitBSONHandlers extends ImplicitBSONHandlers