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
import za.co.absa.cobrix.spark.cobol.reader.{FixedLenReader, FixedLenTextReader, VarLenReader}
import za.co.absa.cobrix.spark.cobol.schema.CobolSchema

object CobolServerlessReader {

  def read(spark: SparkSession, path: String, options: Map[String, String]): DataFrame =
    read(spark, Seq(path), options)

  def read(spark: SparkSession, paths: Seq[String], options: Map[String, String]): DataFrame = {
    val merged = ServerlessReaderBuilder.mergeOptions(paths, options)
    val cobolParams = ServerlessReaderBuilder.parse(paths, merged)
    ServerlessReaderBuilder.validateSupportedInput(cobolParams)
    val copybooks = ServerlessReaderBuilder.copybookContents(cobolParams)
    val reader = ServerlessReaderBuilder.buildReader(copybooks, cobolParams, hasCompressedFiles = false)
    val schema = reader.getSparkSchema
    val ser = SerializableReaderParams(copybooks, merged, cobolParams.sourcePaths)
    val debugIgnore = cobolParams.debugIgnoreFileSize
    val files = if (cobolParams.isText) Seq.empty else InputFile.discover(spark, cobolParams.sourcePaths)
    ServerlessReaderBuilder.validateSupportedPaths(files.map(_.path))

    val df = reader match {
      case _: FixedLenTextReader =>
        TextDatasetScan.scan(spark, cobolParams.sourcePaths, ser, schema)
      case fixed: FixedLenReader =>
        val splits = FixedSplitPlanner.plan(files, fixed.getRecordSize, debugIgnore)
        FixedDatasetScan.scan(spark, splits, ser, schema)
      case varLen: VarLenReader =>
        val splits = VarLenIndexBuilder.build(files, varLen)
        VarLenDatasetScan.scan(spark, splits, ser, schema)
      case other =>
        throw new IllegalArgumentException(s"Unsupported reader type: ${other.getClass.getName}")
    }
    df.to(SchemaAlign.forDataset(schema, df.schema))
  }

  def schema(spark: SparkSession, options: Map[String, String], dummyPath: String = "unused"): org.apache.spark.sql.types.StructType = {
    val lower = options.map { case (k, v) => k.toLowerCase -> v }
    CobolSchema.fromSparkOptions(copybookSeq(lower, spark), lower).getSparkSchema
  }

  private def copybookSeq(options: Map[String, String], spark: SparkSession): Seq[String] = {
    val params = ServerlessReaderBuilder.parse(Seq(options.getOrElse("path", ".")), options)
    ServerlessReaderBuilder.copybookContents(params)
  }
}
