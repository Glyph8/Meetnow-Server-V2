package timetogeter.global.security.util.jwt;


import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import timetogeter.global.interceptor.response.error.status.BaseErrorCode;
import timetogeter.context.auth.application.dto.TokenCommand;
import timetogeter.context.auth.domain.adaptor.UserPrincipal;
import timetogeter.context.auth.exception.InvalidJwtException;

import java.security.Key;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtTokenProvider implements TokenProvider {

    private static final String BEARER_TYPE = "Bearer";
    private static final String AUTHORITIES_KEY = "auth";
    private static final String ACCESS_TYPE = "access";
    private static final String REFRESH_TYPE = "refresh";

    @Value("${jwt.access.expiration}")
    private int ACCESS_TOKEN_EXPIRE_TIME;

    @Value("${jwt.refresh.expiration}")
    private int REFRESH_TOKEN_EXPIRE_TIME;

    private final Key key;
    public JwtTokenProvider(@Value("${jwt.secretKey}") String secretKey){
        log.info("//// Injected JWT secretKey: [{}]", secretKey);
        this.key = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(secretKey));
    }

    @Override
    public TokenCommand generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        String userId = userPrincipal.getId();
        String authorities = getAuthorities(authentication.getAuthorities());

        String accessToken = createAccessToken(userId, authorities);
        String refreshToken = createRefreshToken(userId, authorities);
        log.info("generateToken 함수 실행, accessToken = {}, refreshToken = {}", accessToken, refreshToken);

        return new TokenCommand(accessToken, ACCESS_TOKEN_EXPIRE_TIME, refreshToken, REFRESH_TOKEN_EXPIRE_TIME);
    }

    private String getAuthorities(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
    }

    private String createAccessToken(String userId, String authorities) {
        Date now = new Date();
        String accessToken = Jwts.builder()
                .setSubject(userId)
                .claim(AUTHORITIES_KEY, authorities)
                .claim("type", ACCESS_TYPE)
                .setIssuedAt(now)   //토큰 발행 시간 정보
                .setExpiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRE_TIME))  //토큰 만료 시간 설정
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return accessToken;
    }

    private String createRefreshToken(String userId, String authorities){
        Date now = new Date();
        String refreshToken = Jwts.builder()
                .setSubject(userId)
                .claim(AUTHORITIES_KEY, authorities)
                .claim("type", REFRESH_TYPE)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + REFRESH_TOKEN_EXPIRE_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return refreshToken;
    }

    @Override
    public String validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidJwtException(BaseErrorCode.INVALID_TOKEN, "[ERROR] 토큰이 비어있습니다.");
        }
        //
        log.info("=== 토큰 검증 시작 ===");
        log.info("검증 토큰 (길이:" + token.length() + "): " + token);
        log.info("사용 중인 비밀키 (Base64 인코딩): " + Base64.getEncoder().encodeToString(key.getEncoded()));
        //
        String error = "";
        log.info("토큰 : " + token);
        try {
            Jws<Claims> claimsJws = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            Claims claims = claimsJws.getBody();
            //
            log.info("서명 검증 성공. subject: " + claims.getSubject());
            //
            return claims.getSubject();
        }catch (SecurityException | MalformedJwtException e){
            error = "[ERROR] 토큰의 서명이 유효하지 않습니다.";
        }catch (ExpiredJwtException e){
            error = "[ERROR] 토큰이 만료되었습니다.";
        }catch (UnsupportedJwtException e){
            error = "[ERROR] 지원되지 않는 JWT 토큰입니다.";
        }catch (IllegalArgumentException e){
            error = "[ERROR] JWT 클레임이 포함되어있지 않습니다.";
        }catch (Exception e) {
            log.info(e.getMessage());
            error = "[ERROR] 유효하지 않은 토큰입니다.";
        }


        throw new InvalidJwtException(BaseErrorCode.INVALID_TOKEN, error);
    }

    public String extractBearer(String token) {
        if (token.startsWith(BEARER_TYPE)) {
            return token = token.substring(7);
        }
        return null;
    }

    public String generateTokenFromRefreshToken(String refreshToken) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(refreshToken)
                .getBody();

        String tokenType = claims.get("type", String.class);
        if (!REFRESH_TYPE.equals(tokenType)) {
            throw new InvalidJwtException(BaseErrorCode.INVALID_TOKEN, "[ERROR]: 제공된 토큰이 리프레시 토큰이 아닙니다.");
        }

        String userId = claims.getSubject();
        String authorities = claims.get(AUTHORITIES_KEY, String.class);

        return createAccessToken(userId, authorities);
    }

    public int getExpiration(String token){
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key) // 위에서 토큰 만들 때 사용한 키
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Date expiration = claims.getExpiration();
            long now = System.currentTimeMillis();

            int remaining = (int) ((expiration.getTime() - now) / 1000);
            if (remaining < 0) throw new InvalidJwtException(BaseErrorCode.INVALID_TOKEN, "[ERROR] 토큰이 만료되었습니다.");
            return remaining;

        } catch (JwtException e) {
            throw new InvalidJwtException(BaseErrorCode.INVALID_TOKEN, "[ERROR] "+e.getMessage());
        }
    }
}
