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

import org.apache.spark.sql.DataFrame
import org.scalatest.funsuite.AnyFunSuite
import za.co.absa.cobrix.spark.cobol.source.base.SparkTestBase
import za.co.absa.cobrix.spark.cobol.utils.{FileUtils, SparkUtils}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

trait ServerlessParitySupport extends AnyFunSuite with SparkTestBase {

  def assertGolden(df: DataFrame, expectedSchemaPath: String, expectedDataPath: String, actualSchemaPath: String, actualDataPath: String, takeN: Int = 0): Unit = {
    val expectedSchema = Files.readAllLines(Paths.get(expectedSchemaPath), StandardCharsets.ISO_8859_1).toArray.mkString("\n")
    val actualSchema = SparkUtils.prettyJSON(df.schema.json)
      .replace("\"maxElements\" : 80", "[redacted]")
      .replace("\"minElements\" : 0", "[redacted]")
      .replace("\"maxElements\" : 2", "[redacted]")
      .replace("\"maxElements\" : 3", "[redacted]")
    if (actualSchema != expectedSchema) {
      FileUtils.writeStringToFile(actualSchema, actualSchemaPath)
      fail(s"Schema mismatch. Compare $expectedSchemaPath to $actualSchemaPath")
    }
    val expected = Files.readAllLines(Paths.get(expectedDataPath), StandardCharsets.ISO_8859_1).toArray.toSeq.map(_.toString)
    val n = if (takeN <= 0) expected.length else math.min(takeN, expected.length)
    if (takeN <= 0 && df.count() != expected.length) {
      fail(s"Row count mismatch. Expected ${expected.length} rows.")
    }
    val actual = df.toJSON.take(n).toSeq
    if (actual != expected) {
      FileUtils.writeStringsToFile(actual.toArray, actualDataPath)
      fail(s"Data mismatch. Compare $expectedDataPath to $actualDataPath")
    }
  }
}
