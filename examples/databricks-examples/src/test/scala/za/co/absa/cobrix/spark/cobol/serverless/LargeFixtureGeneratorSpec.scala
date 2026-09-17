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
import za.co.absa.cobrix.spark.cobol.serverless.spike.LargeFixtureGeneratorMain

import java.nio.file.Files

class LargeFixtureGeneratorSpec extends AnyFunSuite {
  test("skip_if_exists preserves a valid generated fixture") {
    val source = Files.createTempFile("fixture-source", ".bin")
    val target = Files.createTempFile("fixture-target", ".bin")
    Files.write(source, Array[Byte](1, 2, 3))
    Files.write(target, Array[Byte](1, 2, 3, 1, 2, 3))

    LargeFixtureGeneratorMain.main(Array(source.toString, target.toString, "4", "skip_if_exists"))

    assert(Files.readAllBytes(target).sameElements(Array[Byte](1, 2, 3, 1, 2, 3)))
  }

  test("skip_if_exists recreates an undersized fixture") {
    val source = Files.createTempFile("fixture-source", ".bin")
    val target = Files.createTempFile("fixture-target", ".bin")
    Files.write(source, Array[Byte](1, 2, 3))
    Files.write(target, Array[Byte](9))

    LargeFixtureGeneratorMain.main(Array(source.toString, target.toString, "4", "skip_if_exists"))

    assert(Files.readAllBytes(target).sameElements(Array[Byte](1, 2, 3, 1, 2, 3)))
  }

  test("skip_if_exists rejects a stale fixture with the wrong size") {
    val source = Files.createTempFile("fixture-source", ".bin")
    val target = Files.createTempFile("fixture-target", ".bin")
    Files.write(source, Array[Byte](1, 2, 3))
    Files.write(target, Array.fill[Byte](7)(9))

    assertThrows[IllegalArgumentException] {
      LargeFixtureGeneratorMain.main(Array(source.toString, target.toString, "4", "skip_if_exists"))
    }
  }
}
