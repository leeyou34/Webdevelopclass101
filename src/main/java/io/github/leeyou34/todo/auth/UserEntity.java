package io.github.leeyou34.todo.auth;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class UserEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 50)
	private String username;

	@Column(nullable = false, length = 255)
	private String email;

	/** BCrypt로 암호화된 값만 저장합니다. */
	@Column(nullable = false, length = 100)
	private String password;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected UserEntity() {
	}

	public UserEntity(String username, String email, String passwordHash) {
		this.username = username;
		this.email = email;
		this.password = passwordHash;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getEmail() {
		return email;
	}

	public String getPassword() {
		return password;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
