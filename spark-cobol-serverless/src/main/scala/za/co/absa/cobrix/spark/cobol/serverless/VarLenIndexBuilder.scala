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

import za.co.absa.cobrix.cobol.reader.index.entry.SparseIndexEntry
import za.co.absa.cobrix.spark.cobol.reader.VarLenReader

import scala.collection.mutable.ArrayBuffer

object VarLenIndexBuilder {

  def build(
    files: Seq[InputFile],
    reader: VarLenReader
  ): Seq[ByteRangeSplit] = {
    files.zipWithIndex.flatMap { case (file, fileId) =>
      indexFile(file, fileId, reader)
    }
  }

  def indexFile(
    file: InputFile,
    fileId: Int,
    reader: VarLenReader
  ): Seq[ByteRangeSplit] = {
    val filePath = file.path
    val startOffset = reader.getReaderProperties.fileStartOffset
    val endOffset = reader.getReaderProperties.fileEndOffset
    val maximumBytes = math.max(0L, file.length - startOffset - endOffset)

    val dataStream = new NioFileStreamer(filePath, startOffset, maximumBytes)
    val headerStream = new NioFileStreamer(filePath)
    val index: ArrayBuffer[SparseIndexEntry] = try {
      reader.generateIndex(dataStream, headerStream, fileId, reader.isRdwBigEndian)
    } finally {
      dataStream.close()
      headerStream.close()
    }

    val withEnd =
      index.map(entry => if (entry.offsetTo == -1) entry.copy(offsetTo = startOffset + maximumBytes) else entry)

    if (withEnd.isEmpty) {
      Seq(ByteRangeSplit(filePath, startOffset, maximumBytes, fileId, 0L))
    } else {
      withEnd.map { entry =>
        val from = if (entry.offsetFrom >= 0) entry.offsetFrom else 0L
        val numBytes = if (entry.offsetTo > 0L) entry.offsetTo - from else 0L
        ByteRangeSplit(filePath, from, numBytes, entry.fileId, entry.recordIndex)
      }.toSeq
    }
  }
}
