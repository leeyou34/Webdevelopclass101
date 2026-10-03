package io.github.leeyou34.todo.printer;

import java.time.LocalDate;
import java.util.UUID;

import io.github.leeyou34.todo.printer.Enums.InvoiceStatus;
import io.github.leeyou34.todo.printer.Enums.InvoiceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** 세금계산서. 개인 앞은 신청 건마다, 본사 앞은 월(사이클)마다 한 장 */
@Entity
@Table(name = "pr_invoice")
public class Invoice extends OwnedEntity {

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	public InvoiceType type;

	@Column(nullable = false)
	public UUID cycleId;

	/** 개인 앞일 때만 */
	public UUID requestId;

	public long amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	public InvoiceStatus status = InvoiceStatus.REQUESTED;

	public LocalDate plannedOn;

	public LocalDate issuedOn;

	public LocalDate paidOn;
}
