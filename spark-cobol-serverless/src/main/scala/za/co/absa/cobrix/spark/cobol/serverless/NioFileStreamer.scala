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

import za.co.absa.cobrix.cobol.reader.stream.SimpleStream

import java.net.URI
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.{Files, Path, Paths, StandardOpenOption}

final class NioFileStreamer(
  filePath: String,
  startOffset: Long = 0L,
  maximumBytes: Long = -1L
) extends SimpleStream {
  private val path = NioFileStreamer.toPath(filePath)
  private val fileSize = Files.size(path)
  private val endOffset =
    if (maximumBytes >= 0L) math.min(fileSize, startOffset + maximumBytes)
    else fileSize
  private var byteIndex = startOffset
  private var channel: FileChannel = _

  override def inputFileName: String = filePath
  override def size: Long = endOffset
  override def totalSize: Long = fileSize
  override def offset: Long = byteIndex

  override def next(numberOfBytes: Int): Array[Byte] = {
    if (numberOfBytes <= 0 || byteIndex >= endOffset) {
      Array.emptyByteArray
    } else {
      ensureOpen()
      val requested = math.min(numberOfBytes.toLong, endOffset - byteIndex).toInt
      val buffer = ByteBuffer.allocate(requested)
      var totalRead = 0
      while (totalRead < requested && channel.read(buffer) >= 0) {
        totalRead = buffer.position()
      }
      byteIndex += totalRead
      if (totalRead == requested) buffer.array()
      else java.util.Arrays.copyOf(buffer.array(), totalRead)
    }
  }

  override def copyStream(): SimpleStream =
    new NioFileStreamer(filePath, startOffset, maximumBytes)

  override def close(): Unit = {
    if (channel != null) {
      channel.close()
      channel = null
    }
  }

  private def ensureOpen(): Unit = {
    if (channel == null) {
      channel = FileChannel.open(path, StandardOpenOption.READ)
      channel.position(byteIndex)
    }
  }
}

object NioFileStreamer {
  def toPath(filePath: String): Path = {
    if (filePath.startsWith("dbfs:/Volumes/")) {
      Paths.get(filePath.stripPrefix("dbfs:"))
    } else if (filePath.startsWith("file://../") || filePath.startsWith("file://./")) {
      Paths.get(filePath.stripPrefix("file://"))
    } else if (filePath.startsWith("file:")) {
      Paths.get(URI.create(filePath))
    } else {
      Paths.get(filePath)
    }
  }
}
