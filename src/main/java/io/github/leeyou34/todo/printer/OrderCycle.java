package io.github.leeyou34.todo.printer;

import java.time.LocalDate;

import io.github.leeyou34.todo.printer.Enums.CycleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** 월별 신청 기간(한 달 = 한 사이클). month는 "2017-08" 형식 */
@Entity
@Table(name = "pr_cycle")
public class OrderCycle extends OwnedEntity {

	@Column(nullable = false, length = 7)
	public String month;

	@Column(nullable = false)
	public LocalDate closesOn;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	public CycleStatus status = CycleStatus.OPEN;

	public LocalDate closedOn;

	public LocalDate confirmedOn;
}
