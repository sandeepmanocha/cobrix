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

import org.apache.spark.sql.{Encoder, Row}
import org.apache.spark.sql.types.StructType

object RowEncoders {
  def forSchema(schema: StructType): Encoder[Row] = {
    publicRowEncoder(schema)
      .orElse(catalystExpressionEncoder(schema))
      .getOrElse {
        throw new IllegalStateException(
          "No Encoder[Row] available. Need org.apache.spark.sql.Encoders.row (Spark 4 / Serverless) " +
            "or org.apache.spark.sql.catalyst.encoders.ExpressionEncoder (classic Spark 3.5)."
        )
      }
  }

  private def publicRowEncoder(schema: StructType): Option[Encoder[Row]] = {
    try {
      val method = Class.forName("org.apache.spark.sql.Encoders").getMethod("row", classOf[StructType])
      Some(method.invoke(null, schema).asInstanceOf[Encoder[Row]])
    } catch {
      case _: ClassNotFoundException | _: NoSuchMethodException | _: NoClassDefFoundError => None
    }
  }

  private def catalystExpressionEncoder(schema: StructType): Option[Encoder[Row]] = {
    try {
      val method = Class
        .forName("org.apache.spark.sql.catalyst.encoders.ExpressionEncoder")
        .getMethod("apply", classOf[StructType])
      Some(method.invoke(null, schema).asInstanceOf[Encoder[Row]])
    } catch {
      case _: ClassNotFoundException | _: NoSuchMethodException | _: NoClassDefFoundError => None
    }
  }
}
