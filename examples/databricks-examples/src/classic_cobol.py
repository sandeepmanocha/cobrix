#
# Copyright 2018 ABSA Group Limited
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import sys

from pyspark.sql import SparkSession

spark = SparkSession.builder.getOrCreate()
data_path = sys.argv[1]
copybook_path = sys.argv[2]
out_table = sys.argv[3]

df = (
    spark.read.format("cobol")
    .option("copybook", copybook_path)
    .option("schema_retention_policy", "collapse_root")
    .load(data_path)
)
df.write.mode("overwrite").saveAsTable(out_table)
print(f"OUTPUT_TABLE={out_table}")
print(f"ROW_COUNT={spark.table(out_table).count()}")
