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

import za.co.absa.cobrix.spark.cobol.serverless.spike.CitiFakeFixtures

import java.nio.file.Files

class CitiOptionPacksServerlessSpec extends ServerlessParitySupport {
  private lazy val packDir = {
    val dir = Files.createTempDirectory("citi-fakes")
    CitiFakeFixtures.writePack(dir)
    dir.toAbsolutePath.toString
  }

  test("VB big-endian adjustment=-4") {
    val df = CobolServerlessReader.read(
      spark,
      s"$packDir/vb_be/data.dat",
      Map(
        "copybook_contents" -> CitiFakeFixtures.vbCopybook,
        "record_format" -> "VB",
        "is_bdw_big_endian" -> "true",
        "is_rdw_big_endian" -> "true",
        "bdw_adjustment" -> "-4",
        "rdw_adjustment" -> "-4"
      )
    )
    assert(df.count() == CitiFakeFixtures.VbBeRecords)
  }

  test("V + variable OCCURS + RDW BE adjustment=-4 + cp1047") {
    val df = CobolServerlessReader.read(
      spark,
      s"$packDir/v_occurs/data.dat",
      Map(
        "copybook_contents" -> CitiFakeFixtures.vOccursCopybook,
        "record_format" -> "V",
        "is_rdw_big_endian" -> "true",
        "rdw_adjustment" -> "-4",
        "variable_size_occurs" -> "true",
        "ebcdic_code_page" -> "cp1047"
      )
    )
    assert(df.count() == CitiFakeFixtures.VOccursRecords)
  }

  test("VB + segments + BE adjustment=-4") {
    val df = CobolServerlessReader.read(
      spark,
      s"$packDir/vb_seg/data.dat",
      Map(
        "copybook_contents" -> CitiFakeFixtures.vbSegCopybook,
        "record_format" -> "VB",
        "is_bdw_big_endian" -> "true",
        "is_rdw_big_endian" -> "true",
        "bdw_adjustment" -> "-4",
        "rdw_adjustment" -> "-4",
        "segment_field" -> "SEGMENT_ID"
      )
    )
    assert(df.count() == CitiFakeFixtures.VbSegRecords)
  }

  test("record_format=D ASCII") {
    val df = CobolServerlessReader.read(
      spark,
      s"$packDir/ascii_d/ascii.txt",
      Map(
        "copybook_contents" -> CitiFakeFixtures.asciiDCopybook,
        "record_format" -> "D",
        "encoding" -> "ascii"
      )
    )
    assert(df.count() == CitiFakeFixtures.AsciiDRecords)
  }

  test("nested NAME.FIRST-NAME / LAST-NAME") {
    val df = CobolServerlessReader.read(
      spark,
      s"$packDir/nested_name/person.dat",
      Map(
        "copybook_contents" -> CitiFakeFixtures.nestedNameCopybook,
        "encoding" -> "ascii",
        "schema_retention_policy" -> "keep_original"
      )
    )
    assert(df.count() == CitiFakeFixtures.NestedNameRecords)
    val person = df.schema("PERSON").dataType.asInstanceOf[org.apache.spark.sql.types.StructType]
    assert(person.fieldNames.contains("NAME"))
  }

  ignore("control file next to data is not read as COBOL") {
    val df = CobolServerlessReader.read(
      spark,
      s"$packDir/nested_name",
      Map(
        "copybook_contents" -> CitiFakeFixtures.nestedNameCopybook,
        "encoding" -> "ascii",
        "schema_retention_policy" -> "keep_original"
      )
    )
    assert(df.count() == CitiFakeFixtures.NestedNameRecords)
  }

  test("F pack extra options on test1") {
    val df = CobolServerlessReader.read(
      spark,
      "../../data/test1_data",
      Map(
        "copybook" -> "file://../../data/test1_copybook.cob",
        "schema_retention_policy" -> "collapse_root",
        "ebcdic_code_page" -> "cp037_extended",
        "string_trimming_policy" -> "none",
        "comments_ubound" -> "120"
      )
    )
    assert(df.count() == 10L)
  }

  test("test9 with cp1047") {
    val df = CobolServerlessReader.read(
      spark,
      "../../data/test9_data",
      Map(
        "copybook" -> "file://../../data/test9_copybook.cob",
        "schema_retention_policy" -> "collapse_root",
        "ebcdic_code_page" -> "cp1047"
      )
    )
    assert(df.count() > 0L)
  }
}
