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

package za.co.absa.cobrix.spark.cobol.serverless.spike

import org.apache.spark.sql.{DataFrame, SparkSession}

object TableValidationMain {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder().appName("CobrixTableValidation").getOrCreate()
    val tableName = requiredArg(args, "--table")
    val actual = spark.table(tableName)
    val actualCount = actual.count()

    argValue(args, "--expected-count").foreach { value =>
      val expectedCount = value.toLong
      require(actualCount == expectedCount, s"Expected $expectedCount rows in $tableName, found $actualCount")
    }

    argValue(args, "--expected-json").foreach { path =>
      val expected = spark.read.schema(actual.schema).json(path)
      val goldenCount = expected.count()
      // Cobrix goldens for test17/test4 are take(N) samples, not the full file.
      val forCompare =
        if (actualCount > goldenCount) {
          val ordered =
            if (actual.columns.contains("File_Id") && actual.columns.contains("Record_Id")) {
              actual.orderBy("File_Id", "Record_Id")
            } else {
              actual
            }
          ordered.limit(goldenCount.toInt)
        } else {
          actual
        }
      assertEquivalent(forCompare, expected, s"$tableName and golden JSON $path")
    }

    argValue(args, "--compare-table").foreach { otherTable =>
      val other = spark.table(otherTable)
      require(actual.schema == other.schema, s"Schema mismatch between $tableName and $otherTable")
      assertEquivalent(actual, other, s"$tableName and $otherTable")
    }

    println(s"VALIDATED_TABLE=$tableName")
    println(s"VALIDATED_ROW_COUNT=$actualCount")
  }

  private def assertEquivalent(left: DataFrame, right: DataFrame, description: String): Unit = {
    val leftOnly = left.exceptAll(right).limit(1).count()
    val rightOnly = right.exceptAll(left).limit(1).count()
    require(leftOnly == 0L && rightOnly == 0L, s"Data mismatch between $description")
  }

  private def requiredArg(args: Array[String], key: String): String =
    argValue(args, key).getOrElse(throw new IllegalArgumentException(s"Missing $key <value>"))

  private def argValue(args: Array[String], key: String): Option[String] = {
    val index = args.indexOf(key)
    if (index >= 0 && index + 1 < args.length) Some(args(index + 1)) else None
  }
}
