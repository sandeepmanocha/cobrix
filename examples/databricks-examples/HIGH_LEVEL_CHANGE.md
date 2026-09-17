# Cobrix on Databricks Serverless — high-level change

Audience: how to read COBOL/EBCDIC files after this work. File-by-file diffs: `DETAILED_CHANGE.md`.

## Why anything changed

Databricks Serverless does not allow:

1. Custom Spark data sources — `spark.read.format("cobol")` is banned.
2. RDD / `SparkContext` APIs — that is how classic `spark-cobol` splits and scans files.

Citi’s existing Cobrix jobs therefore cannot run on Serverless as written. Decode logic (copybook parse, EBCDIC, COMP-3) was already Spark-free in `cobol-parser`. We added a **new read path** that uses DataFrame/Dataset APIs only.

`cobol-parser` and the classic `spark-cobol` data source were **not** rewritten. Classic clusters can still use `format("cobol")`.

## What we added

New in-repo module: `spark-cobol-serverless`.

- Public API: `CobolServerlessReader.read(spark, path, options)` → `DataFrame`
- Same option names as classic Cobrix (`copybook`, `copybook_contents`, `record_format`, `schema_retention_policy`, `ebcdic_code_page`, RDW/BDW flags, segment maps, …)
- Files on Unity Catalog Volumes (`/Volumes/...`) via Java NIO ranged reads (not Hadoop `FileSystem`, not a full-file `binaryFile` load)
- Splits and scans are typed Datasets, not RDDs
- Job JAR main: `CobolServerlessMain` (`--data`, `--copybook`, `--out`)

## Where to find it

Library: `spark-cobol-serverless/`. Tests, DAB, and job helpers: `examples/databricks-examples/` (same split as `spark-cobol` vs `examples/spark-cobol-app`).

| What | Path |
|------|------|
| **Test code (local)** | `examples/databricks-examples/src/test/scala/...` — `Test1…Test40*Spec`, `CitiOptionPacksServerlessSpec`, planner/index/NIO specs. Run: `sbt "databricksExamples/test"` |
| **Test fixtures** | Repo `data/testN_*`. Fakes: `examples/databricks-examples/src/main/.../spike/CitiFakeFixtures.scala` |
| **API example** | `spark-cobol-serverless/README.md` and this file |
| **Job example (Scala)** | `spark-cobol-serverless/.../CobolServerlessMain.scala` |
| **Job examples (Python)** | `examples/databricks-examples/src/classic_cobol.py`, `src/fail_closed.py` |
| **Databricks Asset Bundles** | `examples/databricks-examples/databricks.yml` + `resources/*.yml` |
| **DAB jobs** | `resources/serverless_job.yml`, `classic_parity_job.yml`, `validation_jobs.yml` |
| **Volume data (lab)** | `/Volumes/citi_lab/citi_demo/cobrix_poc/` |

Deploy/run a bundle job:

```bash
cd examples/databricks-examples
databricks bundle deploy -t dev --profile <profile>
databricks bundle run cobrix_serverless_reader -t dev --profile <profile>
```

Job resource names: `cobrix_serverless_reader`, `cobrix_seek_spike`, `cobrix_validate_smoke`, `cobrix_serverless_varlen`, `cobrix_serverless_large`, `cobrix_serverless_large_xl`, `cobrix_fail_closed`, `cobrix_validate_classic_parity`, `cobrix_serverless_v`, `cobrix_serverless_v_multi`, `cobrix_serverless_ascii`, `cobrix_serverless_vb`, `cobrix_citi_option_packs`.

## How you read files

### Old (classic cluster only)

```scala
val df = spark.read
  .format("cobol")
  .option("copybook", "/dbfs/path/copybook.cob")
  .option("schema_retention_policy", "collapse_root")
  .load("/dbfs/path/data")
```

This still works on a **classic** cluster with the `spark-cobol` JAR. On Serverless it fails (`DATA_SOURCE_NOT_FOUND`). That failure is intentional.

### New (Serverless, or anywhere the serverless JAR is on the classpath)

```scala
import za.co.absa.cobrix.spark.cobol.serverless.CobolServerlessReader

val df = CobolServerlessReader.read(
  spark,
  "/Volumes/catalog/schema/volume/data",
  Map(
    "copybook" -> "/Volumes/catalog/schema/volume/copybook.cob",
    "schema_retention_policy" -> "collapse_root"
  )
)
```

After `read`, the DataFrame is a normal Spark Dataset. Write, SQL, and downstream transforms are unchanged.

Python/SQL jobs typically call the JAR (`CobolServerlessMain`) rather than embedding Scala.

## What is the same vs different

| | Classic `format("cobol")` | Serverless reader |
|--|--|--|
| Copybook decode | `cobol-parser` | Same library, untouched |
| Option names | Cobrix options | Same names |
| How Spark is told to read | Registered data source | Library call / JAR |
| File listing + byte ranges | Hadoop FS + RDD | `binaryFile` metadata + NIO ranges + Dataset |
| Where data should live | DBFS / cloud FS Hadoop can see | UC Volume path (`/Volumes/...`) |
| Files larger than 2 GB | Supported | Supported (ranged NIO; proven 2 GB and 30 GB) |
| gzip / GPG on the reader | Classic Hadoop codecs (cluster-dependent) | Rejected — decrypt/uncompress **before** Spark |
| Control file (`.ctl`) next to data | Classic can ignore non-data files depending on glob | Currently treats `.ctl` as COBOL and **fails** (logged, not fixed) |

## Proven on Serverless (this lab)

- Fixed (F/FB): test1, including 2 GB / 30 GB repeats
- Variable OCCURS on F (test21)
- Variable + RDW (V, test17)
- Variable multisegment (test4, V)
- Variable + BDW+RDW (VB), including Citi-style BE `rdw/bdw_adjustment=-4` on fakes
- ASCII text (`is_text` and `record_format=D`)
- Code pages `cp037` / `cp1047` / `cp037_extended` (fixtures / option packs)
- Nested group names (`PERSON.NAME.FIRST-NAME`) on fakes
- Classic vs Serverless row parity on test1
- `format("cobol")` confirmed banned on Serverless

Fakes prove **options**. They are not real Citi bytes.

## What you still cannot do on Serverless

- Call `spark.read.format("cobol")`
- Point the reader at gzip/GPG and expect it to unpack
- Drop a `.ctl` control file in the same directory as data (fails today)
- Rely on Hadoop-only paths that are not mounted as `/Volumes`

## Bottom line

Keep classic Cobrix on classic compute. On Serverless, ship `spark-cobol-serverless`, put copybook + data on a Volume, and call `CobolServerlessReader.read` (or the JAR) with the **same options** you already use.
