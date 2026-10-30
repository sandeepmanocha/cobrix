# Databricks examples (spark-cobol-serverless)

Lab only. Same role as `examples/spark-cobol-app`, for the Serverless reader.

Customer JAR and how to read: [`spark-cobol-serverless/README.md`](../../spark-cobol-serverless/README.md).

This folder has:

- Local Scala tests against `data/testN_*`
- Databricks Asset Bundle jobs
- Volume fixture generators and table validators (`databricks-examples-bundle.jar` — not the product)

Library code stays in `spark-cobol-serverless/`.

## Local tests

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@11
sbt "databricksExamples/test"
```

Run log: [`tests.md`](tests.md).

## Lab JARs (not for Citi)

```bash
sbt "sparkCobolServerless/assembly"
sbt "databricksExamples/assembly"
```

Product JAR pickup: `spark-cobol-serverless/artifacts/`. For DAB jobs, also upload `databricks-examples-bundle.jar` (validators / generators only).

## Databricks Asset Bundles

From this directory:

```bash
databricks bundle deploy -t dev --profile <profile>
databricks bundle run cobrix_serverless_reader -t dev --profile <profile>
```

Jobs: `resources/serverless_job.yml`, `classic_parity_job.yml`, `validation_jobs.yml`.

Python: `src/classic_cobol.py`, `src/fail_closed.py`.

Job names: `cobrix_serverless_reader`, `cobrix_seek_spike`, `cobrix_validate_smoke`, `cobrix_serverless_varlen`, `cobrix_serverless_large`, `cobrix_serverless_large_xl`, `cobrix_fail_closed`, `cobrix_validate_classic_parity`, `cobrix_serverless_v`, `cobrix_serverless_v_multi`, `cobrix_serverless_ascii`, `cobrix_serverless_vb`, `cobrix_citi_option_packs`.

Lab volume: `/Volumes/citi_lab/citi_demo/cobrix_poc/`.

## Coverage (this lab)

Local = `sbt "databricksExamples/test"`. Databricks = Serverless jobs on a configured workspace.

| Case | Local | Databricks |
|--|--|--|
| F / FB (test1, including 2 GB / 30 GB) | done | done |
| Variable OCCURS on F (test21) | done | done |
| V + RDW (test17) | done | done |
| V multisegment (test4) | done | done |
| VB (Test29 LE + Citi-style BE `-4` fakes) | done | done |
| ASCII `is_text` (test40) and `record_format=D` | done | done |
| Citi option packs (fakes, not real Citi bytes) | done | done except `.ctl` |
| Nested `PERSON.NAME.FIRST-NAME` | done | done |
| Classic vs Serverless parity (test1) | n/a | done |
| `format("cobol")` banned on Serverless | n/a | done |
| `.ctl` beside data | fail (not fixed) | fail |
| gzip / GPG as input | rejected | rejected |
| D2, real Citi bytes | pending | pending |

Fakes prove **options**. They are not real Citi files.
