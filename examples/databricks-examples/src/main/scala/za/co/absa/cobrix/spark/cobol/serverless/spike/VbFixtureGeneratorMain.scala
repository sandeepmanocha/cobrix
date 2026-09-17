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

object VbFixtureGeneratorMain {
  private val CreateMode = "create"
  private val SkipIfExistsMode = "skip_if_exists"

  /** Little-endian BDW (payload 6) + RDW (payload 2) + 2-byte PIC X(2) record. */
  val BytesPerRecord: Int = 10

  def expectedBytes(recordCount: Long): Long = recordCount * BytesPerRecord

  def main(args: Array[String]): Unit = {
    if (args.length < 2 || args.length > 3) {
      throw new IllegalArgumentException(
        "Usage: VbFixtureGeneratorMain <target> <record-count> [create|skip_if_exists]"
      )
    }

    val target = NioFileStreamer.toPath(args(0))
    val recordCount = args(1).toLong
    if (recordCount <= 0) {
      throw new IllegalArgumentException(s"record-count must be positive, got $recordCount")
    }
    val mode = if (args.length == 3) args(2).trim.toLowerCase else CreateMode
    if (mode != CreateMode && mode != SkipIfExistsMode) {
      throw new IllegalArgumentException(s"Unknown mode '$mode'; expected $CreateMode or $SkipIfExistsMode")
    }

    val expected = expectedBytes(recordCount)
    val existingBytes = if (Files.exists(target)) Some(Files.size(target)) else None

    existingBytes match {
      case Some(size) if mode == SkipIfExistsMode && size == expected =>
        println(s"VB_FIXTURE_PATH=$target")
        println(s"VB_FIXTURE_BYTES=$size")
        println(s"VB_FIXTURE_RECORDS=$recordCount")
        println("VB_FIXTURE_SKIPPED=true")
      case Some(size) if mode == SkipIfExistsMode =>
        throw new IllegalArgumentException(
          s"Existing fixture has unexpected size $size; expected exactly $expected bytes: $target"
        )
      case _ =>
        Option(target.getParent).foreach(parent => Files.createDirectories(parent))
        val bytes = encodeRecords(recordCount)
        val channel = FileChannel.open(
          target,
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING,
          StandardOpenOption.WRITE
        )
        try {
          val buffer = ByteBuffer.wrap(bytes)
          while (buffer.hasRemaining) {
            channel.write(buffer)
          }
        } finally {
          channel.close()
        }

        println(s"VB_FIXTURE_PATH=$target")
        println(s"VB_FIXTURE_BYTES=${Files.size(target)}")
        println(s"VB_FIXTURE_RECORDS=$recordCount")
        println("VB_FIXTURE_SKIPPED=false")
    }
  }

  def encodeRecords(recordCount: Long): Array[Byte] = {
    val out = new Array[Byte](expectedBytes(recordCount).toInt)
    var offset = 0
    var i = 0L
    while (i < recordCount) {
      writeLeHeader(out, offset, 6)
      writeLeHeader(out, offset + 4, 2)
      out(offset + 8) = (0xF0 + ((i / 10) % 10)).toByte
      out(offset + 9) = (0xF0 + (i % 10)).toByte
      offset += BytesPerRecord
      i += 1L
    }
    out
  }

  private def writeLeHeader(out: Array[Byte], offset: Int, payload: Int): Unit = {
    out(offset) = 0
    out(offset + 1) = 0
    out(offset + 2) = (payload % 256).toByte
    out(offset + 3) = (payload / 256).toByte
  }
}
