package io.github.leeyou34.todo.todo;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.leeyou34.todo.common.ApiException;

/**
 * 할 일 등록·조회·수정·삭제.
 * 모든 메서드는 로그인한 사용자 id를 받아, 그 사용자의 데이터 안에서만 동작합니다.
 * (2022 버전은 수정·삭제 때 소유자를 확인하지 않아 id만 알면 남의 할 일을 바꿀 수 있었습니다.)
 */
@Service
public class TodoService {

	private final TodoRepository repository;

	public TodoService(TodoRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public List<TodoDTO> list(UUID userId) {
		return repository.findByUserIdOrderByCreatedAtAsc(userId).stream().map(TodoDTO::from).toList();
	}

	@Transactional
	public List<TodoDTO> create(UUID userId, String title) {
		repository.save(new TodoEntity(userId, title.trim()));
		return list(userId);
	}

	@Transactional
	public List<TodoDTO> update(UUID userId, UUID todoId, String title, boolean done) {
		TodoEntity todo = findOwned(userId, todoId);
		todo.change(title.trim(), done);
		repository.flush();
		return list(userId);
	}

	@Transactional
	public List<TodoDTO> delete(UUID userId, UUID todoId) {
		repository.delete(findOwned(userId, todoId));
		repository.flush();
		return list(userId);
	}

	private TodoEntity findOwned(UUID userId, UUID todoId) {
		return repository.findByIdAndUserId(todoId, userId)
			.orElseThrow(() -> ApiException.notFound("할 일을 찾을 수 없습니다."));
	}
}
