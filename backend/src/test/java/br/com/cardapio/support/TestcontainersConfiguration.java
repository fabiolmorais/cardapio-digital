package br.com.cardapio.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /** O bucket que o S3Mock já cria ao subir. */
    public static final String BUCKET = "cardapio-test";

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
    }

    /**
     * O S3Mock faz o papel do R2 nos testes: fala a API S3, sem chave de verdade nem internet.
     * Ele aceita a URL assinada sem conferir a assinatura; isso o R2 de verdade confere.
     */
    @Bean
    GenericContainer<?> s3MockContainer() {
        return new GenericContainer<>(DockerImageName.parse("adobe/s3mock:4.11.0"))
                .withEnv("COM_ADOBE_TESTING_S3MOCK_STORE_INITIAL_BUCKETS", BUCKET)
                .withExposedPorts(9090)
                .waitingFor(Wait.forHttp("/").forPort(9090));
    }

    @Bean
    DynamicPropertyRegistrar storageProperties(GenericContainer<?> s3MockContainer) {
        return registry -> {
            String endpoint = "http://" + s3MockContainer.getHost() + ":" + s3MockContainer.getMappedPort(9090);
            registry.add("app.storage.endpoint", () -> endpoint);
            registry.add("app.storage.bucket", () -> BUCKET);
            registry.add("app.storage.public-url", () -> endpoint + "/" + BUCKET);
            registry.add("app.storage.access-key-id", () -> "teste");
            registry.add("app.storage.secret-access-key", () -> "teste");
        };
    }
}
