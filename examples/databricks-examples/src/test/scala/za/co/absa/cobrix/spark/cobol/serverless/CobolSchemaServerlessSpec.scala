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

import org.scalatest.funsuite.AnyFunSuite
import za.co.absa.cobrix.spark.cobol.schema.CobolSchema
import za.co.absa.cobrix.spark.cobol.utils.SparkUtils

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

class CobolSchemaServerlessSpec extends AnyFunSuite {

  test("schema matches test1 golden file") {
    val copybook = Files.readAllLines(Paths.get("../../data/test1_copybook.cob"), StandardCharsets.ISO_8859_1).toArray.mkString("\n")
    val schema = CobolSchema.fromSparkOptions(Seq(copybook), Map("schema_retention_policy" -> "collapse_root")).getSparkSchema
    val expected = Files.readAllLines(Paths.get("../../data/test1_expected/test1_schema.json"), StandardCharsets.ISO_8859_1).toArray.mkString("\n")
    val actual = SparkUtils.prettyJSON(schema.json)
      .replace("\"maxElements\" : 80", "[redacted]")
      .replace("\"minElements\" : 0", "[redacted]")
    assert(actual == expected)
  }
}
