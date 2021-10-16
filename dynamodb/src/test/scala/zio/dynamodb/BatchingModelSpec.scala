package zio.dynamodb

import zio.Chunk
import zio.dynamodb.DynamoDBQuery.BatchGetItem.TableGet
import zio.dynamodb.DynamoDBQuery.{ BatchGetItem, BatchWriteItem }
import zio.test.Assertion._
import zio.test.{ DefaultRunnableSpec, _ }

object BatchingModelSpec extends DefaultRunnableSpec with DynamoDBFixtures {

  override def spec: ZSpec[Environment, Failure] = suite("Batch Model")(batchGetItemSuite, batchWriteItemSuite)

  private val batchGetItemSuite = suite("BatchGetItem")(
    test("should aggregate GetItems using +") {
      val batch = BatchGetItem().addAll(getItemT1, getItemT1_2)

      assert(batch.addList)(equalTo(Chunk(getItemT1, getItemT1_2))) &&
      assert(batch.requestItems)(
        equalTo(
          MapOfSet.empty.addAll(
            tableName1 -> TableGet(getItemT1.key, getItemT1.projections),
            tableName1 -> TableGet(getItemT1_2.key, getItemT1_2.projections)
          )
        )
      )
    },
    test("with aggregated GetItem's should return Some values back when keys are found") {
      val batch    = BatchGetItem().addAll(getItemT1, getItemT1_2)
      val response =
        BatchGetItem.Response(MapOfSet.empty.addAll(tableName1 -> itemT1, tableName1 -> itemT1_2))

      assert(batch.toGetItemResponses(response))(equalTo(Chunk(Some(itemT1), Some(itemT1_2))))
    },
    test("with aggregated GetItem's should return None back for keys that are not found") {
      val batch    = BatchGetItem().addAll(getItemT1, getItemT1_2)
      val response =
        BatchGetItem.Response(
          MapOfSet.empty.addAll(tableName1 -> itemT1_NotExists, tableName1 -> itemT1_2)
        )

      assert(batch.toGetItemResponses(response))(equalTo(Chunk(None, Some(itemT1_2))))
    }
  )

  private val batchWriteItemSuite = suite("BatchWriteItem")(
    test("should aggregate a PutItem and then a DeleteItem for the same table using +") {
      val batch: BatchWriteItem = BatchWriteItem().addAll(putItemT1, deleteItemT1)

      assert(batch.addList)(
        equalTo(Chunk(BatchWriteItem.Put(putItemT1.item), BatchWriteItem.Delete(deleteItemT1.key)))
      ) &&
      assert(batch.requestItems)(
        equalTo(
          MapOfSet
            .empty[TableName, BatchWriteItem.Write]
            .addAll(
              tableName1 -> BatchWriteItem.Put(putItemT1.item),
              tableName1 -> BatchWriteItem.Delete(deleteItemT1.key)
            )
        )
      )
    }
  )

}
