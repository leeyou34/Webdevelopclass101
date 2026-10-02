package io.github.leeyou34.todo.printer;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

/**
 * 모든 운영 데이터의 공통 부분. ownerId = 이 데이터를 가진 로그인 사용자.
 * 필드를 public으로 두어 화면에 그대로 내보냅니다(작은 체험용 시스템이라 단순함을 택함).
 */
@MappedSuperclass
public abstract class OwnedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	public UUID id;

	@Column(nullable = false, updatable = false)
	public UUID ownerId;

	@Column(nullable = false, updatable = false)
	public Instant createdAt;

	@PrePersist
	void stampCreated() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}
}
