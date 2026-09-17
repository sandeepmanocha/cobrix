# spark-cobol-serverless code review

Date: 2026-09-03
Second pass: independent review of the same diff (no extra scope).
Scope: uncommitted serverless reader + Databricks harness (not `cobol-parser`).
Local tests: 26/26 PASS.

Harness files later moved to `examples/databricks-examples/` (library stays in `spark-cobol-serverless/`).

Track items below. Change **Status** to `done` when fixed. Do not delete rows.

## Findings

| ID | Status | Severity | Finding | Suggested fix |
|----|--------|----------|---------|----------------|
| CR-02 | done | should | `cobrix_serverless_large` and `_xl` are the same DAG, different paths/sizes | Kept one XL job |
| CR-04 | done | should | No tests for `create` / `skip_if_exists` | Added valid-skip, undersized-recreate, malformed-reject tests |
| CR-08 | done | must | Default `fixture_mode=create` rewrites 30GB on a default rerun | Job parameter now defaults to `skip_if_exists` |
| CR-12 | done | must | `maximumBytes=0` on `NioFileStreamer` means “read to EOF”. `VarLenIndexBuilder` / `VarLenDatasetScan` pass `0` for empty ranges, so a zero-length split reads the whole file | `-1` now means unbounded; `0` is an empty range |
| CR-13 | done | must | `hasCompressedFiles` is always `false`; compressed/GPG files are decoded as raw bytes | Explicitly reject GPG and compressed binary input |
| CR-14 | done | should | Golden tests `take(expected.length)` so extra rows still pass | Assert full row count before comparison |
| CR-15 | done | should | `skip_if_exists` accepts any file ≥ min bytes (stale/corrupt/unaligned) | Require exact generated size |
| CR-16 | done | should | Copybook load via `Paths.get` cannot resolve `jar://` | Load `jar://` from classpath resources |
| CR-17 | done | should | Inline `copybook_contents` plus a path concatenates both; classic Cobrix uses contents only | Inline contents now take precedence |
| CR-18 | done | should | `classic_cobol.py` and `fail_closed.py` missing Apache header | Added Apache headers |

Dropped after second pass (not worth tracking): helper extract (old CR-01), package rename (CR-03), Python layout/CLI style (CR-05), double `Files.size` (CR-06), YAML magic numbers alone (CR-07), sbt-assembly changelog (CR-09), sort-before-`exceptAll` (CR-10 — `exceptAll` is a bag compare), `.cursor/plans` (CR-11). LICENSE/NOTICE fat-jar discard (proposed CR-19) left out: Databricks job JAR, not a published artifact.

Accepted as-is:

- NIO streamer + Hadoop removal
- `RowEncoders` reflection (Serverless vs classic)
- `binaryFile` used for path+length only (30GB run already showed this is not a full-file load)

## Files changed and why

### Build

| File | Why |
|------|-----|
| `build.sbt` | Serverless module; Hadoop-free assembly |
| `project/Dependencies.scala` | Serverless deps = spark-cobol |
| `project/plugins.sbt` | Databricks Maven proxy; plugin bumps; sbt-assembly 2.3.1 |

### Reader

| File | Why |
|------|-----|
| `NioFileStreamer.scala` (new) | Volume ranged reads, no Hadoop |
| `InputFile.scala` (new) | List files via `binaryFile` metadata |
| `HadoopConfSupport.scala` (deleted) | Not available on Serverless |
| `CobolServerlessReader.scala` | Public API uses NIO discovery |
| `ServerlessReaderBuilder.scala` | Copybooks via NIO `Files` |
| `FixedSplitPlanner.scala` | Splits from `InputFile` |
| `VarLenIndexBuilder.scala` | Index from NIO lengths |
| `FixedDatasetScan.scala` | Fixed path uses `NioFileStreamer` |
| `VarLenDatasetScan.scala` | Varlen path uses NIO |
| `TextDatasetScan.scala` | Text path uses NIO |
| `RowEncoders.scala` | `Encoders.row` then `ExpressionEncoder` |
| `CobolServerlessMain.scala` | Job main; print table + count |

### Tests

| File | Why |
|------|-----|
| `FileStreamerRangeSpec.scala` | NIO range vs RandomAccessFile |
| `FixedSplitPlannerSpec.scala` | Splits without Hadoop |
| `VarLenIndexBuilderSpec.scala` | Index without Hadoop |
| `ServerlessParitySupport.scala` | Shared golden helpers |
| `OptionsParityServerlessSpec.scala` | Option parity |
| `Test4MultisegmentServerlessSpec.scala` | Fixture compare |
| `Test6TypeVarietyServerlessSpec.scala` | Type options |
| `Test29BdwServerlessSpec.scala` | BDW fixture |

### Databricks harness

| File | Why |
|------|-----|
| `databricks.yml` | Volume/JAR paths, `classic_jar_path`, `fixture_mode` |
| `resources/classic_parity_job.yml` | Classic `format("cobol")` + shaded spark-cobol JAR |
| `resources/validation_jobs.yml` (new) | D2/D3/D5/XL + validators |
| `spike/SeekSpikeMain.scala` | NIO seek spike |
| `spike/LargeFixtureGeneratorMain.scala` (new) | Repeat test1; `create` / `skip_if_exists` |
| `spike/TableValidationMain.scala` (new) | Count / golden / table compare |
| `src/fail_closed.py` (new) | D5: `format("cobol")` must fail |
| `src/classic_cobol.py` (new) | D4 classic ingest |
| `TEST_REPORT.md` | Run log |
