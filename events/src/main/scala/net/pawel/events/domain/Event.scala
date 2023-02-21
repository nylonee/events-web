package net.pawel.events.domain

import java.time.Instant

case class Event(name: String,
                 url: String,
                 start: Instant,
                 end: Instant,
                 address: String,
                 organizerUrl: String)
