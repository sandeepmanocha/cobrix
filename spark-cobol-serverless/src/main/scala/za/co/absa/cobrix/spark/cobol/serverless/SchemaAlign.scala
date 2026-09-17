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

import org.apache.spark.sql.types._

object SchemaAlign {

  /**
   * Spark Connect rebuilds Dataset[Row] with nullable=true. Dataset.to(cobolSchema)
   * then rejects nullable -> non-nullable (File_Id, Record_Id, …). Keep Cobrix
   * types and metadata; never tighten nullability.
   */
  def forDataset(wanted: StructType, actual: StructType): StructType = {
    val byName = actual.fields.map(f => f.name -> f).toMap
    StructType(wanted.fields.map { w =>
      byName.get(w.name) match {
        case Some(a) =>
          w.copy(
            dataType = alignDataType(w.dataType, a.dataType),
            nullable = w.nullable || a.nullable
          )
        case None => w
      }
    })
  }

  private def alignDataType(wanted: DataType, actual: DataType): DataType =
    (wanted, actual) match {
      case (ws: StructType, as: StructType) =>
        forDataset(ws, as)
      case (wa: ArrayType, aa: ArrayType) =>
        ArrayType(alignDataType(wa.elementType, aa.elementType), containsNull = wa.containsNull || aa.containsNull)
      case (wm: MapType, am: MapType) =>
        MapType(
          alignDataType(wm.keyType, am.keyType),
          alignDataType(wm.valueType, am.valueType),
          valueContainsNull = wm.valueContainsNull || am.valueContainsNull
        )
      case _ =>
        wanted
    }
}
