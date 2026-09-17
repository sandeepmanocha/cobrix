# Databricks examples (spark-cobol-serverless)

Same role as `examples/spark-cobol-app`, for the Serverless reader:

- Local Scala tests against `data/testN_*`
- Databricks Asset Bundle jobs on a configured workspace
- Volume fixture generators and table validators (not part of the library JAR)

Library code stays in `spark-cobol-serverless/`.

## Local tests

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@11
sbt "databricksExamples/test"
```

Results are appended to `TEST_REPORT.md` in this directory.

## JARs to upload

```bash
sbt "sparkCobolServerless/assembly"
sbt "databricksExamples/assembly"
```

Put both on the Volume:

- `spark-cobol-serverless-bundle.jar` — reader + `CobolServerlessMain`
- `databricks-examples-bundle.jar` — seek spike, fixture generators, `TableValidationMain`

## Databricks Asset Bundles

From this directory:

```bash
databricks bundle deploy -t dev --profile <profile>
databricks bundle run cobrix_serverless_reader -t dev --profile <profile>
```

Jobs are in `resources/*.yml`. Python tasks: `src/classic_cobol.py`, `src/fail_closed.py`.
