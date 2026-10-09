package com.cafeflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Phase 0 integration test against real PostgreSQL and Redis (Testcontainers).
 * Runs in {@code ./mvnw verify} (maven-failsafe); requires a Docker daemon.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FoundationIT {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("DATABASE_URL", POSTGRES::getJdbcUrl);
        registry.add("DATABASE_USERNAME", POSTGRES::getUsername);
        registry.add("DATABASE_PASSWORD", POSTGRES::getPassword);
        registry.add("REDIS_URL", () -> "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
    }

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void contextStartsAndPostgresIsReachable() {
        assertThat(jdbc.queryForObject("SELECT 1", Integer.class)).isEqualTo(1);
    }

    @Test
    void readinessIsUpOnlyWhenPostgresAndRedisAreReachable() throws Exception {
        HttpResponse<String> response = get("/actuator/health/readiness");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }

    @Test
    void livenessIsUp() throws Exception {
        assertThat(get("/actuator/health/liveness").statusCode()).isEqualTo(200);
    }

    @Test
    void healthDoesNotLeakComponentDetails() throws Exception {
        assertThat(get("/actuator/health").body()).doesNotContain("jdbc", "postgres", "redis");
    }

    @Test
    void everythingElseIsDeniedByDefault() throws Exception {
        HttpResponse<String> api = get("/api/v1/orders");
        assertThat(api.statusCode()).isEqualTo(401);
        assertThat(api.body()).contains("\"code\":\"UNAUTHENTICATED\"");

        assertThat(get("/actuator/env").statusCode()).isEqualTo(401);
    }

    @Test
    void responsesCarryRequestIdAndSecurityHeaders() throws Exception {
        HttpResponse<String> response = get("/api/v1/orders");

        assertThat(response.headers().firstValue("X-Request-Id")).isPresent();
        assertThat(response.headers().firstValue("X-Content-Type-Options")).hasValue("nosniff");
    }
}
