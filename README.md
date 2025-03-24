**Scala Learning Outline for a Lead Data Role**

1. **Core Scala Fundamentals**
    - **Syntax**: variables (vals vs. vars), control structures (if/else, for-comprehensions).
    - **Data Types & OOP**: classes, traits, objects, case classes.
    - **Basic Collections**: Lists, Maps, Sets (mutable/immutable).

2. **Functional Programming in Scala**
    - **Immutability**: advantages, avoiding side effects.
    - **Higher-Order Functions**: map, flatten, flatMap, filter, reduce.
    - **Pattern Matching**: exhaustive checks, case classes in match.
    - **Partial Functions** & **Currying**: advanced function usage.

3. **Scala Collections & Transformations Deep Dive**
    - **Map vs. FlatMap**: how and when to use each, typical data transformation patterns.
    - **Flatten**: dealing with nested lists or optional values.
    - **Fold & Reduce**: aggregations and custom accumulations.
    - **Streams & Lazy Evaluations** (basic concepts).

4. **Concurrency & Parallelism**
    - **Futures & Promises**: asynchronous programming.
    - **Akka** basics: actors, message passing (optional but useful for large-scale data flows).
    - **Threading & Parallel Collections**: parallel operations on large datasets.

5. **Spark with Scala**
    - **RDD vs. DataFrame vs. Dataset** APIs.
    - **Transformations & Actions** in Spark.
    - **Structured Streaming**: real-time data ingestion.
    - **Optimizations**: partitioning, caching, understanding Spark’s execution model (Catalyst, Tungsten).

6. **Data Engineering & Architecture**
    - **ETL/ELT Pipelines**: batch vs. streaming design patterns.
    - **Data Lake / Data Warehouse** principles (e.g., Delta, Iceberg).
    - **Orchestration Tools**: Airflow, Argo, or other scheduling solutions.
    - **Testing & Observability**: ScalaTest, logging, monitoring pipeline health.

7. **Leadership & Strategy**
    - **Architecting Scalable Systems**: distributed data pipelines, fault tolerance.
    - **Team Mentoring & Code Reviews**: best practices, style guides, performance tips.
    - **Stakeholder Communication**: translating data requirements into technical solutions.
    - **Roadmap & Innovation**: continuous improvement, evaluating new technologies.

---

| **Scala Outline**                                          | **Project Stages**                                                                                | **Example Overlaps**                                                                                                          |
|------------------------------------------------------------|----------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| **1. Core Scala Fundamentals**                             | **Stage 1: Define Data Schemas & Basic Ingestion**                                                 | Basic syntax, working with case classes / `StructType`, reading CSV/JSON into Spark DataFrames.                                |
| **2. Functional Programming in Scala**<br/>(map, flatMap…) | **Stage 2: Apply Transformations**<br/>**Stage 3: Dimension Table**                                | Filtering/transforming DataFrames using higher-order functions (map, filter), cleaning data, joining dimension tables.         |
| **3. Spark with Scala**<br/>(DataFrame API, Datasets)      | **Stage 4: Fact Table Creation**<br/>**Stage 5: Aggregation**<br/>**Stage 6: Incremental Updates** | Creating transaction-level fact tables, performing groupBy/agg for daily metrics, merges/upserts with Iceberg or Delta.        |
| **4. Data Architecture & Best Practices**                  | **Stage 3: Dimension Table**<br/>**Stage 4 & 5**<br/>**Stage 6 & 7**                                | Table design (SCDs, partitioning), handling incremental loads, testing pipelines, ensuring data quality.                       |
| **5. Concurrency & Streaming (optional in advanced)**      | **Stage 6: Incremental Updates**                                                                  | Potential use of Spark Structured Streaming or concurrency strategies for real-time ingestion.                                 |
| **6. Testing & Observability**                             | **Stage 7: Testing & Validation**                                                                  | ScalaTest integration, verifying transformations, checking row counts and schema correctness.                                  |
| **7. Optimization & Performance**                          | **Stage 8: Optimization**                                                                          | Tuning Spark jobs, partitioning, caching, broadcasting small tables, analyzing performance with Spark UI.                      |
| **8. Leadership & Strategy**                               | **Stage 9: Deployment & Leadership**                                                               | Final architecture, code reviews, CI/CD pipeline, team standards, roadmap for iterative improvements.                          |

