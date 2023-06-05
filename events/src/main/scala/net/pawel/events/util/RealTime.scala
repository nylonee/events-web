package net.pawel.events.util

import java.time.LocalDate

class RealTime extends Time {
  override def today(): LocalDate = LocalDate.now()
}
