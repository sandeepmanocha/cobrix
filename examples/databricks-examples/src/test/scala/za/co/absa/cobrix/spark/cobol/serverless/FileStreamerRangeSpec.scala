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

import java.io.RandomAccessFile
import java.nio.file.Paths

class FileStreamerRangeSpec extends AnyFunSuite {

  test("NioFileStreamer reads a byte range matching RandomAccessFile") {
    val path = Paths.get("../../data/test1_data/example.bin").toAbsolutePath.toString
    val offset = 10L
    val length = 20
    val stream = new NioFileStreamer(path, offset, length)
    val fromStream = stream.next(length)
    stream.close()

    val raf = new RandomAccessFile(path, "r")
    raf.seek(offset)
    val expected = new Array[Byte](length)
    raf.readFully(expected)
    raf.close()

    assert(fromStream.sameElements(expected))
  }

  test("NioFileStreamer reads no bytes for a zero-length range") {
    val path = Paths.get("../../data/test1_data/example.bin").toAbsolutePath.toString
    val stream = new NioFileStreamer(path, startOffset = 10L, maximumBytes = 0L)
    try {
      assert(stream.next(20).isEmpty)
    } finally {
      stream.close()
    }
  }
}
