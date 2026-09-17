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

import za.co.absa.cobrix.spark.cobol.source.fixtures.BinaryFileFixture

class Test29BdwServerlessSpec extends ServerlessParitySupport with BinaryFileFixture {
  private val copybook =
    """      01  R.
                03 A        PIC X(2).
      """

  test("VB little-endian BDW+RDW two records") {
    def hdr(payload: Int): Seq[Byte] = {
      val b0 = (payload % 256).toByte
      val b1 = (payload / 256).toByte
      Seq(0, 0, b0, b1)
    }
    val data = Range(0, 2).flatMap { blockNum =>
      hdr(6) ++ hdr(2) ++ Seq((0xF0 + 0).toByte, (0xF0 + blockNum).toByte)
    }.toArray
    withTempBinFile("bdw", ".dat", data) { tmp =>
      val df = CobolServerlessReader.read(
        spark,
        tmp,
        Map(
          "copybook_contents" -> copybook,
          "record_format" -> "VB"
        )
      )
      val actual = df.orderBy("A").toJSON.collect().mkString("[", ",", "]")
      assert(actual == """[{"A":"00"},{"A":"01"}]""")
      ()
    }
  }
}
