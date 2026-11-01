package com.ayushi.expensetracker.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Date;
import javax.crypto.SecretKey;

@Component
public class JwtUtil {
    @Value("${app.jwt.secret}")
    private String Secret;
    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey getSigningKey(){
        return Keys.hmacShaKeyFor(Secret.getBytes());
    }
    public String generateToken(String email, Long userId){
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmail(String token){
        return parseClaims(token).getSubject();
    }

    public Long extractUserId(String token){
        return parseClaims(token).get("userId", Long.class);
    }

    public boolean isTokenValid(String token){
        try{
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        }
        catch (Exception e){
            return false;
        }
    }

    public Claims parseClaims(String token){
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
