# Cobrix Serverless — detailed file changes

Companion to `HIGH_LEVEL_CHANGE.md`. Layout after the split:

- Library: `spark-cobol-serverless/src/main`
- Tests, DAB, spikes, Python jobs: `examples/databricks-examples/`

Branch: `feature/spark-cobol-serverless`, based on Cobrix `2796c0b5`.
This document describes the complete branch diff.

**Not edited:** `cobol-parser/**`, classic `spark-cobol` data source (`DefaultSource`, `CobolRelation`, `CobolScanners`, Hadoop `FileStreamer`). Decode is reused via `dependsOn(sparkCobol)`.

**Deleted:** `spark-cobol-serverless/.../HadoopConfSupport.scala` — Hadoop `Configuration` is not available on Serverless.

Paths below are under `spark-cobol-serverless/` unless noted.

**Jump:** [test code](#test-code) · [examples](#examples-not-a-new-examplestree) · [Databricks Asset Bundles](#databricks-asset-bundles)

---

## Root build (only Cobrix files outside the new module)

| File | Why |
|------|-----|
| `build.sbt` | New `sparkCobolServerless` project, aggregated with the rest of Cobrix. Scala 2.13 only. `dependsOn(sparkCobol)` so readers/`CobolSchema` are reused. Hadoop-free assembly: empty shade rules, drop slf4j (Spark kernel provides it). Databricks Maven proxy resolver so sbt can resolve on this laptop. |
| `project/Dependencies.scala` | `SparkCobolServerlessDependencies` = same Spark SQL (Provided) + test deps as spark-cobol. No extra Hadoop client. |
| `project/plugins.sbt` | Databricks Maven proxy. Plugin bumps (`sbt-header` 5.11.0, `sbt-assembly` 2.3.1, `sbt-scoverage` 2.0.11) because older ivy-only artifacts 404 on the proxy. Jacoco/ASM URLs also via the proxy. |

---

## Public API and job entry

| File | Why |
|------|-----|
| `src/main/.../CobolServerlessReader.scala` | Public `read(spark, path(s), options): DataFrame`. Parses options, builds classic readers, discovers files, dispatches F / V / text scans, then `SchemaAlign` so Spark Connect can `Dataset.to`. Not a data source. |
| `src/main/.../CobolServerlessMain.scala` | JAR `main` for Databricks jobs. `--data`, `--copybook`, extra options, optional `--out` table. No `spark.stop()`, no `sys.exit()`, no `.master()`. |

---

## Options, copybooks, fail-closed

| File | Why |
|------|-----|
| `src/main/.../ServerlessReaderBuilder.scala` | Same Cobrix parameter parser as classic. Loads copybooks via NIO `Files` (and `jar://` from classpath). Inline `copybook_contents` wins over a path (classic behavior). Builds `FixedLenNestedReader` / `VarLenNestedReader` / `FixedLenTextReader` **inside** partitions from serializable params. Rejects GPG keys and compressed extensions (`.gz`, `.bz2`, …) so we fail closed instead of decoding ciphertext as EBCDIC. |
| `src/main/.../SerializableReaderParams.scala` | Copybook text + options shipped into executors. Closures must not capture live Hadoop/Spark objects from the driver. |

---

## I/O (Hadoop → NIO)

Classic Cobrix uses Hadoop `FileStreamer`. Serverless Connect does not ship a usable Hadoop FS client for this path.

| File | Why |
|------|-----|
| `src/main/.../NioFileStreamer.scala` | `SimpleStream` over `FileChannel` ranged reads. `maximumBytes = -1` means to EOF; `0` is an empty range (CR-12: `0` used to mean “whole file” and empty VB splits re-read the file). Maps `dbfs:/Volumes/...` and `file:` URIs to `java.nio.file.Path`. |
| `src/main/.../InputFile.scala` | List path + length via Spark `binaryFile` **metadata only** (`select path, length`). Not a 2 GB whole-file load. Driver uses lengths to plan splits. |
| `src/main/.../HadoopConfSupport.scala` | **Deleted.** Serialized Hadoop `Configuration` is not on the Serverless client. |

---

## Splits and scans (RDD → Dataset)

Classic `CobolScanners` uses `sc.parallelize` / `sc.binaryRecords` / `sc.textFile`. Those APIs are banned on Connect.

| File | Why |
|------|-----|
| `src/main/.../ByteRangeSplit.scala` | Split descriptor: path, start offset, length, file id. Typed Dataset element instead of an RDD partition. |
| `src/main/.../FixedSplitPlanner.scala` | Fixed-length: uniform record-size splits from `InputFile` lengths. Throws if file size is not divisible by record size (this is what fails today when a `.ctl` sits next to data). |
| `src/main/.../VarLenIndexBuilder.scala` | Variable-length: `IndexGenerator` over `NioFileStreamer` on the driver, no RDD. Empty ranges use `maximumBytes = 0` (do not read). |
| `src/main/.../FixedDatasetScan.scala` | `createDataset(splits).flatMap`: open NIO range, `getRowIterator`, emit `Row`. |
| `src/main/.../VarLenDatasetScan.scala` | Same for V/VB using index entries + NIO. |
| `src/main/.../TextDatasetScan.scala` | ASCII / `is_text` / `record_format=D`: `spark.read.text` + typed flatMap (not `sc.textFile`). |
| `src/main/.../RowEncoders.scala` | `Encoder[Row]` via reflection: `Encoders.row` on Spark 4 / Serverless, `ExpressionEncoder` on local Spark 3.5. One JAR for both. |
| `src/main/.../SchemaAlign.scala` | Spark Connect rebuilds `Dataset[Row]` with `nullable=true`. `df.to(cobolSchema)` then failed (`NULLABLE_COLUMN_OR_FIELD File_Id`). Keep Cobrix types/metadata; **never tighten** nullability. Does not change `cobol-parser`. |

---

## Databricks Asset Bundles

YAML under `spark-cobol-serverless/`. Root file is `databricks.yml`. Jobs are `resources/*.yml`. Python tasks live next to the Scala JAR.

| File | Why |
|------|-----|
| `databricks.yml` | Bundle `cobrix-serverless-reader`, env `'5'`. The caller supplies a workspace profile; Volume/JAR/catalog defaults are configurable. |
| `resources/serverless_job.yml` | Smoke read (`cobrix_serverless_reader` → test1 table) and NIO seek spike (`cobrix_seek_spike`). |
| `resources/classic_parity_job.yml` | Classic cluster job using `format("cobol")` + shaded `spark-cobol` JAR so Serverless output can be compared (D4). |
| `resources/validation_jobs.yml` | Rest of the harness: golden validate, test21 varlen, 2 GB / 30 GB generate+read, fail-closed, V (test17), V-multi (test4), ASCII (test40), VB 1000-row, Citi option-pack fakes. `fixture_mode` defaults to `skip_if_exists` so 30 GB is not rewritten on every run (CR-08). |

---

## Examples (not a new `examples/` tree)

Classic repo `examples/` still uses `format("cobol")` and was not touched. Serverless “examples” are the README snippet, the JAR `main`, and the two Python jobs:

| File | Why |
|------|-----|
| `README.md` | Caller API snippet |
| `src/main/.../CobolServerlessMain.scala` | JAR job example (`--data`, `--copybook`, `--out`) |
| `src/classic_cobol.py` | Classic ingest for D4 parity (`format("cobol")` on a classic cluster). |
| `src/fail_closed.py` | D5: `format("cobol").count()` **must** throw `DATA_SOURCE_NOT_FOUND` on Serverless. |

---

## Fixture generators (Volume data, not committed binaries)

| File | Why |
|------|-----|
| `src/main/.../spike/SeekSpikeMain.scala` | GO/NO-GO: ranged NIO read of 64 bytes on a Volume. Proved Hadoop was a dead end; NIO worked. |
| `src/main/.../spike/LargeFixtureGeneratorMain.scala` | Repeat test1 bytes to 2 GB / 30 GB on the Volume. `create` vs `skip_if_exists` (exact size required, CR-15). |
| `src/main/.../spike/VbFixtureGeneratorMain.scala` | 1000-row little-endian VB Test29 file for Serverless VB job. |
| `src/main/.../spike/CitiFakeFixtures.scala` | In-repo fake Citi-**style** files (not real Citi bytes): VB BE `adjustment=-4`, V+OCCURS+cp1047, VB+segment, ASCII D, nested NAME, control-file dir, F pack options. |
| `src/main/.../spike/CitiFakeFixtureMain.scala` | Write those fakes to a Volume (`create` / `skip_if_exists`). |
| `src/main/.../spike/TableValidationMain.scala` | Job helper: row count, golden prefix compare, table-vs-table `exceptAll`. |

---

## Test code

Golden specs reuse `data/testN_*`. They call `CobolServerlessReader.read`, not `format("cobol")`.

| File | Why |
|------|-----|
| `src/test/.../ServerlessParitySupport.scala` | Shared Spark session + golden row compare. Full count before `take` (CR-14). |
| `src/test/.../CobolSchemaServerlessSpec.scala` | Schema from copybook vs `test1` golden schema JSON. |
| `src/test/.../FileStreamerRangeSpec.scala` | NIO range bytes match `RandomAccessFile`. |
| `src/test/.../FixedSplitPlannerSpec.scala` | Fixed splits without Hadoop; divisibility. |
| `src/test/.../VarLenIndexBuilderSpec.scala` | Sparse index over NIO, empty-range handling. |
| `src/test/.../SchemaAlignSpec.scala` | Connect nullability: never tighten `File_Id` / nested fields. |
| `src/test/.../ServerlessReaderBuilderSpec.scala` | Inline copybook wins; `jar://`; GPG and `.gz` rejected (CR-13/16/17). |
| `src/test/.../LargeFixtureGeneratorSpec.scala` | `create` / skip / undersized-recreate / malformed-reject (CR-04/15). |
| `src/test/.../Test1FixedLengthServerlessSpec.scala` | F / test1 golden. |
| `src/test/.../Test6TypeVarietyServerlessSpec.scala` | COMP / IEEE754 / sign overpunch options. |
| `src/test/.../Test4MultisegmentServerlessSpec.scala` | V multisegment test4. |
| `src/test/.../Test17HierarchicalServerlessSpec.scala` | V + RDW test17. |
| `src/test/.../Test21VariableOccursServerlessSpec.scala` | F + variable OCCURS test21. |
| `src/test/.../Test29BdwServerlessSpec.scala` | VB BDW small LE fixture. |
| `src/test/.../Test29Vb1000ServerlessSpec.scala` | 1000-row VB generator + read. |
| `src/test/.../Test9CodePagesServerlessSpec.scala` | cp037 / cp1047 on test9. |
| `src/test/.../Test40AsciiServerlessSpec.scala` | `is_text` + ASCII test40. |
| `src/test/.../OptionsParityServerlessSpec.scala` | Fillers, record id, headers, input file name, record length (tests 7, 1b, 13, 20, 27). |
| `src/test/.../CitiOptionPacksServerlessSpec.scala` | Citi-style option packs on fakes. **7/8 PASS**; control-file dir FAIL (`.ctl` parsed as COBOL). Left unfixed pending discussion. |

---

## Docs

| File | Why |
|------|-----|
| `spark-cobol-serverless/README.md` | How to read a file, Serverless rules, File_Id note. |
| `HIGH_LEVEL_CHANGE.md` | Report 1: old vs new read. |
| `DETAILED_CHANGE.md` | This file. |
| `CODE_REVIEW.md` | Review findings CR-02…CR-18 and status. |
| `TEST_REPORT.md` | Append-only local + Databricks run log. |
| `FORMAT_COVERAGE.md` | F/V/VB/D and Citi pack matrix vs what is still pending (real Citi bytes, `.ctl` skip). |

---

## Intentionally unchanged

| Path | Why leave it |
|------|----------------|
| `cobol-parser/` | Decode is Spark-free and already correct. |
| `spark-cobol/` source | Classic `format("cobol")` must keep working on classic clusters. |
| `cobol-converters/` | Unrelated. |
| Real Citi copybooks/files | Not in-repo; fakes only prove option wiring. |

---

## Known gap tied to a file

`FixedSplitPlanner.scala` + directory discovery in `InputFile.scala`: a data dir that also contains `person.ctl` fails (“size NOT DIVISIBLE by RECORD SIZE”). Same locally and on Serverless. No reader change until that behavior is decided (skip by extension vs glob vs skip non-multiples).
