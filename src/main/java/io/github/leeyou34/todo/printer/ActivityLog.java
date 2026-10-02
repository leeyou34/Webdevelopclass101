package io.github.leeyou34.todo.printer;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** 누가 언제 어떤 동작을 했는지. 동작 번호(1~21)는 기획서의 동작 목록과 같습니다. */
@Entity
@Table(name = "pr_activity")
public class ActivityLog extends OwnedEntity {

	public int action;

	@Column(nullable = false, length = 30)
	public String label;

	@Column(length = 20)
	public String targetType;

	public UUID targetId;

	@Column(length = 300)
	public String detail;
}
