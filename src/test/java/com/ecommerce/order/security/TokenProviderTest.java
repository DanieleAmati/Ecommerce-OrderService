package com.ecommerce.order.security;

import com.ecommerce.order.config.JwtProperties;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitario di TokenProvider — verifica isolata del componente JWT.
 * Nessun mock necessario: TokenProvider è puro (no I/O).
 * I token vengono costruiti con JJWT direttamente nel test,
 * poiché TokenProvider espone solo validateAccessToken (validazione in entrata).
 */
class TokenProviderTest {

    private TokenProvider tokenProvider;

    private static final String SECRET  = "testSecretKeyThatIsAtLeast256BitsLongForHMAC";
    private static final String USER_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);
        props.setExpirationMs(3_600_000L);
        tokenProvider = new TokenProvider(props);
    }

    @Test
    @DisplayName("token valido con userId e ruoli → Claims corretti")
    void validateAccessToken_validToken_returnsExpectedClaims() {
        String token = buildToken(USER_ID, List.of("USER", "ADMIN"), 3_600_000L);

        StepVerifier.create(tokenProvider.validateAccessToken(token))
                .assertNext(claims -> {
                    assertThat(claims.getSubject()).isEqualTo(USER_ID);
                    assertThat(tokenProvider.extractRoles(claims))
                            .containsExactlyInAnyOrder("USER", "ADMIN");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("token manomesso → JwtException")
    void validateAccessToken_tamperedToken_returnsError() {
        String token = buildToken(USER_ID, List.of("USER"), 3_600_000L) + "tampered";

        StepVerifier.create(tokenProvider.validateAccessToken(token))
                .expectError(JwtException.class)
                .verify();
    }

    @Test
    @DisplayName("token scaduto → JwtException")
    void validateAccessToken_expiredToken_returnsError() {
        String token = buildToken(USER_ID, List.of("USER"), -1_000L);

        StepVerifier.create(tokenProvider.validateAccessToken(token))
                .expectError(JwtException.class)
                .verify();
    }

    private String buildToken(String userId, List<String> roles, long expirationMs) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }
}
