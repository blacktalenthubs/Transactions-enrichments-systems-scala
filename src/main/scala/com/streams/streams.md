
---

## 1. Create a `docker-compose.yml`

```yaml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.3.0
    container_name: local_zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.3.0
    container_name: local_kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: "zookeeper:2181"
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

---

## 2. Start the Containers

1. Save the above YAML to a file named `docker-compose.yml`.
2. In a terminal, navigate to the directory containing that file.
3. Run:

   ```bash
   docker-compose up -d
   ```

   This will spin up both Zookeeper and a single Kafka broker.

---

## 3. Verify It’s Running

1. Check container status:

   ```bash
   docker ps
   ```

2. You should see `local_zookeeper` and `local_kafka` running.
3. By default, Kafka will listen on `localhost:9092`.

---

## 4. Create a Test Topic and Produce/Consume

1. **Create a topic:**
   ```bash
   docker exec -it local_kafka bash
   kafka-topics --create \
     --topic test_topic \
     --bootstrap-server localhost:9092 \
     --partitions 1 \
     --replication-factor 1
   ```

2. **Start a producer** (within the Kafka container):
   ```bash
   kafka-console-producer --broker-list localhost:9092 --topic test_topic
   ```
   Type some messages, then hit **Enter**.

3. **Start a consumer** (in another terminal):
   ```bash
   docker exec -it local_kafka bash
   kafka-console-consumer --bootstrap-server localhost:9092 \
     --topic test_topic --from-beginning
   ```
   You should see your messages appear in the consumer.

---

## 5. Cleanup

- To stop the containers without removing them:
  ```bash
  docker-compose stop
  ```
- To remove them completely:
  ```bash
  docker-compose down
  ```

---
