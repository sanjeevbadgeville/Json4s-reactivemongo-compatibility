package com.jacoby6000.json

import _root_.reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter, BSONDocument}

/**
 * Created by jbarber on 2/26/15.
 */

trait BSONDocumentTranslator[T] extends BSONDocumentReader[T] with BSONDocumentWriter[T]

object PersonDocumentTranslator extends BSONDocumentTranslator[Person] {
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


object ContainerDocumentTranslator extends BSONDocumentTranslator[Container] {
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

object OccupationDocumentTranslator extends BSONDocumentTranslator[Occupation] {
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

object KVPairDocumentTranslator extends BSONDocumentTranslator[KVPair] {
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

