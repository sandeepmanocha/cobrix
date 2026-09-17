# Format coverage matrix

Status: `done` = PASS, `partial` = related but not their exact options, `pending` = not run, `fail` = job ran and failed (reader left as-is).

Local = `sbt sparkCobolServerless/test`.
Databricks = Serverless jobs on a configured workspace.

## Record formats

| Format | Meaning | Local fixture / spec | Local | Databricks | Pending |
|--------|---------|----------------------|-------|------------|---------|
| **F** | Fixed length | `data/test1_*`, `Test1FixedLengthServerlessSpec` | done | done (D1 golden, D3 2GB/30GB) | — |
| **FB** | Fixed blocked | Treated as F in Cobrix | done (as F) | done (as F) | Explicit `record_format=FB` job |
| **V** | Variable + RDW | `data/test17`, `Test17HierarchicalServerlessSpec` | done | done (`cobrix_serverless_v`, 951 rows; Connect nullability aligned) | — |

| **VB** | Variable + BDW+RDW | `Test29BdwServerlessSpec` + 1000-row LE + fake BE `adjustment=-4` | done | done (LE 1000 + Citi-style BE `-4` fake) | Real Citi bytes |
| **D** | ASCII / text records | fake `ascii_d` via `record_format=D`; also `is_text` test40 | done (`record_format=D` fake) | done (`cobrix_citi_option_packs` ascii_d, 3 rows) | — |
| **D2** | ASCII variant | not in serverless specs | pending | pending | Fixture + job |

## Citi cobrixOptions packs

| Pack | Distinguishing options | Local | Databricks | Pending |
|------|------------------------|-------|------------|---------|
| **F/FB pack** | `F`, `ebcdic_code_page=cp037_extended`, `string_trimming_policy=none`, `comments_ubound=120` | done (test1 + those options) | done (`cobrix_citi_f_pack`, 10 rows) | Real Citi copybook |
| **V pack** | `V`, RDW BE, `rdw_adjustment=-4`, `cp1047`, `variable_size_occurs=true` | done (fake V+OCCURS) | done (fake, 3 rows) | Real Citi V file |
| **VB pack** | `VB`, RDW+BDW BE, both `*_adjustment=-4` | done (fake) | done (fake, 20 rows) | Real Citi VB file |
| **Multisegment VB** | `segment_field` + maps | done (fake VB+segment_id) | done (fake, 4 rows) | Real 5-map VB |
| **D pack** | `record_format=D`, `collapse_root`, trim `none` | done (`record_format=D` fake; no trim none on that file) | done (3 rows) | trim none on D |

## Also covered (not a Citi pack)

| Case | Local | Databricks |
|------|-------|------------|
| Nested groups (like `COMPANY.SHORT_NAME`) | done (`test1`) | done (D1) |
| V multisegment (`test4`, not VB) | done | done (`cobrix_serverless_v_multi`, 1000 rows) |
| Variable OCCURS on **F** | done (`test21`) | done (D2) |
| ASCII `is_text` + `encoding=ascii` | done (`test40`) | done (`cobrix_serverless_ascii`, 3 rows) |
| Nested `PERSON.NAME.FIRST-NAME` (fake) | done | done (3 rows; keep_original) |
| Control file beside data | fail (`.ctl` parsed as COBOL) | fail (same) |
| Code page CP037 | done (`test9`) | pending |
| Code page CP1047 | done (`test9` + `cp1047`) | done (`cobrix_citi_cp1047`) |
| VB 1000-row LE Test29 | done (`Test29Vb1000ServerlessSpec`) | done (`cobrix_serverless_vb`) |
| Type variety | done (`test6`) | pending |
| Classic `format("cobol")` vs Serverless | n/a | done (D4, `test1` only) |
| `format("cobol")` banned on Serverless | n/a | done (D5) |

## Not started

| Case | Notes |
|------|--------|
| Person file1 vs file2 (1000 rows) | Fake nested `NAME` pack done (3 rows). Real 1000-row file1 vs file2 not run. |
| Control file (same name as data) | **Tried:** directory with `person.dat` + `person.ctl`. Reader treats `.ctl` as COBOL and throws (size not divisible by record length). Not fixed. |
| Unique filename each send | Not in fixtures |
| GPG / `.gz` | Explicitly **rejected** on Serverless NIO path |

Update this file when a row moves from pending → done.
