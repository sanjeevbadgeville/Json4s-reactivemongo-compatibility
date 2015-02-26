# Json4s-reactivemongo-compatibility
An attempt to make json4s and reactivemongo fully interoperable.

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




##### TODO
1. Improve BSONObjectId support with JValues.
2. Figure out some way to leverage json4s to not have to create BSONDocumentReaders/Writers.
