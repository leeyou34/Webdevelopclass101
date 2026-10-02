package io.github.leeyou34.todo.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 로그인·회원가입 요청과 응답 형식.
 */
public final class AuthDtos {

	private AuthDtos() {
	}

	public record SignupRequest(
		@NotBlank(message = "이름을 입력해 주세요.")
		@Size(max = 50, message = "이름은 50자 이내로 입력해 주세요.")
		String username,

		@NotBlank(message = "이메일을 입력해 주세요.")
		@Email(message = "이메일 형식이 올바르지 않습니다.")
		@Size(max = 255)
		String email,

		@NotBlank(message = "비밀번호를 입력해 주세요.")
		@Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하로 입력해 주세요.")
		String password,

		String inviteCode) {
	}

	public record SigninRequest(
		@NotBlank(message = "이메일을 입력해 주세요.")
		String email,

		@NotBlank(message = "비밀번호를 입력해 주세요.")
		String password) {
	}

	/** 비밀번호는 절대 응답에 넣지 않습니다. token은 로그인할 때만 채워집니다. */
	public record UserResponse(String id, String username, String email, String token) {

		static UserResponse of(UserEntity user, String token) {
			return new UserResponse(user.getId().toString(), user.getUsername(), user.getEmail(), token);
		}
	}
}
