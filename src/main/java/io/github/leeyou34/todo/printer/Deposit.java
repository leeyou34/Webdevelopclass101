package io.github.leeyou34.todo.printer;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 통장 입금 내역 한 줄. 신청 건과 자동으로 맞춰 보고(입금 확인), 맞출 수 없으면 "미확인 입금"으로 남습니다.
 * 입금자명은 동명이인을 막기 위해 "영업장명+신청자명"으로 받는 것이 원칙이었습니다.
 */
@Entity
@Table(name = "pr_deposit")
public class Deposit extends OwnedEntity {

	@Column(nullable = false)
	public LocalDate depositedOn;

	@Column(nullable = false, length = 40)
	public String depositorName;

	/** VAT 포함 입금액 */
	@Column(nullable = false)
	public long amount;

	/** 맞춰진 신청 건. 비어 있으면 미확인 입금 */
	public UUID requestId;

	public LocalDate matchedOn;

	@Column(length = 120)
	public String note;
}
