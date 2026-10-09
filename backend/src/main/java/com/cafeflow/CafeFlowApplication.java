package com.cafeflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CaféFlow backend entrypoint.
 *
 * <p>The backend is a <strong>modular monolith</strong>: one deployable, many modules
 * ({@code auth}, {@code tenant}, {@code table}, ...). Do not split modules into services.
 */
@SpringBootApplication
public class CafeFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(CafeFlowApplication.class, args);
    }
}
