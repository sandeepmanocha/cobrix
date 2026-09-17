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

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

class Test4MultisegmentServerlessSpec extends ServerlessParitySupport {
  test("multisegment test4") {
    val df = CobolServerlessReader.read(
      spark,
      "../../data/test4_data",
      Map(
        "copybook" -> "file://../../data/test4_copybook.cob",
        "encoding" -> "ascii",
        "record_format" -> "V",
        "segment_field" -> "SEGMENT_ID",
        "segment_id_level0" -> "C",
        "segment_id_level1" -> "P",
        "generate_record_id" -> "true",
        "schema_retention_policy" -> "collapse_root",
        "segment_id_prefix" -> "A"
      )
    )
    val expectedSchema = java.nio.file.Files.readAllLines(java.nio.file.Paths.get("../../data/test4_expected/test4_schema.json"), java.nio.charset.StandardCharsets.ISO_8859_1).toArray.mkString("\n")
    assert(df.schema.json == expectedSchema)
    val actualJson = df.orderBy("File_Id", "Record_Id").toJSON.take(60)
    val actualResultsPath = "../../data/test4_expected/test4_serverless_actual.txt"
    FileUtils.writeStringsToFile(actualJson, actualResultsPath)
    val actual = Files.readAllLines(Paths.get(actualResultsPath), StandardCharsets.ISO_8859_1).toArray.toSeq.map(_.toString)
    val expected = Files.readAllLines(Paths.get("../../data/test4_expected/test4.txt"), StandardCharsets.ISO_8859_1).toArray.toSeq.map(_.toString)
    val mismatch = actual != expected
    if (!mismatch) {
      Files.deleteIfExists(Paths.get(actualResultsPath))
    }
    assert(!mismatch, "compare test4.txt vs test4_serverless_actual.txt")
  }
}
