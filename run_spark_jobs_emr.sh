#!/bin/bash
set -e

###############################################################################
# Usage:
#   ./emr_deploy_and_submit.sh <CLUSTER_ID> <MAIN_CLASS> <S3_BUCKET_PATH> [spark-submit args...]
#
# Example:
#   ./emr_deploy_and_submit.sh j-3ABCXYZ com.payment.merchants.EnrichmentEngine s3://my-bucket/jars \
#       arg1 arg2
#
#   This will:
#   1) Build the fat jar with Gradle (shadowJar). (for python, you need to run requirements.txt
#   2) Upload the resulting JAR to s3://my-bucket/jars/.
#   3) Submit a Spark step to EMR cluster j-3ABCXYZ, using
#      com.payment.merchants.EnrichmentEngine as the main class.
#   4) Pass 'arg1 arg2' as additional arguments to Spark.

#Note: You can then pass this script to your CICD or airflow jobs to run continuously
###############################################################################

if [ "$#" -lt 3 ]; then
  echo "Usage: $0 <CLUSTER_ID> <MAIN_CLASS> <S3_BUCKET_PATH> [spark-submit args...]"
  exit 1
fi

CLUSTER_ID="$1"
MAIN_CLASS="$2"
S3_BUCKET_PATH="$3"
shift 3  # shift off the first three positional arguments

echo "=== Starting EMR Deploy and Submit ==="
echo "CLUSTER_ID:      $CLUSTER_ID"
echo "MAIN_CLASS:      $MAIN_CLASS"
echo "S3_BUCKET_PATH:  $S3_BUCKET_PATH"
echo "Additional Args: $@"

# 1) Build the fat jar with ShadowJar
echo "=== Building fat jar with Gradle ==="
./gradlew shadowJar

# 2) Identify the jar file
JAR_FILE=$(find build/libs -name "*-all.jar" | head -n 1)
if [ -z "$JAR_FILE" ]; then
  echo "Error: Could not find a fat jar in build/libs. Did shadowJar run successfully?"
  exit 1
fi
echo "=== Found Jar: $JAR_FILE ==="

# 3) Upload the jar to S3
echo "=== Uploading Jar to $S3_BUCKET_PATH ==="
aws s3 cp "$JAR_FILE" "$S3_BUCKET_PATH/"

BASENAME=$(basename "$JAR_FILE")
JAR_S3_PATH="$S3_BUCKET_PATH/$BASENAME"

# 4) Submit the jar as a Spark step
echo "=== Adding EMR Spark Step to cluster: $CLUSTER_ID ==="

aws emr add-steps \
  --no-cli-pager \
  --cluster-id "$CLUSTER_ID" \
  --steps Type=Spark,Name="EnrichmentStep",ActionOnFailure=CONTINUE,\
Args=[--class,"$MAIN_CLASS","$JAR_S3_PATH","$@"]

echo "=== EMR step submitted. Check the EMR console for progress. ==="


# s3://mentorhub-emr-code-run/data/transactions-enrichment-systems-0.1.0-all.jar
# s3://mentorhub-emr-code-run/data//transactions-enrichment-systems-0.1.0-all.jar

