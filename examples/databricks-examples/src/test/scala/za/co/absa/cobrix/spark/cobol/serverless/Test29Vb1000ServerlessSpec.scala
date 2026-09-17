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

import za.co.absa.cobrix.spark.cobol.serverless.spike.VbFixtureGeneratorMain

import java.nio.file.Files

class Test29Vb1000ServerlessSpec extends ServerlessParitySupport {
  private val copybook =
    """      01  R.
                03 A        PIC X(2).
      """

  test("skip_if_exists preserves a valid generated fixture") {
    val target = Files.createTempFile("vb-fixture", ".dat")
    Files.write(target, VbFixtureGeneratorMain.encodeRecords(4))

    VbFixtureGeneratorMain.main(Array(target.toString, "4", "skip_if_exists"))

    assert(Files.readAllBytes(target).sameElements(VbFixtureGeneratorMain.encodeRecords(4)))
  }

  test("skip_if_exists rejects a stale fixture with the wrong size") {
    val target = Files.createTempFile("vb-fixture", ".dat")
    Files.write(target, Array.fill[Byte](7)(9))

    assertThrows[IllegalArgumentException] {
      VbFixtureGeneratorMain.main(Array(target.toString, "4", "skip_if_exists"))
    }
  }

  test("generated 1000-row VB file counts as 1000 records") {
    val target = Files.createTempFile("vb-1000", ".dat")
    Files.deleteIfExists(target)
    VbFixtureGeneratorMain.main(Array(target.toString, "1000", "create"))

    val df = CobolServerlessReader.read(
      spark,
      target.toString,
      Map(
        "copybook_contents" -> copybook,
        "record_format" -> "VB"
      )
    )
    assert(df.count() == 1000L)
  }
}
