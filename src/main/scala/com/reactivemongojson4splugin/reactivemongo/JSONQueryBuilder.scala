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

import org.json4s._
import reactivemongo.api._

/**
 * Created by jbarber on 2/28/15.
 */
case class JSONQueryBuilder(
                             collection: Collection,
                             failover: FailoverStrategy,
                             queryOption: Option[JObject] = None,
                             sortOption: Option[JObject] = None,
                             projectionOption: Option[JObject] = None,
                             hintOption: Option[JObject] = None,
                             explainFlag: Boolean = false,
                             snapshotFlag: Boolean = false,
                             commentString: Option[String] = None,
                             options: QueryOpts = QueryOpts()) extends JSONQueryBuilderLike {

  implicit val formats = DefaultFormats
  implicit val bsonHandlers = ImplicitBSONHandlers

  type Self = JSONQueryBuilder

  def copy(queryOption: Option[JObject], sortOption: Option[JObject], projectionOption: Option[JObject], hintOption: Option[JObject], explainFlag: Boolean, snapshotFlag: Boolean, commentString: Option[String], options: QueryOpts, failover: FailoverStrategy): JSONQueryBuilder =
    JSONQueryBuilder(collection, failover, queryOption, sortOption, projectionOption, hintOption, explainFlag, snapshotFlag, commentString, options)

}
