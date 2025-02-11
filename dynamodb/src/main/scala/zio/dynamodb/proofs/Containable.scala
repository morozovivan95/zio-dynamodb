package zio.dynamodb.proofs

import zio.dynamodb.ProjectionExpression

import scala.annotation.implicitNotFound

@implicitNotFound("DynamoDB does not support 'contains' on type ${A}. This operator only applies to sets and strings")
sealed trait Containable[X, -A]
trait ContainableLowPriorityImplicits0 extends ContainableLowPriorityImplicits1 {
  implicit def unknownRight[X]: Containable[X, ProjectionExpression.Unknown] =
    new Containable[X, ProjectionExpression.Unknown] {}
}
trait ContainableLowPriorityImplicits1 {
  implicit def set[A]: Containable[Set[A], A]      = new Containable[Set[A], A] {}
  implicit def string: Containable[String, String] = new Containable[String, String] {}
}
object Containable                     extends ContainableLowPriorityImplicits0 {
  implicit def unknownLeft[X]: Containable[ProjectionExpression.Unknown, X] =
    new Containable[ProjectionExpression.Unknown, X] {}
}
