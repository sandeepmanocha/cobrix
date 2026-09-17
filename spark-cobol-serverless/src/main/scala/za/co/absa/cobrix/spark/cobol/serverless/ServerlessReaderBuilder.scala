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

import za.co.absa.cobrix.cobol.reader.parameters.CobolParametersParser._
import za.co.absa.cobrix.cobol.reader.parameters.{CobolParameters, CobolParametersParser, Parameters}
import za.co.absa.cobrix.spark.cobol.reader._
import za.co.absa.cobrix.spark.cobol.source.parameters.CobolParametersValidator
import za.co.absa.cobrix.spark.cobol.utils.ResourceUtils

import java.nio.charset.StandardCharsets
import java.nio.file.Files

object ServerlessReaderBuilder {
  private val compressedExtensions = Seq(".gz", ".gzip", ".bz2", ".snappy", ".lz4", ".deflate")

  def mergeOptions(paths: Seq[String], options: Map[String, String]): Map[String, String] = {
    val lower = options.map { case (k, v) => k.toLowerCase -> v }
    if (lower.contains(PARAM_SOURCE_PATH) || lower.contains(PARAM_SOURCE_PATHS) || lower.contains(PARAM_SOURCE_PATHS_LEGACY)) {
      lower
    } else if (paths.size == 1) {
      lower + (PARAM_SOURCE_PATH -> paths.head)
    } else {
      lower + (PARAM_SOURCE_PATHS -> paths.mkString(","))
    }
  }

  def parse(paths: Seq[String], options: Map[String, String]): CobolParameters = {
    val merged = mergeOptions(paths, options)
    val params = CobolParametersParser.parse(new Parameters(merged))
    CobolParametersValidator.checkSanity(params)
    params
  }

  def copybookContents(params: CobolParameters): Seq[String] = {
    params.copybookContent match {
      case Some(contents) => Seq(contents)
      case None =>
        (params.copybookPath.toSeq ++ params.multiCopybookPath).map(loadCopybook)
    }
  }

  def validateSupportedInput(params: CobolParameters): Unit = {
    if (params.gpgPrivateKey.nonEmpty || params.gpgPrivateKeyPassphrase.nonEmpty) {
      throw new UnsupportedOperationException("GPG-encrypted input is not supported by the Serverless NIO reader.")
    }
    validateSupportedPaths(params.sourcePaths)
  }

  def validateSupportedPaths(paths: Seq[String]): Unit = {
    if (paths.exists(path => compressedExtensions.exists(path.toLowerCase.endsWith))) {
      throw new UnsupportedOperationException("Compressed input is not supported by the Serverless NIO reader.")
    }
  }

  def buildReader(copybookContents: Seq[String], params: CobolParameters, hasCompressedFiles: Boolean): Reader = {
    val readerParams = getReaderProperties(params, None)
    if (params.isText && params.variableLengthParams.isEmpty) {
      new FixedLenTextReader(copybookContents, readerParams)
    } else if (params.variableLengthParams.isEmpty && !hasCompressedFiles) {
      new FixedLenNestedReader(copybookContents, readerParams)
    } else {
      new VarLenNestedReader(copybookContents, readerParams)
    }
  }

  def buildFromSerializable(ser: SerializableReaderParams): Reader = {
    val params = parse(ser.paths, ser.options)
    validateSupportedInput(params)
    buildReader(ser.copybookContents, params, hasCompressedFiles = false)
  }

  private def loadCopybook(path: String): String = {
    if (path.toLowerCase.startsWith("jar://")) {
      val resource = path.drop("jar://".length)
      ResourceUtils.readResourceAsString(if (resource.startsWith("/")) resource else s"/$resource")
    } else {
      new String(Files.readAllBytes(NioFileStreamer.toPath(path)), StandardCharsets.ISO_8859_1)
    }
  }
}
