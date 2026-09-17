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
path = sys.argv[1]
try:
    spark.read.format("cobol").load(path).count()
    raise SystemExit("FAIL: format('cobol') succeeded on Serverless")
except Exception as exc:
    message = str(exc)
    ok = (
        "DATA_SOURCE_NOT_FOUND" in message
        or "Failed to find data source" in message
        or "did not find a registered data source named cobol" in message.lower()
    )
    if not ok:
        raise
    print("FAIL_CLOSED_OK")
    print(message.splitlines()[0])