---

## **1) Infrastructure & Initial Setup**

**1.1 AWS Glue & S3 (Iceberg)**
- Create or confirm you have a **Glue Data Catalog** (e.g., `myGlueCatalog`), with an S3 warehouse path (`s3://my-iceberg-warehouse`).
- Ensure you have correct **IAM roles** for reading/writing S3 and calling Glue APIs.
- **S3 Buckets**:
   1. `s3://my-company-upstreams` (for raw/upstream data).
   2. `s3://my-iceberg-warehouse` (for Iceberg table data).
   3. Possibly `s3://my-company-enriched` if you store intermediate outputs as well.

**1.2 Airflow Setup**
- Deploy Airflow in a container or on EC2/Kubernetes.
- Maintain a **DAG repository** (Git) for versioned DAGs.
- Configure **Spark operators** or custom scripts so Airflow can do `spark-submit` calls to an EMR or Spark-on-K8s cluster.

**1.3 Environment Variables & .conf**
- For staging (`stg`) vs. production (`prd`), define separate `.conf` files:
   - `paths.conf` with keys like:
      - `upstreams.trans_table` = "myGlueCatalog.stg.transactions_raw"
      - `upstreams.merch_table` = "myGlueCatalog.stg.merchants_dim"
      - or different paths for S3 raw.
   - `spark.conf` with environment-based spark settings (some might differ by environment).
- Store these `.conf` in a **central config repository** or read them at runtime from S3 / secret manager.

---

## **2) Phase I: Upstream Data Ingestion into Iceberg**

**2.1 Upstream Data Sources**
- **Identify** each upstream: e.g., Payment Gateway logs, Merchant registration system, etc.
- Data might arrive in S3 (CSV/Parquet) or directly via an API -> processed by a script -> stored in an Iceberg table.

**2.2 Ingestion Logic**
- A **Spark job** (Scala or Python) that:
   1. Reads raw data from S3 paths (or an external DB).
   2. Applies minimal transformations (e.g., reformat columns, coerce data types).
   3. Writes to **Iceberg** (Glue-based) tables:
      - `myGlueCatalog.stg.transactions_raw`
      - `myGlueCatalog.stg.merchants_dim`
- This job is triggered by **Airflow** daily (or as needed).

**2.3 Configuration**
- `.conf` example:
  ```
  env = "stg"
  transactions.rawTable = "myGlueCatalog.stg.transactions_raw"
  merchants.rawTable = "myGlueCatalog.stg.merchants_dim"
  s3InputPath = "s3://my-company-upstreams/payment-gateway/<date>/*.parquet"
  ...
  ```
- The job uses these properties to locate input paths & output tables.

**2.4 Airflow DAG**
- **DAG**: `upstreams_ingestion_dag` with tasks:
   - `spark_ingest_transactions` -> calls `spark-submit ingest_upstreams.jar --config stg.conf`
   - `spark_ingest_merchants` -> calls the same or a separate jar, union or separate flow.

---

## **3) Phase II: Transaction Enrichments Engine**

**3.1 Business Logic**
- This engine **consumes** the upstream raw data from `myGlueCatalog.stg.transactions_raw` / `myGlueCatalog.stg.merchants_dim`.
- **Filters** out `DECLINED` transactions, ignores merchants not in `ACTIVE` status, etc.
- Joins with additional reference data (e.g., currency rates, user dimension) if needed.
- Applies transformations (convert amounts, fix timestamps) and advanced business logic (fraud checks, custom field derivations).

**3.2 IndexMaker Logic**
- A dedicated code module: `IndexMaker.scala` or `IndexMaker.py`, with methods that:
   1. Input: **DataFrames** of transactions, merchants, plus any dimension tables.
   2. For each **field** in the final index, apply custom logic:
      - E.g., `merchant_category`, `risk_flag`, normalized columns (lowercased addresses), etc.
   3. Outputs a **complex** final DataFrame representing your “Index” used for searching / analytics.

