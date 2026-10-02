package io.github.leeyou34.todo.todo;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "todo", indexes = @Index(name = "idx_todo_user_id", columnList = "user_id"))
public class TodoEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/** 이 할 일을 만든 사용자. 조회·수정·삭제는 항상 이 값으로 범위를 제한합니다. */
	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false)
	private boolean done;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected TodoEntity() {
	}

	public TodoEntity(UUID userId, String title) {
		this.userId = userId;
		this.title = title;
		this.done = false;
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = Instant.now();
	}

	public void change(String title, boolean done) {
		this.title = title;
		this.done = done;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getTitle() {
		return title;
	}

	public boolean isDone() {
		return done;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
