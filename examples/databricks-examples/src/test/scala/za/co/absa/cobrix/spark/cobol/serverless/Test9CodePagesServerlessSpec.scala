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

import za.co.absa.cobrix.spark.cobol.utils.FileUtils

class Test9CodePagesServerlessSpec extends ServerlessParitySupport {
  test("code page CP037") {
    val df = CobolServerlessReader.read(
      spark,
      "../../data/test9_data",
      Map(
        "copybook" -> "file://../../data/test9_copybook.cob",
        "schema_retention_policy" -> "collapse_root",
        "ebcdic_code_page" -> "cp037"
      )
    )
    val expectedSchema = FileUtils.readAllFileStringUtf8("../../data/test9_expected/test9_schema.json")
    assert(df.schema.json == expectedSchema)
    val actual = df.toJSON.take(60)
    val expected = FileUtils.readAllFileLinesUtf8("../../data/test9_expected/test9_cp037.txt")
    assert(actual.sameElements(expected))
  }
}
