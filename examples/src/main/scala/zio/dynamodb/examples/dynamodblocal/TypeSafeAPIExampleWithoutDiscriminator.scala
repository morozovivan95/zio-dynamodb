package zio.dynamodb.examples.dynamodblocal

import zio.dynamodb.examples.dynamodblocal.DynamoDB._
import zio.dynamodb.DynamoDBQuery._
import zio.dynamodb._
import zio.dynamodb.examples.dynamodblocal.TypeSafeAPIExampleWithoutDiscriminator.TrafficLight.{ Amber, Box, Green }
import zio.schema.DeriveSchema
import zio.{ Console, ZIOAppDefault }

object TypeSafeAPIExampleWithoutDiscriminator extends ZIOAppDefault {

  sealed trait TrafficLight
  object TrafficLight {
    final case class Green(rgb: Int) extends TrafficLight
    object Green {
      implicit val schema = DeriveSchema.gen[Green]
      val rgb             = ProjectionExpression.accessors[Green]
    }
    final case class Red(rgb: Int) extends TrafficLight
    object Red   {
      implicit val schema = DeriveSchema.gen[Red]
      val rgb             = ProjectionExpression.accessors[Red]
    }
    final case class Amber(rgb: Int) extends TrafficLight
    object Amber {
      implicit val schema = DeriveSchema.gen[Amber]
      val rgb             = ProjectionExpression.accessors[Amber]
    }
    final case class Box(id: Int, code: Int, trafficLightColour: TrafficLight)
    object Box   {
      implicit val schema                = DeriveSchema.gen[Box]
      val (id, code, trafficLightColour) = ProjectionExpression.accessors[Box]
    }

    implicit val schema = DeriveSchema.gen[TrafficLight]

    val (green, red, amber) = ProjectionExpression.accessors[TrafficLight]
  }

  private val program = for {
    _         <- createTable("box", KeySchema("id", "code"), BillingMode.PayPerRequest)(
                   AttributeDefinition.attrDefnNumber("id"),
                   AttributeDefinition.attrDefnNumber("code")
                 ).execute
    boxOfGreen = Box(1, 1, Green(1))
    boxOfAmber = Box(1, 2, Amber(1))
    _         <- put[Box]("box", boxOfGreen).execute
    _         <- put[Box]("box", boxOfAmber).execute
    query      = queryAll[Box]("box")
                   .whereKey(Box.id === 1)
                   .filter(Box.trafficLightColour >>> TrafficLight.green >>> Green.rgb === 1)
    stream    <- query.execute
    list      <- stream.runCollect
    _         <- Console.printLine(s"boxes=$list")
  } yield ()

  override def run = program.provide(layer)
}
