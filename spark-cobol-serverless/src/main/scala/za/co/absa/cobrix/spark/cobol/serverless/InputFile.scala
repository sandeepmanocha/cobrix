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

import org.apache.spark.sql.SparkSession

case class InputFile(path: String, length: Long)

object InputFile {
  def discover(spark: SparkSession, paths: Seq[String]): Seq[InputFile] = {
    require(paths.nonEmpty, "At least one input path is required.")
    spark.read
      .format("binaryFile")
      .load(paths: _*)
      .select("path", "length")
      .collect()
      .map(row => InputFile(row.getString(0), row.getLong(1)))
      .sortBy(_.path)
      .toSeq
  }
}
