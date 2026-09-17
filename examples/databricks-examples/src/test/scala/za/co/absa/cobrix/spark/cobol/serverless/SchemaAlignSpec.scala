/*
 * Copyright 2018 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.cobrix.spark.cobol.serverless

import org.apache.spark.sql.types._
import org.scalatest.funsuite.AnyFunSuite

class SchemaAlignSpec extends AnyFunSuite {
  test("keeps File_Id non-nullable when the dataset already is") {
    val wanted = StructType(Seq(StructField("File_Id", IntegerType, nullable = false)))
    val actual = StructType(Seq(StructField("File_Id", IntegerType, nullable = false)))
    assert(!SchemaAlign.forDataset(wanted, actual).fields.head.nullable)
  }

  test("does not tighten File_Id to non-nullable when Spark widened it") {
    val meta = new MetadataBuilder().putLong("maxLength", 2L).build()
    val wanted = StructType(Seq(StructField("File_Id", IntegerType, nullable = false, meta)))
    val actual = StructType(Seq(StructField("File_Id", IntegerType, nullable = true)))
    val aligned = SchemaAlign.forDataset(wanted, actual).fields.head
    assert(aligned.nullable)
    assert(aligned.metadata.getLong("maxLength") == 2L)
  }

  test("aligns nested struct nullability") {
    val wanted = StructType(Seq(
      StructField("COMPANY", StructType(Seq(StructField("NAME", StringType, nullable = false))), nullable = false)
    ))
    val actual = StructType(Seq(
      StructField("COMPANY", StructType(Seq(StructField("NAME", StringType, nullable = true))), nullable = true)
    ))
    val aligned = SchemaAlign.forDataset(wanted, actual)
    assert(aligned.fields.head.nullable)
    assert(aligned.fields.head.dataType.asInstanceOf[StructType].fields.head.nullable)
  }
}
