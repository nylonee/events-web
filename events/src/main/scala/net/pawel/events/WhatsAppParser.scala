package net.pawel.events

import java.time.LocalDateTime
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.ChronoField
import scala.util.parsing.combinator.RegexParsers

case class Message(date: LocalDateTime, text: String)

class WhatsAppParser extends RegexParsers {
  val dateFormatter = new DateTimeFormatterBuilder()
    .appendValue(ChronoField.DAY_OF_MONTH, 2)
    .appendLiteral("/")
    .appendValue(ChronoField.MONTH_OF_YEAR, 2)
    .appendLiteral("/")
    .appendValue(ChronoField.YEAR, 4)
    .toFormatter()

  val dateTimeFormatter = new DateTimeFormatterBuilder()
    .appendLiteral("[")
    .append(dateFormatter)
    .appendLiteral(", ")
    .append(DateTimeFormatter.ISO_LOCAL_TIME)
    .appendLiteral("]")
    .toFormatter()

  def time: Parser[LocalDateTime] =
    """[\d{2}/\d{2}/\d{4}, \d{2}:\d{2}:\d{2}]""".r ^^ {
      string => LocalDateTime.parse(string, dateTimeFormatter)
    }

  def text: Parser[String] = """.+""".r ^^ {
    identity
  }

  def message: Parser[Message] = time ~ text ^^ { case time ~ text => Message(time, text) }

  def messages: Parser[List[Message]] = message.*

  def parse(string: String): List[Message] = parse(messages, string) match {
    case Success(matched, _) => matched
    case Failure(msg, _) => throw new RuntimeException("FAILURE: " + msg)
    case Error(msg, _) => throw new RuntimeException("ERROR: " + msg)
  }
}
