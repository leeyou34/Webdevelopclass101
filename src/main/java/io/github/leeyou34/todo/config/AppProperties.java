package io.github.leeyou34.todo.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.properties 의 app.* 설정값.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, Signup signup) {

	public record Jwt(String secret, Duration expiration) {
	}

	public record Cors(List<String> allowedOrigins) {
	}

	public record Signup(String inviteCode) {
	}

	public String jwtSecret() {
		return jwt == null ? null : jwt.secret();
	}

	public Duration jwtExpiration() {
		return (jwt == null || jwt.expiration() == null) ? Duration.ofDays(1) : jwt.expiration();
	}

	public List<String> corsAllowedOrigins() {
		return (cors == null || cors.allowedOrigins() == null) ? List.of() : cors.allowedOrigins();
	}

	public String inviteCode() {
		return signup == null ? null : signup.inviteCode();
	}
}
