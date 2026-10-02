package io.github.leeyou34.todo.auth;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.leeyou34.todo.auth.AuthDtos.SigninRequest;
import io.github.leeyou34.todo.auth.AuthDtos.SignupRequest;
import io.github.leeyou34.todo.auth.AuthDtos.UserResponse;
import io.github.leeyou34.todo.common.ApiException;
import jakarta.validation.Valid;

/**
 * POST /auth/signup  회원가입
 * POST /auth/signin  로그인 → token 발급
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

	private final UserService userService;
	private final TokenService tokenService;

	public AuthController(UserService userService, TokenService tokenService) {
		this.userService = userService;
		this.tokenService = tokenService;
	}

	@PostMapping("/signup")
	public UserResponse signup(@Valid @RequestBody SignupRequest request) {
		UserEntity user = userService.signup(request);
		return UserResponse.of(user, null);
	}

	@PostMapping("/signin")
	public UserResponse signin(@Valid @RequestBody SigninRequest request) {
		UserEntity user = userService.authenticate(request.email(), request.password())
			// 이메일이 없는지, 비밀번호가 틀렸는지 구분해 알려 주지 않습니다(계정 존재 여부 노출 방지).
			.orElseThrow(() -> ApiException.unauthorized("이메일 또는 비밀번호가 올바르지 않습니다."));
		return UserResponse.of(user, tokenService.issue(user));
	}
}
