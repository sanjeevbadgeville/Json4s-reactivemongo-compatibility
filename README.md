# Json4s-reactivemongo-compatibility
An attempt to make json4s and reactivemongo fully interoperable.

##### How is this helpful?
Suppose we are the NSA keeping a close watch on people, and we have the following data model in json:
```
{
  "id": "1072d5aa-a1e3-4d47-9434-6932e5d53a9f",
  "name": "Billy Bob",
  "stuff": [
    {
      "name": "backpack",
      "things": [
        {
          "name": "Text Book",
          "value": 9000
        },
        {
          "name": "Calculator",
          "value": 200
        }
      ]
    },
    {
      "name": "wallet",
      "things": [
          {
            "name": "Credit Card",
            "value": 4827
          },
          {
            "name": "Dollar Bill",
            "value": 1
          },
          {
            "name": "Passport",
            "value": 30
          }
      ]
    }
  ],
  "job": {
    "occupation": "Web Developer",
    "position": "Sr. Dev 1, Division 3, Subset 7, Team 42"
  }
}
```

Mapped in to scala case classes:
``` scala
case class Person(id: String, name: String, stuff: List[Container], job: Option[Occupation])
case class Container(name: String, things: List[KVPair])
case class Occupation(occupation: String = "ISIS", position: String) //Always assume the worst when being the NSA.
case class KVPair(name: String, value: Int)
```

With reactive mongo's BSON implementation, you must create bsonDocument readers and writers... A datastructure like this may have these classes:
``` scala
object PersonDocumentTranslator extends BSONDocumentReader[Person] with BSONDocumentWriter[Person] {
  implicit val barTranslator = ContainerDocumentTranslator
  implicit val fooBarTranslator = OccupationDocumentTranslator

  def read(bson: BSONDocument): Person = {
    Person(
      bson.getAs[String]("id").get,
      bson.getAs[String]("name").get,
      bson.getAs[List[Container]]("stuff").get,
      bson.getAs[Occupation]("some_option")
    )
  }

  def write(foo: Person): BSONDocument = {
    BSONDocument(
      "id" -> foo.id,
      "name" -> foo.name,
      "stuff" -> foo.stuff,
      "job" -> foo.job
    )
  }
}


object ContainerDocumentTranslator extends BSONDocumentReader[Container] with BSONDocumentWriter[Container] {
  implicit val kvPairTranslator = KVPairDocumentTranslator

  def read(bson: BSONDocument): Container = {
    Container(
      bson.getAs[String]("name").get,
      bson.getAs[List[KVPair]]("map").get
    )
  }

  def write(bar: Container): BSONDocument = {
    BSONDocument(
      "name" -> bar.name,
      "things" -> bar.things
    )
  }
}

object OccupationDocumentTranslator extends BSONDocumentReader[Occupation] with BSONDocumentWriter[Occupation] {
  def read(bson: BSONDocument): Occupation = {
    Occupation(
      bson.getAs[String]("occupation").get,
      bson.getAs[String]("position").get
    )
  }

  def write(fooBar: Occupation): BSONDocument = {
    BSONDocument(
      "occupation" -> fooBar.occupation,
      "position" -> fooBar.position
    )
  }
}

object KVPairDocumentTranslator extends BSONDocumentReader[KVPair] with BSONDocumentWriter[KVPair] {
  def read(bson: BSONDocument) = {
    KVPair(
      bson.getAs[String]("name").get,
      bson.getAs[Int]("value").get
    )
  }

  def write(kv: KVPair) = {
    BSONDocument(
      "name" -> kv.name,
      "value" -> kv.value
    )
  }
}
```

Then a dao may look like
``` scala
class PersonDao(db: DefaultDB)(implicit val ec: ExecutionContext) {
  val collection = db.collection[BSONCollection]("persons")
  implicit val translator = PersonDocumentTranslator

  def get(id: String) = {
    collection.find(BSONDocument("_id" -> id)).one[Person]
  }
  
  def insert(person: Person) = {
    collection.insert(person)
  }
}
```

