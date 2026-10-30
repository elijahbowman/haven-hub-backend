package com.example.havenhub.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /**
     * A real PostgreSQL (same major version as compose.yaml), so the tests exercise
     * Liquibase, btree_gist and the booking_no_overlap constraint.
     */
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:17"))
                .withInitScript("db/init-test.sql");
    }

    /**
     * A static "okta" registration. Without it, Spring Boot fetches the issuer's
     * OIDC discovery document at startup, so tests would need network access and
     * a real Auth0 tenant. Logins in tests use spring-security-test instead.
     */
    @Bean
    ClientRegistrationRepository testClientRegistrationRepository() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("okta")
                .clientId("test-client")
                .clientSecret("test-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .issuerUri("https://idp.test/")
                .authorizationUri("https://idp.test/authorize")
                .tokenUri("https://idp.test/oauth/token")
                .jwkSetUri("https://idp.test/.well-known/jwks.json")
                .userInfoUri("https://idp.test/userinfo")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .build();
        return new InMemoryClientRegistrationRepository(registration);
    }

    /**
     * The resource-server (bearer token) path isn't used by the app's own frontend
     * and isn't exercised here.
     */
    @Bean
    JwtDecoder testJwtDecoder() {
        return token -> {
            throw new BadJwtException("JWT authentication isn't used in tests");
        };
    }
}
