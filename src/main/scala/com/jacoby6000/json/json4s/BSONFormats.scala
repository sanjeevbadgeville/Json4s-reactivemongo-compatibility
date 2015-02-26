package com.jacoby6000.json.json4s

import com.jacoby6000.json.Serializer
import org.json4s.CustomSerializer
import reactivemongo.bson.BSONValue

/**
 * Created by jbarber on 2/26/15.
 */
class BSONFormats extends CustomSerializer[BSONValue](format => (Serializer.deserialize,Serializer.serialize))
