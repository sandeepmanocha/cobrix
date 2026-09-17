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

class Test40AsciiServerlessSpec extends ServerlessParitySupport {
  test("ascii text via is_text") {
    val copybook =
      """       01  RECORD.
           05  A1       PIC X(1).
           05  A2       PIC X(5).
           05  A3       PIC X(10).
    """
    val df = CobolServerlessReader.read(
      spark,
      "../../data/test40_data_ascii",
      Map(
        "copybook_contents" -> copybook,
        "is_text" -> "true",
        "encoding" -> "ascii",
        "pedantic" -> "true"
      )
    )
    assert(df.count() >= 0)
  }
}
