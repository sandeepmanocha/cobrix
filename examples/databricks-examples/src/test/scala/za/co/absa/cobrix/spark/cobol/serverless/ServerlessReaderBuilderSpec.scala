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

class ServerlessReaderBuilderSpec extends AnyFunSuite {
  test("inline copybook takes precedence over copybook path") {
    val inline = "       01 RECORD. 05 FIELD PIC X."
    val params = ServerlessReaderBuilder.parse(
      Seq("../../data/test1_data"),
      Map(
        "copybook_contents" -> inline,
        "copybook" -> "file://../../data/test1_copybook.cob"
      )
    )

    assert(ServerlessReaderBuilder.copybookContents(params) == Seq(inline))
  }

  test("jar copybook is loaded from classpath") {
    val params = ServerlessReaderBuilder.parse(
      Seq("../../data/test1_data"),
      Map("copybook" -> "jar://test/copybook.cpy")
    )

    assert(ServerlessReaderBuilder.copybookContents(params).head.nonEmpty)
  }

  test("GPG input is rejected explicitly") {
    val params = ServerlessReaderBuilder.parse(
      Seq("../../data/test1_data"),
      Map(
        "copybook" -> "file://../../data/test1_copybook.cob",
        "gpg_private_key" -> "not-a-key"
      )
    )

    val error = intercept[UnsupportedOperationException] {
      ServerlessReaderBuilder.validateSupportedInput(params)
    }
    assert(error.getMessage.contains("GPG"))
  }

  test("compressed input is rejected explicitly") {
    val params = ServerlessReaderBuilder.parse(
      Seq("../../data/example.dat.gz"),
      Map("copybook" -> "file://../../data/test1_copybook.cob")
    )

    val error = intercept[UnsupportedOperationException] {
      ServerlessReaderBuilder.validateSupportedInput(params)
    }
    assert(error.getMessage.contains("Compressed"))
  }
}
