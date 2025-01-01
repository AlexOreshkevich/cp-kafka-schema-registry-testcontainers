package com.example.testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
public class KafkaIntegrationTest {

  public static final String CONFLUENT_PLATFORM_VERSION = "7.8.0";

  private static final KafkaContainer KAFKA_CONTAINER;
  private static final GenericContainer<?> SCHEMA_REGISTER_CONTAINER;

  static {

    final Network network = Network.newNetwork();

    KAFKA_CONTAINER =
        new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:" + CONFLUENT_PLATFORM_VERSION))
            .withNetwork(network)
            .withStartupTimeout(java.time.Duration.ofSeconds(30));

    SCHEMA_REGISTER_CONTAINER = new GenericContainer<>(
        DockerImageName.parse("confluentinc/cp-schema-registry:" + CONFLUENT_PLATFORM_VERSION))
        .withNetwork(network)
        .dependsOn(KAFKA_CONTAINER)
        .withExposedPorts(8081)
        .waitingFor(Wait.forHttp("/subjects").forStatusCode(200))
        .withStartupTimeout(Duration.ofSeconds(30))
        .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
        .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
        .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS",
            KAFKA_CONTAINER.getNetworkAliases().get(0) + ":9092");
  }

  @BeforeAll
  static void init() {
    SCHEMA_REGISTER_CONTAINER.start();
  }

  @Test
  void shouldHaveHealthyContainers() {
    assertThat(KAFKA_CONTAINER.isRunning()).isTrue();
    assertThat(SCHEMA_REGISTER_CONTAINER.isRunning()).isTrue();
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