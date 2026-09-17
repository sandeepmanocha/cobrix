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

class Test21VariableOccursServerlessSpec extends ServerlessParitySupport {
  test("variable occurs test21") {
    val df = CobolServerlessReader.read(
      spark,
      "../../data/test21_data",
      Map(
        "copybook" -> "file://../../data/test21_copybook.cob",
        "record_format" -> "F",
        "encoding" -> "ascii",
        "variable_size_occurs" -> "true",
        "schema_retention_policy" -> "keep_original"
      )
    )
    assertGolden(
      df,
      "../../data/test21_expected/test21_schema.json",
      "../../data/test21_expected/test21.txt",
      "../../data/test21_expected/test21_schema_serverless_actual.json",
      "../../data/test21_expected/test21_serverless_actual.txt"
    )
  }
}
