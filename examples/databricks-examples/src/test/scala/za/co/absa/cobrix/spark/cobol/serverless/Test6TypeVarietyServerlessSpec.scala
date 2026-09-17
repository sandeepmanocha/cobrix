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

import za.co.absa.cobrix.spark.cobol.utils.{FileUtils, SparkUtils}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

class Test6TypeVarietyServerlessSpec extends ServerlessParitySupport {
  test("type variety test6") {
    val df = CobolServerlessReader.read(
      spark,
      "../../data/test6_data",
      Map(
        "copybook" -> "file://../../data/test6_copybook.cob",
        "schema_retention_policy" -> "collapse_root",
        "floating_point_format" -> "IEEE754",
        "strict_sign_overpunching" -> "true",
        "pedantic" -> "true"
      )
    )
    val actualDf = df.orderBy("ID").na.fill(0)
    val expectedSchema = Files.readAllLines(Paths.get("../../data/test6_expected/test6_schema.json"), StandardCharsets.ISO_8859_1).toArray.mkString("\n")
    val actualSchema = SparkUtils.prettyJSON(df.schema.json)
    if (actualSchema != expectedSchema) {
      FileUtils.writeStringToFile(actualSchema, "../../data/test6_expected/test6_schema_serverless_actual.json")
      fail("Schema mismatch. Compare ../../data/test6_expected/test6_schema.json to ../../data/test6_expected/test6_schema_serverless_actual.json")
    }
    val actual = actualDf.toJSON.take(100)
    val expected = Files.readAllLines(Paths.get("../../data/test6_expected/test6.txt"), StandardCharsets.ISO_8859_1).toArray
    if (!actual.sameElements(expected)) {
      FileUtils.writeStringsToFile(actual, "../../data/test6_expected/test6_serverless_actual.txt")
      fail("Data mismatch. Compare ../../data/test6_expected/test6.txt to ../../data/test6_expected/test6_serverless_actual.txt")
    }
  }
}
