package com.practical.userapi.config;

import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Component
public class JwtTokenUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenUtil.class);

    private final String secret = Base64.getEncoder().encodeToString(
            "your-very-long-secret-key-for-jwt-token-generation-12345".getBytes()
    );

    private final Key signingKey = new SecretKeySpec(secret.getBytes(), SignatureAlgorithm.HS512.getJcaName());

    private static final long JWT_TOKEN_VALIDITY = 3 * 60 * 1000;
    private final Map<String, Date> blacklistedTokens = new ConcurrentHashMap<>();

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    private Boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public Boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.containsKey(token);
    }

    public void blacklistToken(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            blacklistedTokens.put(token, expiration);
            log.info("Token blacklisted successfully. Will expire at: {}", expiration);
        } catch (Exception e) {
            blacklistedTokens.put(token, new Date());
            log.warn("Token blacklisted but could not parse expiration: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 3600000)
    public void cleanupBlacklistedTokens() {
        Date now = new Date();
        int initialSize = blacklistedTokens.size();
        blacklistedTokens.entrySet().removeIf(entry -> now.after(entry.getValue()));
        int removedCount = initialSize - blacklistedTokens.size();
        if (removedCount > 0) {
            log.info("Cleaned up {} expired blacklisted tokens. Remaining: {}", removedCount, blacklistedTokens.size());
        }
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return doGenerateToken(claims, userDetails.getUsername());
    }

    private String doGenerateToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY))
                .signWith(signingKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = getUsernameFromToken(token);

            if (isTokenBlacklisted(token)) {
                log.warn("Token is blacklisted for user: {}", username);
                return false;
            }
            if (isTokenExpired(token)) {
                log.warn("Token has expired for user: {}", username);
                return false;
            }
            boolean isValid = username.equals(userDetails.getUsername());
            if (!isValid) {
                log.warn("Token username mismatch. Token: {}, UserDetails: {}", username, userDetails.getUsername());
            }
            return isValid;

        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
    public Boolean isTokenValid(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return false;
            }

            if (isTokenBlacklisted(token)) {
                log.warn("Token is blacklisted");
                return false;
            }

            Claims claims = getAllClaimsFromToken(token);

            if (claims.getExpiration().before(new Date())) {
                log.warn("Token has expired");
                return false;
            }

            if (claims.getSubject() == null || claims.getSubject().isEmpty()) {
                log.warn("Token has no subject");
                return false;
            }

            return true;

        } catch (ExpiredJwtException e) {
            log.warn("Token has expired: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("Token is malformed: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.warn("Token signature is invalid: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("Token is empty or null: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}