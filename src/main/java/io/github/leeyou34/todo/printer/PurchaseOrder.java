package io.github.leeyou34.todo.printer;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** 납품사 앞 발주서. 확정 수량에 여유분을 더해 발주합니다. */
@Entity
@Table(name = "pr_purchase_order")
public class PurchaseOrder extends OwnedEntity {

	@Column(nullable = false)
	public UUID cycleId;

	@Column(nullable = false, length = 20)
	public String orderNo;

	@Column(nullable = false)
	public LocalDate orderedOn;

	public int androidQty;

	public int iosQty;

	public int bufferAndroid;

	public int bufferIos;

	@Column(length = 100)
	public String boxNote;

	@Column(length = 30)
	public String approvalNo;

	public LocalDate approvedOn;

	public int receivedAndroid;

	public int receivedIos;

	public LocalDate receivedOn;
}
