package io.github.leeyou34.todo.printer;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** 계산서 발행 뒤 생긴 변동(환불 등). 다음 계산서에 반영할 조정 항목 */
@Entity
@Table(name = "pr_adjustment")
public class Adjustment extends OwnedEntity {

	/** 반영할 달, "2017-09" 형식 */
	@Column(nullable = false, length = 7)
	public String month;

	public UUID requestId;

	/** 원, 음수면 차감 */
	public long amount;

	@Column(length = 200)
	public String reason;

	@Column(nullable = false)
	public LocalDate createdOn;
}
