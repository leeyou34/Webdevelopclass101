package io.github.leeyou34.todo.printer;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import io.github.leeyou34.todo.printer.Enums.InvoiceType;
import io.github.leeyou34.todo.printer.Enums.ShopType;

/**
 * 요청·응답 형식. 숫자는 Integer/Long(빈 값 허용)으로 받아 서비스에서 기본값을 채웁니다.
 * 날짜를 비우면 오늘 날짜로 처리합니다.
 */
public final class PrinterDtos {

	private PrinterDtos() {
	}

	public record ShopInput(String code, String name, String division, String team, ShopType type,
		String managerName, String phone, String address, LocalDate openedOn) {
	}

	public record CycleInput(String month, LocalDate closesOn) {
	}

	public record DateInput(LocalDate date) {
	}

	public record RequestInput(java.util.UUID cycleId, java.util.UUID shopId, String applicantName,
		Integer androidQty, Integer iosQty) {
	}

	public record PaymentInput(Long amount, LocalDate date) {
	}

	public record PurchaseOrderInput(String orderNo, LocalDate orderedOn, Integer bufferAndroid,
		Integer bufferIos, String boxNote) {
	}

	public record ApprovalInput(String approvalNo, LocalDate date) {
	}

	/** serials를 주거나, autoAndroid/autoIos 대수만 주면 시리얼을 자동으로 만듭니다(체험용). */
	public record ReceiveInput(LocalDate date, List<String> serials, Integer autoAndroid, Integer autoIos) {
	}

	/** serials를 비우면 재고에서 기종에 맞게 자동 배정합니다. */
	public record AssignInput(List<String> serials) {
	}

	public record ShipInput(String trackingNo, LocalDate date) {
	}

	public record InvoiceRequestInput(InvoiceType type, LocalDate plannedOn) {
	}

	public record ReasonInput(String reason, LocalDate date) {
	}

	public record ModelChangeInput(Integer androidQty, Integer iosQty) {
	}

	public record ReturnToSenderInput(String reason, String correctedAddress, LocalDate date) {
	}

	public record RefundInput(Long amount, LocalDate date) {
	}

	/** newSerial을 비우면 재고에서 같은 기종을 자동으로 골라 보냅니다. */
	public record ExchangeInput(String newSerial, String reason, LocalDate date) {
	}

	public record RepairResultInput(Boolean repaired, LocalDate date) {
	}

	public record ModelCount(int android, int ios) {
		public int total() {
			return android + ios;
		}
	}

	public record Suggestion(String level, String message, String targetType, String targetId) {
	}

	public record Dashboard(
		LocalDate asOf,
		String month,
		Map<String, Long> requestsByStatus,
		ModelCount deliveredThisMonth,
		ModelCount invoicedThisMonth,
		ModelCount stock,
		long receivablesAmount,
		int receivablesCount,
		long overdueAmount,
		int overdueCount,
		int unrecoveredExchanges,
		int openRepairs,
		long pendingRefunds,
		List<Suggestion> suggestions) {
	}

	public record ShopMonthRow(String shopCode, String shopName, int android, int ios) {
	}

	public record MonthlyReport(
		String month,
		ModelCount salesBasis,
		ModelCount accountingBasis,
		List<ShopMonthRow> byShop,
		long revenue,
		long cost,
		long profit,
		int cancelled,
		int returns,
		int exchanges,
		int repairs,
		long adjustments) {
	}
}
