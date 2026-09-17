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

object FixedSplitPlanner {

  val defaultSplitSizeBytes: Long = 128L * 1024L * 1024L

  def plan(
    files: Seq[InputFile],
    recordSize: Int,
    debugIgnoreFileSize: Boolean,
    splitSizeBytes: Long = defaultSplitSizeBytes
  ): Seq[ByteRangeSplit] = {
    if (recordSize <= 0) {
      throw new IllegalArgumentException(s"Invalid record size: $recordSize")
    }
    files.zipWithIndex.flatMap { case (file, fileId) =>
      splitsForFile(file.path, file.length, fileId, recordSize, debugIgnoreFileSize, splitSizeBytes)
    }
  }

  def splitsForFile(
    filePath: String,
    size: Long,
    fileId: Int,
    recordSize: Int,
    debugIgnoreFileSize: Boolean,
    splitSizeBytes: Long
  ): Seq[ByteRangeSplit] = {
    if (!debugIgnoreFileSize && size % recordSize != 0) {
      throw new IllegalArgumentException(
        s"File $filePath size ($size bytes) is NOT DIVISIBLE by the RECORD SIZE calculated from the copybook ($recordSize bytes per record)."
      )
    }
    if (size == 0) {
      Seq.empty
    } else {
      val alignedSplit = Math.max(recordSize.toLong, (splitSizeBytes / recordSize) * recordSize)
      val ranges = Iterator.iterate(0L)(_ + alignedSplit).takeWhile(_ < size).toSeq
      ranges.zipWithIndex.map { case (start, _) =>
        val remaining = size - start
        val length = Math.min(alignedSplit, remaining)
        val alignedLength = if (debugIgnoreFileSize) length else (length / recordSize) * recordSize
        ByteRangeSplit(filePath, start, alignedLength, fileId)
      }.filter(_.length > 0)
    }
  }
}
