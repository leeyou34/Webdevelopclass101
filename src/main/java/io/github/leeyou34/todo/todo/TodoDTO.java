package io.github.leeyou34.todo.todo;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 할 일 주고받기 형식. 2022 버전의 {id, title, done}에 생성·수정 시각을 더했습니다.
 * createdAt, updatedAt은 서버가 채우며 요청에 넣어도 무시됩니다.
 */
public record TodoDTO(
	String id,

	@NotBlank(message = "할 일 내용을 입력해 주세요.")
	@Size(max = 200, message = "할 일은 200자 이내로 입력해 주세요.")
	String title,

	boolean done,

	Instant createdAt,

	Instant updatedAt) {

	static TodoDTO from(TodoEntity entity) {
		return new TodoDTO(entity.getId().toString(), entity.getTitle(), entity.isDone(),
			entity.getCreatedAt(), entity.getUpdatedAt());
	}
}
