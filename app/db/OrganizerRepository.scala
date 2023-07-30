package db

import ai.snips.bsonmacros.{CodecGen, DatabaseContext}
import net.pawel.events.domain.Organizer
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.Document
import org.mongodb.scala.result.DeleteResult

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OrganizerRepository @Inject()(context: DatabaseContext) extends Dao[Organizer] {
  CodecGen[Organizer](context.codecRegistry)

  val db = context.database("events")

  override val collection: MongoCollection[Organizer] = db.getCollection[Organizer]("organizers")

  def upsert(organizer: Organizer): Future[_] = upsertOne(organizer, Document("url" -> organizer.url))

  def deleteAll(): Future[DeleteResult] = collection.deleteMany(Document()).toFuture()
}
