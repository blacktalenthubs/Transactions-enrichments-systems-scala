#!/bin/bash
set -e

if [ "$#" -lt 1 ]; then
  echo "Usage: $0 <main_class> [additional spark-submit args]"
  exit 1
fi

MAIN_CLASS="$1"
shift

./gradlew shadowJar

spark-submit \
  --class "$MAIN_CLASS" \
  --master "local[*]" \
  --driver-memory 4G \
  --conf spark.executor.memory=4G \
  build/libs/transactions-enrichment-systems-0.1.0-all.jar "$@"
