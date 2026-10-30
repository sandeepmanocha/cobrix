# spark-cobol-serverless

Read COBOL / EBCDIC files on **Databricks Serverless**.

`spark.read.format("cobol")` does not work there. This library is the replacement. Copybook decode is the same Cobrix. You call a method instead of a data source.

## Download and use (Citi)

Take a JAR from [`artifacts/`](artifacts/). No sbt or Maven required.

| File | What |
|--|--|
| `spark-cobol-serverless_2.13-maven-bundle.jar` | Maven-built Serverless fat JAR (preferred) |
| `spark-cobol-serverless_2.13-sbt-bundle.jar` | Same reader, built with sbt |

Both are Scala **2.13** for Serverless environment version **5**. Same function, not the same bytes.

Do **not** use `databricks-examples-bundle.jar`. That is lab helpers, not the product.

1. Put the JAR, **copybook**, and **data** on a Unity Catalog Volume (`/Volumes/catalog/schema/volume/...`).
2. Attach **only** the serverless bundle as a Java dependency on Serverless env 5.
3. Point the reader at the **data file or data directory**, not a mixed folder of control files.
4. If the feed is gzip or GPG, decrypt / uncompress first. Then land the plain bytes on the Volume.

A `.ctl` (or any non-data file) next to the data currently fails the read. Point `--data` at the data file, or keep control files out of that directory.

### Notebook / Scala

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

df.write.mode("overwrite").saveAsTable("catalog.schema.cobol_table")
```

Option names are the **same as classic Cobrix** (`copybook`, `copybook_contents`, `record_format`, `schema_retention_policy`, `ebcdic_code_page`, RDW/BDW flags, segment maps, and so on). After `read`, it is a normal DataFrame.

### JAR job

Main class: `za.co.absa.cobrix.spark.cobol.serverless.CobolServerlessMain`

```
--data /Volumes/.../data
--copybook /Volumes/.../copybook.cob
--schema_retention_policy collapse_root
--out catalog.schema.table
```

Do not register a `cobol` data source.

### Do not

| | |
|--|--|
| `spark.read.format("cobol")` on Serverless | Banned. Use `CobolServerlessReader.read`. Classic clusters can still use `format("cobol")`. |
| Hadoop / DBFS paths the Volume does not mount | Use `/Volumes/...`. |
| gzip / GPG as the Spark input | Unpack before the job. |
| `.ctl` beside the data | Treats the control file as COBOL and fails. |
| `cache` / `persist` / `checkpoint` on this DataFrame | Not supported on Serverless. |
| `spark.stop()` / `sys.exit()` in job code | Serverless owns the session. |

If a write fails with `File_Id is nullable`, the reader already accounts for Spark Connect. Do not change the copybook or Cobrix core to “fix” it. If it still happens, the JAR is old.

### Classic vs Serverless

| | Classic cluster | Serverless |
|--|--|--|
| API | `spark.read.format("cobol")` | `CobolServerlessReader.read` (this JAR) |
| JAR | `spark-cobol` | `artifacts/spark-cobol-serverless_2.13-*-bundle.jar` |
| Options | Cobrix names | Same names |
| Copybook decode | `cobol-parser` | Same library, untouched |
| Files | DBFS / Hadoop paths | UC Volume (`/Volumes/...`) |
| gzip / GPG | Classic Hadoop codecs | Rejected — unpack first |

Files larger than 2 GB are supported (ranged NIO).

## Build from source (optional)

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@11
sbt "sparkCobolServerless/assembly"
# copy: spark-cobol-serverless/target/scala-2.13/*-bundle.jar
#    -> artifacts/spark-cobol-serverless_2.13-sbt-bundle.jar

mvn -pl spark-cobol-serverless -am package -DskipTests
# copy: spark-cobol-serverless/target/spark-cobol-serverless_2.13-*-bundle.jar
#    -> artifacts/spark-cobol-serverless_2.13-maven-bundle.jar
```

Lab tests and Databricks jobs: [`examples/databricks-examples`](../examples/databricks-examples).
