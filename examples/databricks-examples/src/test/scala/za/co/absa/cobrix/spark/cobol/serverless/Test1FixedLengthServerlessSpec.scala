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

class Test1FixedLengthServerlessSpec extends ServerlessParitySupport {
  test("fixed-length test1 matches golden files") {
    val df = CobolServerlessReader.read(
      spark,
      "../../data/test1_data",
      Map("copybook" -> "file://../../data/test1_copybook.cob", "schema_retention_policy" -> "collapse_root")
    )
    assertGolden(
      df,
      "../../data/test1_expected/test1_schema.json",
      "../../data/test1_expected/test1.txt",
      "../../data/test1_expected/test1_schema_serverless_actual.json",
      "../../data/test1_expected/test1_serverless_actual.txt"
    )
  }
}