**3.3 Writing the Output**
- Writes final index to `myGlueCatalog.stg.index_payments` or `s3://my-company-enriched/index/payments/`.
- Possibly store it as **Iceberg** for ACID & time-travel or partition by date/merchant for performance.

**3.4 Configuration**
- Another `.conf` file: `enrich.conf`:
  ```
  env = "stg"
  input.transactionsTable = "myGlueCatalog.stg.transactions_raw"
  input.merchantsTable = "myGlueCatalog.stg.merchants_dim"
  output.indexTable = "myGlueCatalog.stg.index_payments"
  merchantActiveField = "status" 
  merchantActiveValue = "ACTIVE"
  filterDeclined = true
  ...
  ```
- This can vary by environment (prd has a different DB name).

**3.5 Airflow DAG**
- **DAG**: `enrichment_dag` with tasks:
   - `spark_enrich_transactions` -> calls `spark-submit enrichment_engine.jar --config stg.conf`
   - Final “index” is available for BI or search queries.

---

## **4) Orchestration & Scheduling**

**4.1 Airflow DAG Flow**
- Potentially a **two-DAG** system or a single DAG with multiple tasks in sequence:
   1. `IngestUpstreams` (fetch raw data -> store in Iceberg)
   2. `TransactionEnrichment` (join, filter, transform)
   3. (Optional) `IndexMaker` or part of the same enrichment step

**4.2 Dependencies**
- The `TransactionEnrichment` step should **wait** for the upstream ingestion to complete.
- Airflow can pass the same `.conf` or environment variables.
- You can store job success/failure in a small meta table or rely on Airflow states.

---

## **5) Deploying Code & Spark Config**

**5.1 Build & Publish**
- Use **SBT** or **Maven** (for Scala) to produce a **fat JAR**.
- Store the JAR in an artifact repo or S3.
- Airflow tasks reference that JAR (like `spark-submit --class com.example.pipeline.EnrichmentEngine s3://.../pipeline.jar`).

**5.2 Environment-based Spark Config**
- In **Airflow** or the shell script, you supply environment-specific flags:
   - `--conf spark.yarn.queue=staging` vs. `--conf spark.yarn.queue=production`
   - `--conf spark.someKey=someVal` if you want different shuffle partitions in prod.

**5.3 Infrastructure**
- Typically an **EMR** or **K8s** cluster for Spark.
- Make sure your cluster has the necessary JAR dependencies for **Iceberg** and **Glue** support.

---

## **6) Production-Level Readiness**

**6.1 Logging & Monitoring**
- Each Spark job logs to **CloudWatch** or **Datadog**.
- Key metrics: row counts, read/writes, partition counts, job duration, memory usage.

**6.2 Testing**
- For the **IndexMaker** logic, create a small test dataset with known transactions.
- Use **ScalaTest** to ensure transformations yield expected results: e.g., `DECLINED` transactions are dropped, inactive merchants are filtered out, etc.

**6.3 Governance**
- Enforce **versioning** on the Iceberg tables (time-travel for debugging).
- Tag S3 buckets for cost monitoring.
- If dealing with sensitive data (PII, payment info), add encryption at rest, secure IAM roles, and possibly Lake Formation for column-level access control.

---

## **7) Summary Project Flow**

1. **Phase 1**: Upstreams -> `spark-submit ingest_data.jar` -> writes raw data to `myGlueCatalog.stg.transactions_raw` and `myGlueCatalog.stg.merchants_dim`.
2. **Phase 2**: `TransactionEnrichment` job -> reads those tables + reference dims -> filters business rules + advanced transforms -> writes to `myGlueCatalog.stg.index_payments`.
3. **Airflow** orchestrates both phases, logs job success/failure.
4. **Deployment**: Use a single “pipeline.jar” or multiple modules. Distinguish `stg.conf` vs. `prd.conf` for environment-specific table names/paths.
5. **End**: A final “index” table is available for dashboards, search indexing, or further analytics, all managed via Iceberg for easy merges/time-travel.


