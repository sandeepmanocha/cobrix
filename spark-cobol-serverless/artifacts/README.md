# Deliverable JARs

Citi: download a JAR and use it. You do not need sbt or Maven.

| File | Built with |
|--|--|
| `spark-cobol-serverless_2.13-maven-bundle.jar` | Maven (preferred) |
| `spark-cobol-serverless_2.13-sbt-bundle.jar` | sbt |

Both are Scala 2.13 fat JARs for Databricks Serverless environment version 5. Same reader, not the same bytes.

How to attach and read: [`../README.md`](../README.md).

Do not use `databricks-examples-bundle.jar` from `examples/databricks-examples`. That is lab helpers.

Rebuild after reader changes:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@11
sbt "sparkCobolServerless/assembly"
cp spark-cobol-serverless/target/scala-2.13/spark-cobol-serverless_2.13_*-bundle.jar \
  spark-cobol-serverless/artifacts/spark-cobol-serverless_2.13-sbt-bundle.jar

mvn -pl spark-cobol-serverless -am package -DskipTests
cp spark-cobol-serverless/target/spark-cobol-serverless_2.13-*-bundle.jar \
  spark-cobol-serverless/artifacts/spark-cobol-serverless_2.13-maven-bundle.jar
```
