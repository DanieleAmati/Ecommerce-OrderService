package com.ecommerce.catalog.security;

import com.ecommerce.catalog.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class TokenProvider {

    private static final String CLAIM_ROLES = "roles";

    private final SecretKey accessKey;
    private final JwtProperties jwtProperties;

    public TokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.accessKey = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    public Mono<Claims> validateAccessToken(String token) {
        return parseToken(token, accessKey);
    }


    private Mono<Claims> parseToken(String token, SecretKey key) {
        return Mono.fromCallable(() ->
                Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
        ).onErrorMap(e -> {
            log.debug("[TokenProvider] Validazione token fallita: {}", e.getMessage());
            return new JwtException("Token non valido o scaduto", e);
        });
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(Claims claims) {
        return claims.get(CLAIM_ROLES, List.class);
    }

    public long getExpirationMs() {
        return jwtProperties.getExpirationMs();
    }
}
