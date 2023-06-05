package net.pawel.events.util

import java.time.LocalDate

trait Time {
  def today(): LocalDate
}