**Transactions (Upstream) – `myGlueCatalog.stg.transactions_raw`**

| **Field**         | **Type**             | **Description**                                                | **Example**           |
|-------------------|----------------------|----------------------------------------------------------------|-----------------------|
| transaction_id    | STRING               | Unique payment identifier                                      | `"txn_a1b2c3"`        |
| user_id           | STRING (nullable)    | User reference (or null if guest)                              | `"U4499"`             |
| merchant_id       | STRING (nullable)    | Merchant reference                                             | `"M8822"`             |
| amount            | DECIMAL(12,2)        | Payment amount (precise currency storage)                      | `123.45`             |
| currency          | STRING(3)           | ISO currency code                                              | `"USD"`               |
| status            | STRING               | Payment status (`COMPLETED`, `DECLINED`, `PENDING`, `REFUNDED`)| `"DECLINED"`          |
| reason_code       | STRING (nullable)    | Extra info if `DECLINED`/`FAILED`                              | `"InsufficientFunds"` |
| payment_method    | STRING               | Method used (`CREDIT_CARD`, `PAYPAL`, etc.)                    | `"CREDIT_CARD"`       |
| transaction_ts    | TIMESTAMP           | Actual time of the payment                                     | `2025-03-10 09:15:00` |
| created_at        | TIMESTAMP           | Ingest/record creation time                                    | `2025-03-10 09:16:00` |
| ip_address        | STRING (nullable)    | Possibly used for fraud checks                                 | `"192.168.1.10"`      |
| device_id         | STRING (nullable)    | POS terminal or app device                                     | `"dev_001"`           |
| user_agent        | STRING (nullable)    | Browser/app info (for web analytics)                           | `"Mozilla/5.0..."`    |

---

**Merchants (Upstream) – `myGlueCatalog.stg.merchants_dim`**

| **Field**      | **Type**             | **Description**                                              | **Example**           |
|----------------|----------------------|--------------------------------------------------------------|-----------------------|
| merchant_id    | STRING               | Primary key matching transactions                           | `"M8822"`             |
| merchant_name  | STRING               | Display/legal entity name                                   | `"Acme Corp"`         |
| status         | STRING               | Merchant status (`ACTIVE`, `INACTIVE`, `SUSPENDED`, etc.)   | `"ACTIVE"`            |
| industry       | STRING               | Industry category (`Retail`, `Food Delivery`, etc.)         | `"Retail"`            |
| risk_score     | DECIMAL(3,1)        | Simple numeric risk metric                                  | `5.4`                 |
| country_code   | STRING(2)           | ISO country code                                            | `"US"`                |
| onboard_date   | DATE or TIMESTAMP   | Date/time merchant onboarded                                | `2023-01-15`          |
| extra_metadata | STRING (nullable)    | Additional freeform info                                    | `"salesRep=15"`       |

---

**Index (Enriched/Final) – `myGlueCatalog.stg.index_payments`**  
*(Produced in later enrichment phase, not directly from upstream.)*

| **Field**            | **Type**             | **Description**                                                          | **Example**               |
|----------------------|----------------------|--------------------------------------------------------------------------|---------------------------|
| transaction_id       | STRING               | Copied from raw (reference to payment record)                           | `"txn_a1b2c3"`            |
| merchant_id          | STRING               | From raw, used to join dimension                                        | `"M8822"`                 |
| user_id              | STRING               | From raw, or possibly null                                              | `"U4499"`                 |
| status               | STRING               | Possibly filtered (exclude `DECLINED`)                                  | `"COMPLETED"`             |
| amount               | DECIMAL(12,2)        | Final validated amount                                                  | `123.45`                 |
| merchant_name        | STRING               | Brought from `merchants_dim`                                            | `"Acme Corp"`             |
| merchant_active_flag | BOOLEAN or STRING    | Derived from dimension’s `status` (e.g. `ACTIVE` => `true`)             | `true`                    |
| combined_risk_level  | STRING (nullable)    | Merged logic from `risk_score` plus transaction attributes              | `"HIGH" or "LOW"`         |
| index_created_at     | TIMESTAMP            | Timestamp when the record was created in the index pipeline             | `2025-03-11 10:05:00`     |
| any_other_derived    | MAP<STRING,STRING> or STRING (nullable) | Additional business logic output (e.g. `{"source":"mobile","region":"US"}`) | `"{driver=bolt}"`        |
# Transactions-enrichments-systems-scala


