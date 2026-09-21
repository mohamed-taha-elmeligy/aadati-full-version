package com.mts.aadati.configs.security.jwt;

import com.mts.aadati.exceptions.exception.InvalidCredentialsException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final JwtBlocklistService blocklistService;

    @Value("${security.jwt.secret-key}")
    private String secretKey;
    @Value("${security.jwt.expiration}")
    private long expiration;
    @Value("${security.jwt.refresh-expiration}")
    private long refreshExpiration;

    private static final String TYPE = "type";
    private static final String REFRESH = "refresh";
    private static final String ACCESS = "access";

    public String buildToken(Map<String,Object> claims, String username, long jwtExpiration){
        Date now = new Date();

        return Jwts.builder()
                .claims(claims)
                .id(UUID.randomUUID().toString())
                .subject(username)
                .signWith(getSignInKey(),Jwts.SIG.HS512)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtExpiration))
                .compact();
    }

    public String generateAccessToken(UserDetails userDetails){
        Map<String, Object> claims = new HashMap<>();
        claims.put(TYPE,ACCESS);
        return buildToken(claims, userDetails.getUsername(), expiration);
    }

    public String generateRefreshToken(UserDetails userDetails){
        Map<String, Object> claims = new HashMap<>();
        claims.put(TYPE, REFRESH);
        return buildToken(claims,userDetails.getUsername(),refreshExpiration);
    }

    public String extractUsername(String token){
        return extractClaims(token,Claims::getSubject);
    }

    public Date extractExpiration(String token){
        return extractClaims(token,Claims::getExpiration);
    }

    public String extractJti(String token) {
        return extractClaims(token, Claims::getId);
    }

    public boolean isRefreshToken(String token){
        return REFRESH.equals(
                extractAllClaims(token).get(TYPE,String.class)
        );
    }

    public long getAccessTokenExpiration() {
        return expiration;
    }

    public boolean isTokenAccessValid(String token, UserDetails userDetails){
        Claims claims = extractAllClaims(token);

        return userDetails.getUsername().equals(claims.getSubject())
                && ACCESS.equals(claims.get(TYPE,String.class))
                && !blocklistService.isBlocked(claims.getId());
    }

    public boolean isRefreshTokenValid(String token,UserDetails userDetails) {
        Claims claims = extractAllClaims(token);

        return userDetails.getUsername().equals(claims.getSubject())
                && REFRESH.equals(claims.get(TYPE,String.class))
                && !blocklistService.isBlocked(claims.getId());
    }


    // helper methods
    private  <T>T extractClaims(String token, Function<Claims,T> claimsResolver){
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token){
        try {
            return Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        }
        catch (ExpiredJwtException e){throw  new InvalidCredentialsException("Token is Expired");}
        catch (MalformedJwtException e){throw  new InvalidCredentialsException("Token is Malformed");}
        catch (SignatureException e) {throw new InvalidCredentialsException("Signature not valid");}
        catch (UnsupportedJwtException e){throw new InvalidCredentialsException("Unsupported token");}
        catch (IllegalArgumentException e) { throw new InvalidCredentialsException("Token is empty or invalid"); }
    }

    private SecretKey getSignInKey() {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }
}