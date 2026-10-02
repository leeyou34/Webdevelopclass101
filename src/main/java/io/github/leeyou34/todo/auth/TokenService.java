package io.github.leeyou34.todo.auth;

import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import io.github.leeyou34.todo.config.AppProperties;

/**
 * 로그인에 성공한 사용자에게 JWT(로그인 증표)를 발급합니다.
 * 토큰의 subject에는 사용자 id만 넣고, 비밀번호 같은 민감 정보는 넣지 않습니다.
 */
@Service
public class TokenService {

	static final String ISSUER = "todo-api";

	private final JwtEncoder jwtEncoder;
	private final AppProperties properties;

	public TokenService(JwtEncoder jwtEncoder, AppProperties properties) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
	}

	public String issue(UserEntity user) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(ISSUER)
			.issuedAt(now)
			.expiresAt(now.plus(properties.jwtExpiration()))
			.subject(user.getId().toString())
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}
