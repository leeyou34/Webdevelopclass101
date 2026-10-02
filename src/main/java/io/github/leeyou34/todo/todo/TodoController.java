package io.github.leeyou34.todo.todo;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.leeyou34.todo.common.ApiException;
import io.github.leeyou34.todo.common.ResponseDTO;
import jakarta.validation.Valid;

/**
 * /todo  (로그인 필요, Authorization: Bearer 토큰)
 *   GET     내 할 일 목록
 *   POST    등록   {title}
 *   PUT     수정   {id, title, done}
 *   DELETE  삭제   {id}
 * 모든 응답은 변경 후의 전체 목록을 {data: [...]}로 돌려줍니다(2022 버전과 동일한 약속).
 */
@RestController
@RequestMapping("/todo")
public class TodoController {

	private final TodoService service;

	public TodoController(TodoService service) {
		this.service = service;
	}

	@GetMapping
	public ResponseDTO<TodoDTO> list(@AuthenticationPrincipal Jwt jwt) {
		return ResponseDTO.of(service.list(userId(jwt)));
	}

	@PostMapping
	public ResponseDTO<TodoDTO> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TodoDTO dto) {
		return ResponseDTO.of(service.create(userId(jwt), dto.title()));
	}

	@PutMapping
	public ResponseDTO<TodoDTO> update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TodoDTO dto) {
		return ResponseDTO.of(service.update(userId(jwt), todoId(dto), dto.title(), dto.doneOrFalse()));
	}

	@DeleteMapping
	public ResponseDTO<TodoDTO> delete(@AuthenticationPrincipal Jwt jwt, @RequestBody TodoDTO dto) {
		return ResponseDTO.of(service.delete(userId(jwt), todoId(dto)));
	}

	private static UUID userId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}

	private static UUID todoId(TodoDTO dto) {
		if (dto.id() == null || dto.id().isBlank()) {
			throw ApiException.badRequest("수정·삭제할 할 일의 id가 필요합니다.");
		}
		try {
			return UUID.fromString(dto.id());
		} catch (IllegalArgumentException e) {
			throw ApiException.notFound("할 일을 찾을 수 없습니다.");
		}
	}
}
