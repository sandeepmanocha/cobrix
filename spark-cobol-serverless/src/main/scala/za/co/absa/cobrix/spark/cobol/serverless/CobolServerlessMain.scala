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

object CobolServerlessMain {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder().appName("CobolServerlessReader").getOrCreate()
    val dataPath = argValue(args, "--data").getOrElse(throw new IllegalArgumentException("Missing --data <path>"))
    val copybook = argValue(args, "--copybook")
    val copybookContents = argValue(args, "--copybook_contents")
    val extra = optionsFromArgs(args)
    val options = extra ++
      copybook.map("copybook" -> _).toMap ++
      copybookContents.map("copybook_contents" -> _).toMap
    val df = CobolServerlessReader.read(spark, dataPath, options)
    val out = argValue(args, "--out")
    out match {
      case Some(table) =>
        df.write.mode("overwrite").saveAsTable(table)
        println(s"OUTPUT_TABLE=$table")
        println(s"ROW_COUNT=${spark.table(table).count()}")
      case None        => println(s"ROW_COUNT=${df.count()}")
    }
  }

  private def argValue(args: Array[String], key: String): Option[String] = {
    val i = args.indexOf(key)
    if (i >= 0 && i + 1 < args.length) Some(args(i + 1)) else None
  }

  private def optionsFromArgs(args: Array[String]): Map[String, String] = {
    args.sliding(2, 2).collect {
      case Array(k, v) if k.startsWith("--") && k != "--data" && k != "--copybook" && k != "--copybook_contents" && k != "--out" =>
        k.drop(2) -> v
    }.toMap
  }
}
