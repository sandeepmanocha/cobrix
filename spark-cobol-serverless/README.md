# spark-cobol-serverless

Read COBOL / EBCDIC files on **Databricks Serverless**.

Serverless does not allow `spark.read.format("cobol")` and does not allow RDD APIs. This library is the replacement. Copybook decode is the same Cobrix parser. You call a method instead of a data source.

Tests, Databricks jobs, and sample scripts are in [`examples/databricks-examples`](../examples/databricks-examples).

---

## 1. Put the files on a Volume

Copybook and data must be on a Unity Catalog Volume, for example:

```
/Volumes/citi_lab/citi_demo/cobrix_poc/copybook.cob
/Volumes/citi_lab/citi_demo/cobrix_poc/data
```

Do not point this reader at gzip or GPG files. Uncompress or decrypt first, then put the plain bytes on the Volume.

---

## 2. Build the JAR (once)

On a Mac with JDK 11:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@11
sbt "sparkCobolServerless/assembly"
```

Upload the bundle JAR to the same Volume.

---

## 3. Read the file

**From Scala / a notebook** (JAR on the cluster):

```scala
import za.co.absa.cobrix.spark.cobol.serverless.CobolServerlessReader

val df = CobolServerlessReader.read(
  spark,
  "/Volumes/citi_lab/citi_demo/cobrix_poc/data",
  Map(
    "copybook" -> "/Volumes/citi_lab/citi_demo/cobrix_poc/copybook.cob",
    "schema_retention_policy" -> "collapse_root"
  )
)

df.write.mode("overwrite").saveAsTable("citi_lab.citi_demo.my_cobol_table")
```

Option names are the same as classic Cobrix (`copybook`, `copybook_contents`, `record_format`, `schema_retention_policy`, `ebcdic_code_page`, and so on).

**From a job**, main class:

`za.co.absa.cobrix.spark.cobol.serverless.CobolServerlessMain`

```
--data /Volumes/.../data
--copybook /Volumes/.../copybook.cob
--schema_retention_policy collapse_root
--out catalog.schema.table
```

Classic cluster (not Serverless) can still use:

```scala
spark.read.format("cobol").option("copybook", "...").load("...")
```

That call **fails on Serverless**. That is expected.

---

## 4. Rules to keep this working on Serverless

Do these, or the job will break on Serverless:

- Call `CobolServerlessReader.read` (or the JAR). Do not register a `cobol` data source.
- Use Dataset / DataFrame APIs only. No `SparkContext`, no `.rdd`, no `sc.parallelize`.
- Leave `cobol-parser` and classic `spark-cobol` alone. This module reuses them.
- Do not call `spark.stop()` or `sys.exit()` in `main`. Serverless owns the session.
- Do not `cache` / `persist` / `checkpoint` the COBOL DataFrame on Serverless.
- Do not use `dbutils` inside `flatMap` / `mapPartitions`.
- Local build: JDK 11, Scala 2.13. Serverless env version `5`.

If you change this module, keep Apache 2.0 headers and the same option names as classic Cobrix.

---

## 5. If you see `File_Id is nullable`

Spark Connect (Serverless) marks columns nullable even when Cobrix says `File_Id` / `Record_Id` are required. The values are still filled in. The reader already loosens nullability so `saveAsTable` works. You should not need to do anything.

If that error comes back, the align step in `CobolServerlessReader.read` is missing. Do not change Cobrix core schema to “fix” it.

---

## 6. Tests and Databricks jobs

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@11
sbt "databricksExamples/test"
```

Job bundle: [`examples/databricks-examples`](../examples/databricks-examples). After a run, append a line to `examples/databricks-examples/TEST_REPORT.md`.
