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

import org.apache.spark.sql.{DataFrame, SparkSession}
import za.co.absa.cobrix.spark.cobol.reader.FixedLenReader

import java.nio.charset.Charset

object TextDatasetScan {

  def scan(
    spark: SparkSession,
    paths: Seq[String],
    params: SerializableReaderParams,
    schema: org.apache.spark.sql.types.StructType
  ): DataFrame = {
    implicit val rowEnc = RowEncoders.forSchema(schema)
    val options = params.options
    val charsetName = options.get("ascii_charset").orElse(options.get("encoding").filter(_.equalsIgnoreCase("ascii")).map(_ => "US-ASCII")).getOrElse("UTF-8")
    val minLen = options.get("minimum_record_length").map(_.toInt).getOrElse(1)
    val maxLen = options.get("maximum_record_length").map(_.toInt).getOrElse(Int.MaxValue)
    val textDf = paths.map(p => spark.read.textFile(p)).reduce(_ union _)

    textDf.flatMap { line =>
      if (line == null || line.isEmpty || line.length < minLen || line.length > maxLen) {
        Seq.empty
      } else {
        val reader = ServerlessReaderBuilder.buildFromSerializable(params).asInstanceOf[FixedLenReader]
        val bytes = line.getBytes(Charset.forName(charsetName))
        reader.getRowIterator(bytes).toSeq
      }
    }.toDF()
  }
}
