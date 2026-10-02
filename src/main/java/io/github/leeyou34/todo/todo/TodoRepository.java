package io.github.leeyou34.todo.todo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoRepository extends JpaRepository<TodoEntity, UUID> {

	List<TodoEntity> findByUserIdOrderByCreatedAtAsc(UUID userId);

	/** id와 소유자를 함께 조건으로 찾습니다. 남의 할 일은 "없음"으로 처리됩니다. */
	Optional<TodoEntity> findByIdAndUserId(UUID id, UUID userId);
}
