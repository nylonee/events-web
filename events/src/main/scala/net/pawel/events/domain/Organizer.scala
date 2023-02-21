package net.pawel.events.domain

object OrganizerType extends Enumeration {
  type OrganizerType = Value
  val TicketTailor, EventBrite, Dandelion = Value
}

case class Organizer(url: String,
                     name: String,
                     organizerType: OrganizerType.OrganizerType)