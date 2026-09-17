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

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.Encoders
import za.co.absa.cobrix.spark.cobol.serverless.{ByteRangeSplit, NioFileStreamer}

object SeekSpikeMain {
  case class SeekRow(path: String, offset: Long, bytes_read: Long, payload: Array[Byte])

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder().appName("CobrixSeekSpike").getOrCreate()
    val path = args.headOption.getOrElse(throw new IllegalArgumentException("Usage: SeekSpikeMain <file> [offset] [length]"))
    val offset = if (args.length > 1) args(1).toLong else 0L
    val length = if (args.length > 2) args(2).toInt else 64
    implicit val splitEnc = Encoders.product[ByteRangeSplit]
    implicit val seekEnc = Encoders.product[SeekRow]
    val splits = Seq(ByteRangeSplit(path, offset, length.toLong, 0))
    val df = spark.createDataset(splits).map { split =>
      val stream = new NioFileStreamer(split.path, split.startOffset, split.length)
      try {
        val buf = stream.next(split.length.toInt)
        SeekRow(split.path, split.startOffset, buf.length.toLong, buf)
      } finally {
        stream.close()
      }
    }.toDF()
    val n = df.count()
    println(s"SPIKE_SEEK_ROWS=$n")
    df.show(false)
  }
}
