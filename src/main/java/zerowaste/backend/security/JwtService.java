package zerowaste.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtService {

    private final SecretKey accessKey;
    private final SecretKey refreshKey;
    private final long accessExpirationMillis;
    private final long refreshExpirationMillis;

    public JwtService(
            @Value("${security.jwt.access.secret}") String accessSecret,
            @Value("${security.jwt.refresh.secret}") String refreshSecret,
            @Value("${security.jwt.access.expiration-ms:3600000}") long accessExpirationMillis,
            @Value("${security.jwt.refresh.expiration-ms:1209600000}") long refreshExpirationMillis
    ) {
        this.accessKey = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMillis = accessExpirationMillis;
        this.refreshExpirationMillis = refreshExpirationMillis;
    }


    public String generateAccessToken(String subject, Map<String, Object> extraClaims) {
        return generateToken(subject, extraClaims, accessKey, accessExpirationMillis);
    }



    public String generateRefreshToken(String subject, Map<String, Object> extraClaims) {
        return generateToken(subject, extraClaims, refreshKey, refreshExpirationMillis);
    }



    private String generateToken(String subject, Map<String, Object> extraClaims, SecretKey key, long ttlMillis) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + ttlMillis);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public String extractSubject(String token, boolean isRefreshToken) {
        return parseAllClaims(token, isRefreshToken).getSubject();
    }

    public boolean isTokenValid(String token, String expectedSubject, boolean isRefreshToken) {
        try {
            Claims claims = parseAllClaims(token, isRefreshToken);
            return expectedSubject.equals(claims.getSubject()) && claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseAllClaims(String token, boolean isRefreshToken) {
        SecretKey key = isRefreshToken ? refreshKey : accessKey;
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
