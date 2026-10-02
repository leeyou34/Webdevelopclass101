package io.github.leeyou34.todo.printer;

import java.time.LocalDate;
import java.util.UUID;

import io.github.leeyou34.todo.printer.Enums.RequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** 신청 건: 카운슬러 한 명의 한 번 신청. 취소·반품이 건 단위로 일어나므로 추적 단위로 삼습니다. */
@Entity
@Table(name = "pr_request")
public class OrderRequest extends OwnedEntity {

	@Column(nullable = false)
	public UUID cycleId;

	@Column(nullable = false)
	public UUID shopId;

	@Column(nullable = false, length = 30)
	public String applicantName;

	public int androidQty;

	public int iosQty;

	/** 개인(카운슬러) 앞 청구액, 원, VAT 미포함 */
	public long personalAmount;

	/** 본사 앞 청구액, 원, VAT 미포함 */
	public long hqAmount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	public RequestStatus status = RequestStatus.APPLIED;

	public long paidAmount;

	public LocalDate paidOn;

	public UUID purchaseOrderId;

	@Column(length = 30)
	public String trackingNo;

	public LocalDate shippedOn;

	public LocalDate deliveredOn;

	/** 회계 기준 출고 월을 정하는 날짜(처음 계산서가 발행된 날) */
	public LocalDate invoicedOn;

	@Column(length = 200)
	public String note;

	/** 돌려줘야 할 금액 합계와 실제 돌려준 금액 합계 */
	public long refundDue;

	public long refundedTotal;

	public int totalQty() {
		return androidQty + iosQty;
	}
}
