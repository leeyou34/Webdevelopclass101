package io.github.leeyou34.todo.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 오류를 {error: "..."} 형식으로 통일합니다. 내부 오류 내용은 로그에만 남기고 화면에는 내보내지 않습니다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ResponseDTO<Object>> handleApi(ApiException e) {
		return ResponseEntity.status(e.getStatus()).body(ResponseDTO.error(e.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ResponseDTO<Object>> handleValidation(MethodArgumentNotValidException e) {
		FieldError fieldError = e.getBindingResult().getFieldError();
		String message = fieldError != null ? fieldError.getDefaultMessage() : "입력값을 확인해 주세요.";
		return ResponseEntity.badRequest().body(ResponseDTO.error(message));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ResponseDTO<Object>> handleUnreadable(HttpMessageNotReadableException e) {
		return ResponseEntity.badRequest().body(ResponseDTO.error("요청 형식이 올바르지 않습니다."));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ResponseDTO<Object>> handleUnexpected(Exception e) {
		if (e instanceof ErrorResponse errorResponse) {
			// 없는 주소(404), 허용하지 않는 메서드(405) 같은 Spring 표준 오류는 원래 상태 코드를 유지
			HttpStatusCode status = errorResponse.getStatusCode();
			return ResponseEntity.status(status).body(ResponseDTO.error("요청을 처리할 수 없습니다. (" + status.value() + ")"));
		}
		log.error("처리하지 못한 오류", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ResponseDTO.error("서버에서 오류가 발생했습니다."));
	}
}
