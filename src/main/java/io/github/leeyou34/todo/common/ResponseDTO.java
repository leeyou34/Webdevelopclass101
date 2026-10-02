package io.github.leeyou34.todo.common;

import java.util.List;

/**
 * 공통 응답 형식. 2022 버전과 같은 모양({error, data})을 유지해 화면 코드와 호환됩니다.
 */
public record ResponseDTO<T>(String error, List<T> data) {

	public static <T> ResponseDTO<T> of(List<T> data) {
		return new ResponseDTO<>(null, data);
	}

	public static <T> ResponseDTO<T> error(String message) {
		return new ResponseDTO<>(message, null);
	}
}
