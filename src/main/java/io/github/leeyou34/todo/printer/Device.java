package io.github.leeyou34.todo.printer;

import java.time.LocalDate;
import java.util.UUID;

import io.github.leeyou34.todo.printer.Enums.DeviceModel;
import io.github.leeyou34.todo.printer.Enums.DeviceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** 기기 한 대. 시리얼로 기종과 생산 연월을 알 수 있습니다. */
@Entity
@Table(name = "pr_device", uniqueConstraints = @UniqueConstraint(columnNames = { "owner_id", "serial" }))
public class Device extends OwnedEntity {

	@Column(nullable = false, length = 30)
	public String serial;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	public DeviceModel model;

	/** 생산 연월의 1일 */
	public LocalDate producedOn;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	public DeviceStatus status = DeviceStatus.IN_STOCK;

	public UUID purchaseOrderId;

	public UUID requestId;

	public UUID shopId;

	public LocalDate receivedOn;

	public LocalDate deliveredOn;
}
