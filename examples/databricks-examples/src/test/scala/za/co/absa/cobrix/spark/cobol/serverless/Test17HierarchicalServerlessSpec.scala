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

import za.co.absa.cobrix.spark.cobol.source.base.CobolTestBase

class Test17HierarchicalServerlessSpec extends ServerlessParitySupport with CobolTestBase {
  test("hierarchical RDW test17a flat") {
    val df = CobolServerlessReader.read(
      spark,
      "../../data/test17/HIERARCHICAL.DATA.RDW.dat",
      Map(
        "copybook" -> "file://../../data/test17_hierarchical.cob",
        "pedantic" -> "true",
        "record_format" -> "V",
        "generate_record_id" -> "true",
        "schema_retention_policy" -> "collapse_root",
        "segment_field" -> "SEGMENT_ID",
        "redefine_segment_id_map:1" -> "COMPANY => 1",
        "redefine-segment-id-map:2" -> "DEPT => 2",
        "redefine-segment-id-map:3" -> "EMPLOYEE => 3",
        "redefine-segment-id-map:4" -> "OFFICE => 4",
        "redefine-segment-id-map:5" -> "CUSTOMER => 5",
        "redefine-segment-id-map:6" -> "CONTACT => 6",
        "redefine-segment-id-map:7" -> "CONTRACT => 7"
      )
    )
    testSchema(df, "../../data/test17_expected/test17a_schema_serverless_actual.json", "../../data/test17_expected/test17a_schema.json")
    val actualDf = df.orderBy("File_Id", "Record_Id").toJSON.take(300)
    testData(actualDf, "../../data/test17_expected/test17a_serverless_actual.txt", "../../data/test17_expected/test17a.txt")
  }
}
