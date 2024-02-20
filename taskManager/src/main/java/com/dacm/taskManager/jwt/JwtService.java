package com.dacm.taskManager.jwt;

import com.dacm.taskManager.entity.User;
import com.dacm.taskManager.exception.JwtErrorResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String SECRET_KEY;


    public String getToken(User user) {
        return getToken(new HashMap<>(), user);
    }

    /**
     * Generates a JWT token for the specified user with additional claims.
     *
     * @param extraClaims Additional claims to include in the JWT.
     * @param user        The user for whom the token is being generated.
     * @return The JWT token string.
     */
    private String getToken(Map<String, Object> extraClaims, User user) {
        return Jwts
                .builder()
                .claims(extraClaims) // Include additional claims
                .claim("firstName", user.getFirstname()) // Add user's first name as a claim
                .claim("lastName", user.getLastname()) // Add user's last name as a claim
                .subject(user.getUsername()) // Set the subject of the token to the user's username
                .issuedAt(new Date(System.currentTimeMillis())) // Set the token's issued date to the current time
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // Set token expiration to 60 minutes from now
                .signWith(getKey()) // Sign the token with the secret key
                .compact(); // Compact and return the token as a string
    }


    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String getUsernameFromToken(String token) {
        return getClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private Claims getAllClaims(String token) {
        try{

        }catch (ExpiredJwtException e){
            throw new JwtErrorResponse("Token has expired",e);
        }
        return Jwts
                .parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public <T> T getClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Date getExpiration(String token) {
        return getClaim(token, Claims::getExpiration);
    }

    private boolean isTokenExpired(String token) {
        return getExpiration(token).before(new Date());
    }


}
