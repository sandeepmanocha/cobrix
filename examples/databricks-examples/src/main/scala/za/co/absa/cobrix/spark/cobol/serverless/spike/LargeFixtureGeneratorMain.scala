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

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.{Files, StandardOpenOption}

object LargeFixtureGeneratorMain {
  private val CreateMode = "create"
  private val SkipIfExistsMode = "skip_if_exists"

  def main(args: Array[String]): Unit = {
    if (args.length < 3 || args.length > 4) {
      throw new IllegalArgumentException(
        "Usage: LargeFixtureGeneratorMain <source> <target> <minimum-bytes> [create|skip_if_exists]"
      )
    }

    val source = NioFileStreamer.toPath(args(0))
    val target = NioFileStreamer.toPath(args(1))
    val minimumBytes = args(2).toLong
    val mode = if (args.length == 4) args(3).trim.toLowerCase else CreateMode
    if (mode != CreateMode && mode != SkipIfExistsMode) {
      throw new IllegalArgumentException(s"Unknown mode '$mode'; expected $CreateMode or $SkipIfExistsMode")
    }

    val sourceBytes = Files.readAllBytes(source)
    if (sourceBytes.isEmpty) {
      throw new IllegalArgumentException(s"Source fixture is empty: $source")
    }

    val repetitions = Math.floorDiv(minimumBytes - 1L, sourceBytes.length.toLong) + 1L
    val expectedBytes = repetitions * sourceBytes.length.toLong
    val existingBytes = if (Files.exists(target)) Some(Files.size(target)) else None

    existingBytes match {
      case Some(size) if mode == SkipIfExistsMode && size == expectedBytes =>
        println(s"LARGE_FIXTURE_PATH=$target")
        println(s"LARGE_FIXTURE_BYTES=$size")
        println("LARGE_FIXTURE_SKIPPED=true")
      case Some(size) if mode == SkipIfExistsMode && size >= minimumBytes =>
        throw new IllegalArgumentException(
          s"Existing fixture has unexpected size $size; expected exactly $expectedBytes bytes: $target"
        )
      case _ =>
        Option(target.getParent).foreach(parent => Files.createDirectories(parent))
        val channel = FileChannel.open(
          target,
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING,
          StandardOpenOption.WRITE
        )
        try {
          var repetition = 0L
          while (repetition < repetitions) {
            val buffer = ByteBuffer.wrap(sourceBytes)
            while (buffer.hasRemaining) {
              channel.write(buffer)
            }
            repetition += 1L
          }
        } finally {
          channel.close()
        }

        println(s"LARGE_FIXTURE_PATH=$target")
        println(s"LARGE_FIXTURE_BYTES=${Files.size(target)}")
        println(s"LARGE_FIXTURE_REPETITIONS=$repetitions")
        println("LARGE_FIXTURE_SKIPPED=false")
    }
  }
}
