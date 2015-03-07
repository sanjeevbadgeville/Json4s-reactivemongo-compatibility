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

import com.reactivemongojson4splugin.reactivemongo.api.JSONReflectionCollection
import org.json4s._
import reactivemongo.api.collections.GenericCollectionProducer
import reactivemongo.api.{DB, FailoverStrategy}

/**
 * A Collection that interacts with the Json4s library, using `Reader` and `Writer`.
 */
object `package` {

  implicit object JSONCollectionProducer extends GenericCollectionProducer[JValue, Reader, Writer, JSONReflectionCollection] {
    def apply(db: DB, name: String, failoverStrategy: FailoverStrategy) = new JSONReflectionCollection(db, name, failoverStrategy)
  }

}
