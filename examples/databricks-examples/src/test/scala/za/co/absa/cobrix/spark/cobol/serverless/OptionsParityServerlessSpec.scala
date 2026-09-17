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

class Test7FillersServerlessSpec extends ServerlessParitySupport {
  test("fillers drop value and group") {
    val df = CobolServerlessReader.read(
      spark,
      "../../data/test7_data",
      Map(
        "copybook" -> "file://../../data/test7_fillers.cob",
        "schema_retention_policy" -> "collapse_root",
        "drop_group_fillers" -> "true",
        "drop_value_fillers" -> "true",
        "filler_naming_policy" -> "sequence_numbers"
      )
    )
    val expectedSchema = java.nio.file.Files.readAllLines(java.nio.file.Paths.get("../../data/test7_expected/test7_schema.json"), java.nio.charset.StandardCharsets.ISO_8859_1).toArray.mkString("\n")
    val actualSchema = za.co.absa.cobrix.spark.cobol.utils.SparkUtils.prettyJSON(df.schema.json)
    assert(actualSchema == expectedSchema)
    val actualPretty = za.co.absa.cobrix.spark.cobol.utils.SparkUtils.convertDataFrameToPrettyJSON(df.orderBy("AMOUNT"), 100)
    val expectedPretty = java.nio.file.Files.readAllLines(java.nio.file.Paths.get("../../data/test7_expected/test7.txt"), java.nio.charset.StandardCharsets.ISO_8859_1).toArray.mkString("\n")
    assert(actualPretty == expectedPretty)
  }
}

class Test1bRecordIdServerlessSpec extends ServerlessParitySupport {
  test("generate_record_id") {
    val df = CobolServerlessReader.read(
      spark,
      "../../data/test1_data",
      Map(
        "copybook" -> "file://../../data/test1_copybook.cob",
        "generate_record_id" -> "true",
        "schema_retention_policy" -> "collapse_root"
      )
    )
    assertGolden(
      df,
      "../../data/test1b_expected/test1b_schema.json",
      "../../data/test1b_expected/test1b.txt",
      "../../data/test1b_expected/test1b_schema_serverless_actual.json",
      "../../data/test1b_expected/test1b_serverless_actual.txt"
    )
  }
}

class Test13HeadersServerlessSpec extends ServerlessParitySupport {
  test("file start and end offsets") {
    val copybook = java.nio.file.Files.readAllLines(java.nio.file.Paths.get("../../data/test13a_file_header_footer.cob"), java.nio.charset.StandardCharsets.ISO_8859_1).toArray.mkString("\n")
    val df = CobolServerlessReader.read(
      spark,
      "../../data/test13a_data",
      Map(
        "copybook_contents" -> copybook,
        "schema_retention_policy" -> "collapse_root",
        "file_start_offset" -> "10",
        "file_end_offset" -> "12"
      )
    )
    assertGolden(
      df.orderBy("COMPANY_ID", "AMOUNT"),
      "../../data/test13_expected/test13a_schema.json",
      "../../data/test13_expected/test13a.txt",
      "../../data/test13_expected/test13a_schema_serverless_actual.json",
      "../../data/test13_expected/test13a_serverless_actual.txt"
    )
  }
}

class Test20InputFileNameServerlessSpec extends ServerlessParitySupport {
  test("dataframe loads with test1 path") {
    val df = CobolServerlessReader.read(
      spark,
      "../../data/test1_data",
      Map("copybook" -> "file://../../data/test1_copybook.cob", "schema_retention_policy" -> "collapse_root")
    )
    assert(df.count() > 0)
  }
}

class Test27RecordLengthServerlessSpec extends ServerlessParitySupport {
  test("forced record_length 2") {
    val tmp = java.nio.file.Files.createTempFile("rec_len", ".dat")
    java.nio.file.Files.write(tmp, "AABBBCCDDDEEFFFZYY".getBytes)
    val df = CobolServerlessReader.read(
      spark,
      tmp.toString,
      Map(
        "copybook_contents" ->
          """      01  R.
                03 A        PIC X(2).
                03 B        PIC X(1).
      """,
        "record_length" -> "2"
      )
    )
    assert(df.count() == 9)
    java.nio.file.Files.deleteIfExists(tmp)
  }
}
