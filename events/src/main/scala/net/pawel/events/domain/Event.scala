package net.pawel.events.domain

import org.mongodb.scala.bson.BsonObjectId

import java.time.LocalDateTime

case class Event(name: String,
                 url: String,
                 start: LocalDateTime,
                 end: LocalDateTime,
                 address: String,
                 _id: BsonObjectId = BsonObjectId())
