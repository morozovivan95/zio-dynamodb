package zio.dynamodb

import zio.Ref
import zio.dynamodb.DynamoDBQuery.put
import zio.schema.{ DeriveSchema, Schema }
import zio.stream.ZStream
import zio.test.Assertion.{ equalTo, isLeft }
import zio.test.{ assert, ZIOSpecDefault }

object ZStreamPipeliningSpec extends ZIOSpecDefault {
  final case class Person(id: Int, name: String)
  object Person {
    implicit val schema: Schema[Person] = DeriveSchema.gen[Person]
  }

  private val people       = (1 to 200).map(i => Person(i, s"name$i")).toList
  private val personStream = ZStream.fromIterable(people)

  override def spec =
    suite("ZStream piplelining suite")(
      test("round trip test") {
        for {
          _           <- TestDynamoDBExecutor.addTable("person", "id")
          _           <- batchWriteFromStream(personStream) { person =>
                           put("person", person)
                         }.runDrain
          refPeople   <- Ref.make(List.empty[Person])
          _           <- batchReadFromStream[Any, Person, Person]("person", personStream)(person => PrimaryKey("id" -> person.id))
                           .mapZIO(pair => refPeople.update(xs => xs :+ pair._2))
                           .runDrain
          foundPeople <- refPeople.get
        } yield assert(foundPeople)(equalTo(people))
      },
      test("lifts de-serialisation errors to ZStream error channel") {
        for {
          _           <- TestDynamoDBExecutor.addTable(
                           "person",
                           "id",
                           PrimaryKey("id" -> 1) -> Item("id" -> 1, "name" -> "Avi"),
                           PrimaryKey("id" -> 2) -> Item("id" -> 2, "boom!" -> "de-serialisation-error-expected")
                         )
          refPeople   <- Ref.make(List.empty[Person])
          either      <- batchReadFromStream[Any, Person, Person]("person", personStream.take(2))(person =>
                           PrimaryKey("id" -> person.id)
                         )
                           .mapZIO(pair => refPeople.update(xs => xs :+ pair._2))
                           .runDrain
                           .either
          foundPeople <- refPeople.get
        } yield assert(either)(isLeft) && assert(foundPeople)(equalTo(List(Person(1, "Avi"))))
      }
    ).provide(DynamoDBExecutor.test)
}
