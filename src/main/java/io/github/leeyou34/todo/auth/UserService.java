package io.github.leeyou34.todo.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.leeyou34.todo.auth.AuthDtos.SignupRequest;
import io.github.leeyou34.todo.common.ApiException;
import io.github.leeyou34.todo.config.AppProperties;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AppProperties properties;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AppProperties properties) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
	}

	@Transactional
	public UserEntity signup(SignupRequest request) {
		checkInviteCode(request.inviteCode());

		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmail(email)) {
			throw ApiException.conflict("이미 가입된 이메일입니다.");
		}
		UserEntity user = new UserEntity(request.username().trim(), email, passwordEncoder.encode(request.password()));
		return userRepository.save(user);
	}

	@Transactional(readOnly = true)
	public Optional<UserEntity> authenticate(String email, String rawPassword) {
		return userRepository.findByEmail(normalizeEmail(email))
			.filter(user -> passwordEncoder.matches(rawPassword, user.getPassword()));
	}

	/** 초대 코드가 설정된 경우에만 확인합니다. 비교 시간으로 코드를 추측하지 못하도록 고정 시간 비교를 씁니다. */
	private void checkInviteCode(String submitted) {
		String required = properties.inviteCode();
		if (required == null || required.isBlank()) {
			return;
		}
		byte[] expected = required.trim().getBytes(StandardCharsets.UTF_8);
		byte[] actual = (submitted == null ? "" : submitted.trim()).getBytes(StandardCharsets.UTF_8);
		if (!MessageDigest.isEqual(expected, actual)) {
			throw ApiException.badRequest("초대 코드가 올바르지 않습니다.");
		}
	}

	private static String normalizeEmail(String email) {
		return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
	}
}
