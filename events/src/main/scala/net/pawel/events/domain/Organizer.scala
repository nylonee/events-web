package net.pawel.events.domain

import org.mongodb.scala.bson.BsonObjectId

object OrganizerType extends Enumeration {
  type OrganizerType = Value
  val TicketTailor, EventBrite, Facebook = Value
}

case class Organizer(url: String,
                     name: String,
                     organizerType: OrganizerType.OrganizerType,
                     _id: BsonObjectId = BsonObjectId())