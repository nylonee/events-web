package db

import ai.snips.bsonmacros.{CodecGen, DatabaseContext}
import net.pawel.events.domain.Event
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.{BsonDateTime, Document}

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EventRepository @Inject()(context: DatabaseContext) extends Dao[Event] {
  CodecGen[Event](context.codecRegistry)

  val db = context.database("events")

  override val collection: MongoCollection[Event] = db.getCollection[Event]("events")

  def upsert(event: Event): Future[_] = upsertOne(event, Document("url" -> event.url, "start" -> BsonDateTime(event.start.toEpochMilli)))
  def replace(event: Event): Future[_] = replaceOne(event, Document("url" -> event.url, "start" -> BsonDateTime(event.start.toEpochMilli)))
  def delete(event: Event): Future[_] = deleteOne(Document("url" -> event.url, "start" -> BsonDateTime(event.start.toEpochMilli)))
}
