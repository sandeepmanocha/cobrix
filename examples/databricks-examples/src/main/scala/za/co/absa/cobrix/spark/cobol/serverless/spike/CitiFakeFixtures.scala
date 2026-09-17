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

import java.nio.file.{Files, Path}

/** Synthetic Citi-style option packs. Not Citi production bytes. */
object CitiFakeFixtures {
  val VbBeRecords = 20
  val VOccursRecords = 3
  val VbSegRecords = 4
  val AsciiDRecords = 3
  val NestedNameRecords = 3

  val vbCopybook =
    """      01  R.
                03 A        PIC X(2).
      """

  val vOccursCopybook =
    """      01  REC.
                05 CNT PIC 9(1).
                05 ARR OCCURS 0 TO 3 TIMES DEPENDING ON CNT.
                   10 X PIC X(1).
      """

  val vbSegCopybook =
    """      01  REC.
                05 SEGMENT-ID PIC X(1).
                05 BODY PIC X(2).
      """

  val asciiDCopybook =
    """       01  RECORD.
           05  A1       PIC X(1).
           05  A2       PIC X(5).
           05  A3       PIC X(10).
    """

  val nestedNameCopybook =
    """      01  PERSON.
                05 ID PIC X(2).
                05 NAME.
                   10 FIRST-NAME PIC X(4).
                   10 LAST-NAME PIC X(4).
      """

  def writePack(dir: Path): Unit = {
    Files.createDirectories(dir.resolve("vb_be"))
    Files.createDirectories(dir.resolve("v_occurs"))
    Files.createDirectories(dir.resolve("vb_seg"))
    Files.createDirectories(dir.resolve("ascii_d"))
    Files.createDirectories(dir.resolve("nested_name"))
    Files.write(dir.resolve("vb_be/data.dat"), encodeVbBeAdj4(VbBeRecords))
    Files.write(dir.resolve("v_occurs/data.dat"), encodeVOccurs(VOccursRecords))
    Files.write(dir.resolve("vb_seg/data.dat"), encodeVbSegments(VbSegRecords))
    Files.write(dir.resolve("ascii_d/ascii.txt"), asciiDBytes())
    Files.write(dir.resolve("nested_name/person.dat"), encodeNestedName(NestedNameRecords))
    Files.write(dir.resolve("nested_name/person.ctl"), "CONTROL OK\n".getBytes("US-ASCII"))
    ()
  }

  def packComplete(dir: Path): Boolean = {
    Files.exists(dir.resolve("vb_be/data.dat")) &&
      Files.size(dir.resolve("vb_be/data.dat")) == encodeVbBeAdj4(VbBeRecords).length &&
      Files.exists(dir.resolve("v_occurs/data.dat")) &&
      Files.size(dir.resolve("v_occurs/data.dat")) == encodeVOccurs(VOccursRecords).length &&
      Files.exists(dir.resolve("vb_seg/data.dat")) &&
      Files.size(dir.resolve("vb_seg/data.dat")) == encodeVbSegments(VbSegRecords).length &&
      Files.exists(dir.resolve("ascii_d/ascii.txt")) &&
      Files.exists(dir.resolve("nested_name/person.dat")) &&
      Files.exists(dir.resolve("nested_name/person.ctl"))
  }

  /** One BDW+RDW block per record. Header stores payload+4 (Citi adjustment=-4). */
  def encodeVbBeAdj4(recordCount: Int): Array[Byte] = {
    val recBytes = 10
    val out = new Array[Byte](recordCount * recBytes)
    var i = 0
    while (i < recordCount) {
      val o = i * recBytes
      writeBeAdj4(out, o, 6)
      writeBeAdj4(out, o + 4, 2)
      out(o + 8) = (0xF0 + (i / 10) % 10).toByte
      out(o + 9) = (0xF0 + (i % 10)).toByte
      i += 1
    }
    out
  }

  def encodeVOccurs(recordCount: Int): Array[Byte] = {
    val chunks = Range(0, recordCount).map { i =>
      val payload = Array((0xF0 + i).toByte) ++ Array.fill(i)(0xC1.toByte)
      val rec = new Array[Byte](4 + payload.length)
      writeBeAdj4(rec, 0, payload.length)
      System.arraycopy(payload, 0, rec, 4, payload.length)
      rec
    }
    chunks.foldLeft(Array.empty[Byte])(_ ++ _)
  }

  def encodeVbSegments(recordCount: Int): Array[Byte] = {
    val recBytes = 11
    val out = new Array[Byte](recordCount * recBytes)
    var i = 0
    while (i < recordCount) {
      val o = i * recBytes
      writeBeAdj4(out, o, 7)
      writeBeAdj4(out, o + 4, 3)
      out(o + 8) = if (i % 2 == 0) 0xC3.toByte else 0xD7.toByte
      out(o + 9) = 0xF0.toByte
      out(o + 10) = (0xF0 + (i % 10)).toByte
      i += 1
    }
    out
  }

  def asciiDBytes(): Array[Byte] =
    "A12345HELLOWORLD\nB12345HELLOWORLD\nC12345HELLOWORLD\n".getBytes("US-ASCII")

  def encodeNestedName(recordCount: Int): Array[Byte] = {
    val rec = 10
    val out = new Array[Byte](recordCount * rec)
    var i = 0
    while (i < recordCount) {
      val row = f"$i%02dJOE SMITH".take(10)
      val bytes = row.getBytes("US-ASCII")
      System.arraycopy(bytes, 0, out, i * rec, rec)
      i += 1
    }
    out
  }

  private def writeBeAdj4(out: Array[Byte], offset: Int, payload: Int): Unit = {
    val stored = payload + 4
    out(offset) = (stored / 256).toByte
    out(offset + 1) = (stored % 256).toByte
    out(offset + 2) = 0
    out(offset + 3) = 0
  }
}
