package com.reactivemongojson4splugin.json

import java.util.Date
import com.reactivemongojson4splugin.reactivemongo.BSONFormats
import com.reactivemongojson4splugin.reactivemongo.api.JSONReflectionCollection
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.scalatest.FlatSpec
import reactivemongo.api.{MongoDriver, MongoConnection}
import reactivemongo.bson.BSONObjectID
import reactivemongo.core.commands.LastError

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

/**
 * Created by jbarber on 2/26/15.
 */
class TranslationTest extends FlatSpec {

  import BSONFormats._

  implicit val ec = scala.concurrent.ExecutionContext.global

  val driver = new MongoDriver
  val connection = driver.connection(List("localhost"))
  val db = connection.db("reactivemongojson4splugin")
  val personDao = new PersonDao(db)


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
    assert(backToJson === parsed, pretty(backToJson))
  }

  "A complicated insert" should "pass" in {
    extractAndReadErrors(personDao.insert(parse(testJson).extract[Person]))
  }

  val id = BSONObjectID.generate
  "A simple insert" should "succeed" in {
    extractAndReadErrors(db.collection[JSONReflectionCollection]("simple_collection_test").insert(SimpleObject(id, "test", Some(false), new Date)))
  }

  "A delete" should "succeed" in {
    extractAndReadErrors(db.collection[JSONReflectionCollection]("simple_collection_test").remove(SimpleObject(id, "test", None, new Date)))
  }

  def extractAndReadErrors(f: Future[LastError]): Unit = {
    Await.result(f.collect {
      case e: LastError =>
        e.code match {
          case Some(_) =>
            println(e.code)
            println(e.err)
            println(e.errMsg)
          case None =>
        }
        None
    }, 5 seconds)
  }
}
