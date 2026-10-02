package io.github.leeyou34.todo.printer;

import java.time.LocalDate;
import java.util.UUID;

import io.github.leeyou34.todo.printer.Enums.CaseStatus;
import io.github.leeyou34.todo.printer.Enums.CaseType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** 배송 후 사후 처리 한 건: 반품, 불량 교환(선배송 후 회수), AS 수리 */
@Entity
@Table(name = "pr_case")
public class ServiceCase extends OwnedEntity {

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	public CaseType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 15)
	public CaseStatus status = CaseStatus.OPEN;

	@Column(nullable = false)
	public UUID deviceId;

	@Column(nullable = false, length = 30)
	public String serial;

	public UUID requestId;

	public UUID shopId;

	@Column(length = 200)
	public String reason;

	@Column(nullable = false)
	public LocalDate openedOn;

	public LocalDate closedOn;

	/** 교환일 때 새로 보낸 기기 */
	public UUID newDeviceId;

	@Column(length = 30)
	public String newSerial;

	/** AS일 때 무상 여부(생산 후 2년 이내) */
	public Boolean freeWarranty;
}
