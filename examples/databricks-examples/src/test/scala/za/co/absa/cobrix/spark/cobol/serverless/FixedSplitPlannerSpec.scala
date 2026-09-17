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

import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.Files

class FixedSplitPlannerSpec extends AnyFunSuite {

  test("splits are multiples of record size") {
    val tmp = Files.createTempFile("split", ".bin")
    val bytes = Array.fill[Byte](100)(1)
    Files.write(tmp, bytes)
    val splits = FixedSplitPlanner.splitsForFile(tmp.toString, 100L, 0, 10, debugIgnoreFileSize = false, splitSizeBytes = 40)
    assert(splits.nonEmpty)
    assert(splits.forall(s => s.length % 10 == 0))
    assert(splits.map(_.length).sum == 100)
    Files.deleteIfExists(tmp)
  }

  test("rejects non-divisible file size") {
    val tmp = Files.createTempFile("split-bad", ".bin")
    Files.write(tmp, Array.fill[Byte](25)(1))
    val ex = intercept[IllegalArgumentException] {
      FixedSplitPlanner.splitsForFile(tmp.toString, 25L, 0, 10, debugIgnoreFileSize = false, splitSizeBytes = 40)
    }
    assert(ex.getMessage.contains("NOT DIVISIBLE"))
    Files.deleteIfExists(tmp)
  }
}
