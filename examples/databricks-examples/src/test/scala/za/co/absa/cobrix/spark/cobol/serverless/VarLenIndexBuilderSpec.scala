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
import za.co.absa.cobrix.spark.cobol.reader.VarLenNestedReader
import za.co.absa.cobrix.cobol.reader.parameters.CobolParametersParser._
import za.co.absa.cobrix.cobol.reader.parameters.{CobolParametersParser, Parameters}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

class VarLenIndexBuilderSpec extends AnyFunSuite {

  test("index entries land on the variable-length file") {
    val copybook = Files.readAllLines(Paths.get("../../data/test17_hierarchical.cob"), StandardCharsets.ISO_8859_1).toArray.mkString("\n")
    val options = Map(
      "copybook_contents" -> copybook,
      "path" -> "../../data/test17/HIERARCHICAL.DATA.RDW.dat",
      "record_format" -> "V",
      "schema_retention_policy" -> "collapse_root"
    )
    val params = CobolParametersParser.parse(new Parameters(options))
    val reader = new VarLenNestedReader(Seq(copybook), getReaderProperties(params, None))
    val path = Paths.get("../../data/test17/HIERARCHICAL.DATA.RDW.dat").toAbsolutePath
    val splits = VarLenIndexBuilder.indexFile(
      InputFile(path.toString, Files.size(path)),
      0,
      reader
    )
    assert(splits.nonEmpty)
    assert(splits.forall(_.path.nonEmpty))
  }
}