```markdown
# Terraform Infrastructure for AWS (EMR, EKS, MSK)

This Terraform configuration sets up a unified AWS environment with a VPC, subnets, and **optional** EMR, EKS, and MSK clusters. Toggle each service on or off via variables.


## Prerequisites
1. **Terraform** >= 1.2  
2. **AWS CLI** or equivalent credentials  
3. **IAM Permissions** to create VPC, EC2, EMR, MSK, EKS, S3, etc.

## Usage
1. **Clone this repo**:

2. **Initialize and Plan**:
   ```bash
   terraform init
   terraform plan -var="create_emr=true" -var="create_eks=false" -var="create_msk=false"
   ```
3. **Apply**:
   ```bash
   terraform apply -var="create_emr=true" -var="create_eks=false" -var="create_msk=false"
   ```
    - This creates a VPC, subnets, and an EMR cluster. Set `create_eks` or `create_msk` to `true` if needed.

## Components
- **VPC + Subnets**: Public and private subnets with an Internet Gateway.
- **EMR** (optional): Spark/Hadoop cluster.
- **EKS** (optional): Managed Kubernetes cluster.
- **MSK** (optional): Apache Kafka cluster.

## Cleanup
When done, destroy all resources:
```bash
terraform destroy
```

---
```

Below is an updated script with a clear **usage** section at the top, along with an example on how to run it:

```bash
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
#   1) Build the fat jar with Gradle (shadowJar).
#   2) Upload the resulting JAR to s3://my-bucket/jars/.
#   3) Submit a Spark step to EMR cluster j-3ABCXYZ, using
#      com.payment.merchants.EnrichmentEngine as the main class.
#   4) Pass 'arg1 arg2' as additional arguments to Spark.
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
  --cluster-id "$CLUSTER_ID" \
  --steps Type=Spark,Name="EnrichmentStep",ActionOnFailure=CONTINUE,\
Args=[--class,"$MAIN_CLASS","$JAR_S3_PATH","$@"]

echo "=== EMR step submitted. Check the EMR console for progress. ==="
```

## How to Use This Script

1. **Make it executable**:
   ```bash
   chmod +x emr_deploy_and_submit.sh
   ```

2. **Run the script** with the required arguments:

   ```bash
   ./emr_deploy_and_submit.sh <CLUSTER_ID> <MAIN_CLASS> <S3_BUCKET_PATH> [spark-submit args...]
   ```

    - **CLUSTER_ID**: Your EMR cluster ID (e.g., `j-3ABCXYZ`).
    - **MAIN_CLASS**: The fully qualified main class of your Spark job (e.g., `com.payment.merchants.EnrichmentEngine`).
    - **S3_BUCKET_PATH**: The S3 path where you want to upload the jar (e.g., `s3://my-bucket/jars`).
    - **[spark-submit args...]**: Any additional arguments that your Spark job needs.

   **Example**:
   ```bash
   ./emr_deploy_and_submit.sh j-3ABCXYZ com.payment.merchants.EnrichmentEngine s3://my-bucket/jars \
       --driver-memory 4g --executor-memory 8g
   ```

   This will:
    1. Compile and create the fat jar via Gradle (`shadowJar`).
    2. Upload that jar to `s3://my-bucket/jars/`.
    3. Submit a Spark step to the EMR cluster `j-3ABCXYZ` using `com.payment.merchants.EnrichmentEngine` as the main class.(You can run any Main class,but this Enrichment)
    4. Pass the additional Spark configuration (`--driver-memory 4g --executor-memory 8g`) to Spark during execution.

Check the **EMR console** → **Steps tab** to see the job progress and logs.