Quite a bit of code there... Not too bad if your data model will never change, and if you didn't make any typos in your document writers. In a static enviornment, this is perfectly fine to do once for each class used to model the persistance layer. 

Lets compare that to a json4s implementation. We'll use the same json payload and case classes as above.

``` scala
import com.jacoby6000.json.json4s.BSONFormats
import com.jacoby6000.json.reactivemongo.translation.{JValueReader, JValueWriter}
class PersonDao(db: DefaultDB)(implicit val ec: ExecutionContext) {
  val collection = db.collection[BSONCollection]("persons")
  implicit val json4sFormats = DefaultFormats + new BSONFormats
  implicit val jValueReader = new JValueReader
  implicit val jValueWriter = new JValueWriter

  def get(id: String) = {
    collection.find(BSONDocument("_id" -> id)).one[JValue].map(_.map(_.camelizeKeys.extract[Person])) // You should probably extract the person differently. This will silently fail if there's an error during deserialization.
  }
  
  def insert(person: Person) = {
    collection.insert(Extraction.decompose(person))
  }
}
```

That's it! Drop the readers and the writers and this is all you need:
``` scala
import com.jacoby6000.json.json4s.BSONFormats
import com.jacoby6000.json.reactivemongo.translation.{JValueReader, JValueWriter}

case class Person(id: String, name: String, stuff: List[Container], job: Option[Occupation])
case class Container(name: String, things: List[KVPair])
case class Occupation(occupation: String = "ISIS", position: String)
case class KVPair(name: String, value: Int)

class PersonDao(db: DefaultDB)(implicit val ec: ExecutionContext) {
  val collection = db.collection[BSONCollection]("persons")
  implicit val json4sFormats = DefaultFormats + new BSONFormats
  implicit val jValueReader = new JValueReader
  implicit val jValueWriter = new JValueWriter

  def get(id: String) = {
    collection.find(BSONDocument("_id" -> id)).one[JValue].map(_.map(_.camelizeKeys.extract[Person]))
  }

  def insert(person: Person) = {
    collection.insert(Extraction.decompose(person))
  }
}
```

##### Some things to clear up
This does not perform as well as document readers and writers, especially as currently implemented. Eventually it will be faster than it is now, but readers and writers will probably always be faster.

The ideal time to use this is when developing in a rapidly changing domain, or during development. After development has mostly finished and the data-model has solidified, I would reccomend creating readers and writers, unless you don't need super fast performance.

This will not automatically serialize normal classes. See the json4s documentation for more details. Json4s requires custom serializers, similar to document readers and writers, in order to serialize classes.

Currently BSONObjectIds are serialized straight in to strings, which means they'll be stored in mongo as strings. I'm working on a way around this currently.  I don't know enough about how json4s works yet to do it.

##### TODO
1. Improve BSONObjectId support with JValues.
2. Figure out some way to leverage json4s to not have to create BSONDocumentReaders/Writers.
3. Get reactive mongo to pull queries in to JValues instead of going BSONCollection -> JValue -> Object.
4. Tests

##### Currently supported:
JValue to BSONValue
```
JObject  -> BSONDocument
JArray   -> BSONArray
JBool    -> BSONBoolean
JString  -> BSONString
JDecimal -> BSONDouble (BSON has no type for decimal, i think. submit a pr or create an issue if I'm wrong.)
JDouble  -> BSONDouble
JInt     -> BSONLong (this is because json4s uses long to store JInt)
JNothing -> BSONNull (There is no BSONNothing. If there is a better way to go about this, make a pr/issue)
JNull    -> BSONNull
```

BSONValue to JValue
```
BSONDocument -> JObject
BSONArray    -> JArray
BSONBoolean  -> JBool
BSONString   -> JString
BSONDouble   -> JDouble
BSONLong     -> JInt (JInt is a wrapped long, so yeah.)
BSONInteger  -> JInt
BSONNull     -> JNull
BSONObjectId -> JString (Will probably make a JObjectId at some point... not sure yet).
```
