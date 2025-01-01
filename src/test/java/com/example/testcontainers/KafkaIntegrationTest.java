package com.example.testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
public class KafkaIntegrationTest {

  public static final String CONFLUENT_PLATFORM_VERSION = "7.5.1";

  private static final ConfluentKafkaContainer KAFKA_CONTAINER;
  private static final GenericContainer<?> SCHEMA_REGISTER_CONTAINER;

  static {

    final Network network = Network.newNetwork();

    KAFKA_CONTAINER =
        new ConfluentKafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:" + CONFLUENT_PLATFORM_VERSION))
            .withNetwork(network)
            .withNetworkAliases("kafka")
            .withEnv("KAFKA_BROKER_ID", "1")
            .withEnv("KAFKA_LISTENERS", "PLAINTEXT://0.0.0.0:9092,BROKER://0.0.0.0:9093,CONTROLLER://0.0.0.0:9094")
            .withEnv("KAFKA_ADVERTISED_LISTENERS", "PLAINTEXT://kafka:9092,BROKER://kafka:9093,CONTROLLER://kafka:9094")
            .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "PLAINTEXT:PLAINTEXT,BROKER:PLAINTEXT,CONTROLLER:PLAINTEXT")
            .withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER")
            .withEnv("KAFKA_CONTROLLER_LISTENER_NAMES", "CONTROLLER")
            .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
            .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
            .withExposedPorts(9092, 9093, 9094)
            .waitingFor(Wait.forListeningPorts(9092));
    KAFKA_CONTAINER.start();

    SCHEMA_REGISTER_CONTAINER = new GenericContainer<>(
        DockerImageName.parse("confluentinc/cp-schema-registry:" + CONFLUENT_PLATFORM_VERSION))
        .withNetwork(network)
        .withExposedPorts(8081)
        .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
        .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
        .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS",KAFKA_CONTAINER.getBootstrapServers())
        .dependsOn(KAFKA_CONTAINER)
        .waitingFor(Wait.forHttp("/subjects").forStatusCode(200)
            .withStartupTimeout(Duration.ofSeconds(5)));
    SCHEMA_REGISTER_CONTAINER.start();
  }

  @Test
  void shouldHaveHealthyContainers() {
    assertThat(KAFKA_CONTAINER.isHealthy()).isTrue();
    assertThat(SCHEMA_REGISTER_CONTAINER.isHealthy()).isTrue();
  }

  @AfterAll
  static void afterAll() {
    if (SCHEMA_REGISTER_CONTAINER != null) {
      SCHEMA_REGISTER_CONTAINER.stop();
    }
    if (KAFKA_CONTAINER != null) {
      KAFKA_CONTAINER.stop();
    }
  }
}