package com.order.order.common.auth;

import com.order.order.member.domain.Member;
import com.order.order.member.repository.MemberRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Date;
import java.util.concurrent.TimeUnit;

//@RequiredArgsConstructor
@Component
public class JwtTokenProvider {

    @Value("${jwt.expirationAt}")
    private int expirationAt;

    @Value("${jwt.secretKeyAt}")
    private String secretKeyAt;

    @Value("${jwt.expirationRt}")
    private int expirationRt;

    @Value("${jwt.secretKeyRt}")
    private String secretKeyRt;

    // 암호화 알고리즘 사용한 key (암호화를 한 건 아님)
    private Key secret_at_key;
    private Key secret_rt_key;

    private final MemberRepository memberRepository;

    // @Qualifier("rtInventory") 사용해야 하기 때문에 @RequiredArgsConstructor 만으로는 주입 받기 어려움
    private final RedisTemplate<String, String> redisTemplate;

    // @Qualifier는 기본적으로 메서드를 통한 주입 가능. 그래서 이 경우 생성자 주입 방식을 해야 Qualifier 사용 가능
    public JwtTokenProvider(MemberRepository memberRepository, @Qualifier("rtInventory") RedisTemplate<String, String> redisTemplate) {
        this.memberRepository = memberRepository;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        secret_at_key = new SecretKeySpec(java.util.Base64.getDecoder().decode(secretKeyAt)
                , SignatureAlgorithm.HS512.getJcaName());

        secret_rt_key = new SecretKeySpec(java.util.Base64.getDecoder().decode(secretKeyRt)
                , SignatureAlgorithm.HS512.getJcaName());
    }

    public String createAtToken(Member member) {
        String email = member.getEmail();
        String role = member.getRole().toString();
        init();

        Claims claims = Jwts.claims().setSubject(email);
        claims.put("role", role);

        Date now = new Date();
        String token =Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationAt * 90 * 1000L))
                .signWith(secret_at_key)
                .compact();

        return token;
    }

    public String createRtToken(Member member) {
        // 유효 기간이 긴 rt 토큰 생성
        String email = member.getEmail();
        String role = member.getRole().toString();
        init();

        Claims claims = Jwts.claims().setSubject(email);
        claims.put("role", role);

        Date now = new Date();
        String refreshToken =Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationRt * 90 * 1000L))
                .signWith(secret_rt_key)
                .compact();
        
        
        // rt 토큰을 redis에 저장 : key-value 형식으로 set
        redisTemplate.opsForValue().set(member.getEmail(), refreshToken);
        // 200일 TTL(유효 시간)
//        redisTemplate.opsForValue().set(member.getEmail(), refreshToken, 200, TimeUnit.DAYS);
        return refreshToken;
    }

    public Member validateRt(String refreshToken) {
        // rt 그 자체를 검증
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKeyRt)
                .build()
                .parseClaimsJws(refreshToken)
                .getBody();

        String email = claims.getSubject();
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("member is not found"));

        // redis의 값과 비교하는 검증
        String redisRt = redisTemplate.opsForValue().get(member.getEmail());
        if (!redisRt.equals(refreshToken)) {
            throw new IllegalArgumentException("잘못된 토큰 입니다.");
        }

        return member;
    }
}
