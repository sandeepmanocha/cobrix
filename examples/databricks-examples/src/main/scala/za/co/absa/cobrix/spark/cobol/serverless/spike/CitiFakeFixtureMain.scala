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

package za.co.absa.cobrix.spark.cobol.serverless.spike

import za.co.absa.cobrix.spark.cobol.serverless.NioFileStreamer

import java.nio.file.Files

object CitiFakeFixtureMain {
  private val CreateMode = "create"
  private val SkipIfExistsMode = "skip_if_exists"

  def main(args: Array[String]): Unit = {
    if (args.length < 1 || args.length > 2) {
      throw new IllegalArgumentException("Usage: CitiFakeFixtureMain <out-dir> [create|skip_if_exists]")
    }
    val dir = NioFileStreamer.toPath(args(0))
    val mode = if (args.length == 2) args(1).trim.toLowerCase else CreateMode
    if (mode != CreateMode && mode != SkipIfExistsMode) {
      throw new IllegalArgumentException(s"Unknown mode '$mode'")
    }
    if (mode == SkipIfExistsMode && CitiFakeFixtures.packComplete(dir)) {
      println(s"CITI_FAKE_DIR=$dir")
      println("CITI_FAKE_SKIPPED=true")
    } else {
      Files.createDirectories(dir)
      CitiFakeFixtures.writePack(dir)
      println(s"CITI_FAKE_DIR=$dir")
      println("CITI_FAKE_SKIPPED=false")
    }
  }
}
