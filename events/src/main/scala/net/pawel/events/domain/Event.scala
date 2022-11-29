package net.pawel.events.domain

import org.mongodb.scala.bson.BsonObjectId

import java.time.Instant

case class Event(name: String,
                 url: String,
                 start: Instant,
                 end: Instant,
                 address: String,
                 organizerUrl: String,
                 _id: BsonObjectId = BsonObjectId())
