package com.jacoby6000.json

/**
 * Created by jbarber on 2/26/15.
 *
 */

case class Person(id: String, name: String, stuff: List[Container], job: Option[Occupation])
case class Container(name: String, things: List[KVPair])
case class Occupation(occupation: String = "ISIS", position: String) //Always assume the worst when being the NSA.
case class KVPair(name: String, value: Int)