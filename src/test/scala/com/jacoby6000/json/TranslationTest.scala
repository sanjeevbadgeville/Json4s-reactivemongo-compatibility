package com.jacoby6000.json

import com.jacoby6000.json.json4s.BSONFormats
import org.scalatest.FlatSpec
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.JsonAST._
import org.json4s.jackson.JsonMethods._

/**
 * Created by jbarber on 2/26/15.
 */
class TranslationTest extends FlatSpec {
  import com.jacoby6000.json.json4s.BSONFormats._


  val testJson =
    """{
      |  "id": "1072d5aa-a1e3-4d47-9434-6932e5d53a9f",
      |  "name": "Billy Bob",
      |  "stuff": [
      |    {
      |      "name": "backpack",
      |      "things": [
      |        {
      |          "name": "Text Book",
      |          "value": 9000
      |        },
      |        {
      |          "name": "Calculator",
      |          "value": 200
      |        }
      |      ]
      |    },
      |    {
      |      "name": "wallet",
      |      "things": [
      |          {
      |            "name": "Credit Card",
      |            "value": 4827
      |          },
      |          {
      |            "name": "Dollar Bill",
      |            "value": 1
      |          },
      |          {
      |            "name": "Passport",
      |            "value": 30
      |          }
      |      ]
      |    }
      |  ],
      |  "job": {
      |    "occupation": "Web Developer",
      |    "position": "Sr. Dev 1, Division 3, Subset 7, Team 42"
      |  }
      |}
    """.stripMargin

  "A Json Payload" should "be recoverable" in {
    val parsed = parse(testJson)
    val asBson = BSONFormats.toBSON(parsed)
    val backToJson = BSONFormats.toJSON(asBson.get)
    println(pretty(render(parsed)))
    println(pretty(render(backToJson)))

    assert(backToJson === parsed)
  }
}
