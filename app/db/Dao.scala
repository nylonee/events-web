package db

import ai.snips.bsonmacros.BaseDAO
import org.mongodb.scala.bson.Document
import org.mongodb.scala.model.ReplaceOptions

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

abstract class Dao[DO](implicit ct: ClassTag[DO],
                       ec: ExecutionContext) extends BaseDAO[DO]()(ct, ec) {

  def upsertOne(it: DO, query: Document): Future[_] =
    collection.replaceOne(query, it, ReplaceOptions().upsert(true)).toFuture()

  def replaceOne(it: DO, query: Document): Future[_] = collection.replaceOne(query, it).toFuture()

  def deleteOne(query: Document): Future[_] = collection.deleteOne(query).toFuture()


}
