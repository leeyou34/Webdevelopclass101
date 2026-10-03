package io.github.leeyou34.todo.printer;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.leeyou34.todo.common.ApiException;
import io.github.leeyou34.todo.printer.Enums.CaseStatus;
import io.github.leeyou34.todo.printer.Enums.CaseType;
import io.github.leeyou34.todo.printer.Enums.CycleStatus;
import io.github.leeyou34.todo.printer.Enums.InvoiceStatus;
import io.github.leeyou34.todo.printer.Enums.InvoiceType;
import io.github.leeyou34.todo.printer.Enums.RequestStatus;
import io.github.leeyou34.todo.printer.InsightDtos.Analytics;
import io.github.leeyou34.todo.printer.InsightDtos.OpsTask;
import io.github.leeyou34.todo.printer.InsightDtos.OpsTasks;
import io.github.leeyou34.todo.printer.InsightDtos.PeriodSummary;
import io.github.leeyou34.todo.printer.InsightDtos.SeriesPoint;
import io.github.leeyou34.todo.printer.InsightDtos.ShopRow;
import io.github.leeyou34.todo.printer.PrinterDtos.ModelCount;

/** 기간 분석(대시보드)과 일자·중요도별 운영 업무 목록(오늘의 할 일) */
@Service
@Transactional(readOnly = true)
public class PrinterInsightService {

	private static final Set<RequestStatus> CANCELLED = EnumSet.of(RequestStatus.CANCELLED_UNPAID,
		RequestStatus.CANCELLED, RequestStatus.REFUND_PENDING, RequestStatus.REFUNDED);
	/** 이 기간보다 짧으면 일 단위, 길면 월 단위로 묶습니다. */
	private static final int DAILY_MAX_DAYS = 45;
	private static final int MAX_DAYS = 3 * 366;

	private final ShopRepository shops;
	private final CycleRepository cycles;
	private final RequestRepository requests;
	private final PurchaseOrderRepository orders;
	private final CaseRepository cases;
	private final InvoiceRepository invoices;
	private final ActivityRepository activities;
	private final DepositRepository deposits;

	public PrinterInsightService(ShopRepository shops, CycleRepository cycles, RequestRepository requests,
		PurchaseOrderRepository orders, CaseRepository cases, InvoiceRepository invoices,
		ActivityRepository activities, DepositRepository deposits) {
		this.shops = shops;
		this.cycles = cycles;
		this.requests = requests;
		this.orders = orders;
		this.cases = cases;
		this.invoices = invoices;
		this.activities = activities;
		this.deposits = deposits;
	}

	// ===================================================================== 기간 분석

	public Analytics analytics(UUID owner, LocalDate fromInput, LocalDate toInput) {
		LocalDate today = LocalDate.now();
		LocalDate to = toInput == null ? today : toInput;
		LocalDate from = fromInput == null ? to.withDayOfMonth(1) : fromInput;
		if (from.isAfter(to)) {
			throw ApiException.badRequest("시작일이 종료일보다 늦습니다.");
		}
		long days = ChronoUnit.DAYS.between(from, to) + 1;
		if (days > MAX_DAYS) {
			throw ApiException.badRequest("기간은 3년 이내로 정해 주세요.");
		}
		LocalDate prevTo = from.minusDays(1);
		LocalDate prevFrom = prevTo.minusDays(days - 1);

		Data d = load(owner);
		boolean daily = days <= DAILY_MAX_DAYS;
		List<SeriesPoint> series = new ArrayList<>();
		if (daily) {
			for (LocalDate x = from; !x.isAfter(to); x = x.plusDays(1)) {
				series.add(point(d, x.toString(), x.getMonthValue() + "/" + x.getDayOfMonth(), x, x));
			}
		} else {
			for (YearMonth m = YearMonth.from(from); !m.isAfter(YearMonth.from(to)); m = m.plusMonths(1)) {
				LocalDate a = max(m.atDay(1), from);
				LocalDate b = min(m.atEndOfMonth(), to);
				series.add(point(d, m.toString(), m.getYear() % 100 + "." + m.getMonthValue(), a, b));
			}
		}

		Map<UUID, int[]> units = new LinkedHashMap<>();
		Map<UUID, Long> revenue = new HashMap<>();
		for (OrderRequest r : d.requests) {
			if (delivered(r, from, to)) {
				int[] u = units.computeIfAbsent(r.shopId, k -> new int[2]);
				u[0] += r.androidQty;
				u[1] += r.iosQty;
				revenue.merge(r.shopId, r.personalAmount + r.hqAmount, Long::sum);
			}
		}
		List<ShopRow> byShop = new ArrayList<>();
		for (Map.Entry<UUID, int[]> e : units.entrySet()) {
			Shop s = d.shops.get(e.getKey());
			byShop.add(new ShopRow(s == null ? "" : s.code, s == null ? "(삭제됨)" : s.name, e.getValue()[0],
				e.getValue()[1], revenue.getOrDefault(e.getKey(), 0L)));
		}
		byShop.sort(Comparator.comparingInt((ShopRow r) -> -(r.android() + r.ios())).thenComparing(ShopRow::shopCode));

		return new Analytics(from, to, daily ? "day" : "month", summary(d, from, to), prevFrom, prevTo,
			summary(d, prevFrom, prevTo), series, byShop);
	}

	private PeriodSummary summary(Data d, LocalDate from, LocalDate to) {
		int requested = 0;
		int cancelled = 0;
		int da = 0;
		int di = 0;
		int ia = 0;
		int ii = 0;
		long revenue = 0;
		long cost = 0;
		long collected = 0;
		for (OrderRequest r : d.requests) {
			OrderCycle c = d.cycles.get(r.cycleId);
			if (c != null && monthOverlaps(c.month, from, to)) {
				requested++;
				if (CANCELLED.contains(r.status)) {
					cancelled++;
				}
			}
			if (delivered(r, from, to)) {
				da += r.androidQty;
				di += r.iosQty;
				revenue += r.personalAmount + r.hqAmount;
				cost += r.androidQty * Pricing.COST_ANDROID + r.iosQty * Pricing.COST_IOS;
			}
			if (between(r.invoicedOn, from, to)) {
				ia += r.androidQty;
				ii += r.iosQty;
			}
			if (between(r.paidOn, from, to)) {
				collected += Pricing.withoutVat(r.paidAmount); // 개인 입금은 VAT 포함으로 받으므로 공급가액으로 환산
			}
		}
		for (Invoice i : d.invoices) {
			if (i.type == InvoiceType.HQ && i.status == InvoiceStatus.PAID && between(i.paidOn, from, to)) {
				collected += i.amount;
			}
		}
		int returns = 0;
		int exchanges = 0;
		int repairs = 0;
		for (ServiceCase k : d.cases) {
			if (between(k.openedOn, from, to)) {
				switch (k.type) {
					case RETURN -> returns++;
					case EXCHANGE -> exchanges++;
					case REPAIR -> repairs++;
				}
			}
		}
		return new PeriodSummary(requested, cancelled, new ModelCount(da, di), new ModelCount(ia, ii), revenue, cost,
			revenue - cost, collected, returns, exchanges, repairs);
	}

	private SeriesPoint point(Data d, String key, String label, LocalDate from, LocalDate to) {
		PeriodSummary s = summary(d, from, to);
		return new SeriesPoint(key, label, s.delivered().android(), s.delivered().ios(), s.revenue(), s.profit(),
			s.collected());
	}

	private static boolean delivered(OrderRequest r, LocalDate from, LocalDate to) {
		return between(r.deliveredOn, from, to) && !CANCELLED.contains(r.status);
	}

	// ===================================================================== 운영 업무(할 일)

	public OpsTasks tasks(UUID owner, LocalDate asOfInput) {
		LocalDate asOf = asOfInput == null ? LocalDate.now() : asOfInput;
		Data d = load(owner);
		List<OpsTask> out = new ArrayList<>();

		for (OrderCycle c : d.cycles.values()) {
			YearMonth m = YearMonth.parse(c.month);
			String cid = c.id.toString();
			List<OrderRequest> rs = d.requests.stream().filter(r -> r.cycleId.equals(c.id)).toList();
			if (c.status == CycleStatus.OPEN) {
				long applied = rs.stream().filter(r -> r.status == RequestStatus.APPLIED).count();
				if (applied > 0) {
					out.add(task("pay-" + cid, c.closesOn, 2, "신청·입금", c.month + " 입금 확인 " + applied + "건",
						"마감 전에 입금을 확인하지 않으면 마감 때 자동 취소됩니다.", "orders", cid, null, null));
				}
				out.add(task("close-" + cid, c.closesOn, 2, "신청·입금", c.month + " 신청 마감",
					"신청 " + rs.size() + "건 · 마감일 " + c.closesOn, "orders", cid, null, null));
			}
			long paid = rs.stream().filter(r -> r.status == RequestStatus.PAID).count();
			if (c.status == CycleStatus.CLOSED && c.confirmedOn == null && paid > 0) {
				out.add(task("confirm-" + cid, c.closesOn.plusDays(1), 2, "발주", c.month + " 배송 리스트 확정",
					"입금 확인된 " + paid + "건을 이번 달 배송 대상으로 확정합니다.", "orders", cid, null, null));
			}
			List<PurchaseOrder> pos = orders.findByOwnerIdAndCycleId(d.owner, c.id);
			long confirmed = rs.stream().filter(r -> r.status == RequestStatus.CONFIRMED).count();
			if (confirmed > 0 && pos.isEmpty()) {
				out.add(task("po-" + cid, c.closesOn.plusDays(1), 2, "발주", c.month + " 발주서 작성",
					"확정 " + confirmed + "건 기준으로 제조사에 발주합니다.", "orders", cid, null, null));
			}
			for (PurchaseOrder po : pos) {
				if (po.approvalNo == null) {
					out.add(task("approve-" + po.id, po.orderedOn.plusDays(1), 3, "발주", po.orderNo + " 품의 기록",
						"사내 품의 번호를 남깁니다.", "orders", cid, po.id.toString(), null));
				}
				int left = po.androidQty + po.iosQty - po.receivedAndroid - po.receivedIos;
				if (left > 0) {
					out.add(task("receive-" + po.id, max(m.atDay(20), po.orderedOn), 2, "입고",
						po.orderNo + " 입고 " + left + "대 남음", "제조사에서 받은 시리얼을 등록합니다.", "orders", cid,
						po.id.toString(), null));
				}
			}
			long toShip = rs.stream().filter(r -> r.status == RequestStatus.ORDERED).count();
			if (toShip > 0) {
				out.add(task("ship-" + cid, m.atEndOfMonth(), 2, "배송", c.month + " 시리얼 배정·발송 " + toShip + "건",
					"송장 번호를 넣고 발송합니다.", "orders", cid, null, null));
			}
			YearMonth next = m.plusMonths(1);
			long toInvoice = rs.stream().filter(r -> r.status == RequestStatus.DELIVERED && r.personalAmount > 0
				&& d.invoices.stream().noneMatch(i -> r.id.equals(i.requestId))).count();
			if (toInvoice > 0) {
				out.add(task("inv-p-" + cid, next.atDay(10), 2, "계산서", c.month + " 개인 앞 계산서 요청 " + toInvoice + "건",
					"배송 완료된 특약점 신청의 계산서를 요청합니다.", "orders", cid, null, null));
			}
			boolean hqNeeded = rs.stream().anyMatch(r -> !CANCELLED.contains(r.status) && r.hqAmount > 0);
			boolean allDelivered = rs.stream().filter(r -> !CANCELLED.contains(r.status))
				.allMatch(r -> r.deliveredOn != null);
			boolean hqRequested = d.invoices.stream().anyMatch(i -> i.type == InvoiceType.HQ && c.id.equals(i.cycleId));
			if (hqNeeded && allDelivered && !hqRequested && c.confirmedOn != null) {
				out.add(task("inv-h-" + cid, next.atDay(10), 2, "계산서", c.month + " 본사 앞 계산서 요청",
					"iOS 차액과 직영 영업소 대금을 본사에 청구합니다.", "orders", cid, null, null));
			}
		}

		for (OrderRequest r : d.requests) {
			String who = shopName(d, r) + " " + r.applicantName;
			String cid = r.cycleId.toString();
			String rid = r.id.toString();
			if (r.status == RequestStatus.SHIPPING) {
				out.add(task("deliver-" + rid, r.shippedOn == null ? asOf : r.shippedOn.plusDays(3), 3, "배송",
					who + " 배송 완료 확인", "송장 " + nz(r.trackingNo), "orders", cid, rid, null));
			} else if (r.status == RequestStatus.RETURNED_TO_SENDER) {
				out.add(task("resend-" + rid, asOf, 1, "배송", who + " 반송 건 재발송", "주소를 확인하고 다시 보냅니다.",
					"orders", cid, rid, null));
			}
			long owed = r.refundDue - r.refundedTotal;
			if (owed > 0) {
				out.add(task("refund-" + rid, asOf, 1, "환불", who + "에게 " + PrinterService.won(owed) + " 환불",
					"고객에게 돌려줄 돈입니다.", "orders", cid, rid, null));
			}
		}

		for (Invoice i : d.invoices) {
			String cid = i.cycleId == null ? null : i.cycleId.toString();
			String who = i.type == InvoiceType.HQ ? "본사 앞" : "개인 앞";
			if (i.status == InvoiceStatus.REQUESTED) {
				out.add(task("issue-" + i.id, i.plannedOn == null ? asOf : i.plannedOn, 2, "계산서",
					who + " 계산서 발행 " + PrinterService.won(i.amount), "발행 처리합니다.", "orders", cid,
					i.id.toString(), null));
			} else if (i.type == InvoiceType.HQ && i.status == InvoiceStatus.ISSUED && i.issuedOn != null) {
				LocalDate due = i.issuedOn.plusDays(PrinterService.OVERDUE_DAYS);
				out.add(task("collect-" + i.id, due, due.isBefore(asOf) ? 1 : 2, "수금",
					"본사 수금 " + PrinterService.won(i.amount),
					due.isBefore(asOf) ? "기한(" + due + ")이 지났습니다. 독촉하세요." : "발행일 " + i.issuedOn + " · 기한 " + due,
					"orders", cid, i.id.toString(), null));
			}
		}

		for (ServiceCase k : d.cases) {
			if (k.status != CaseStatus.OPEN) {
				continue;
			}
			String id = k.id.toString();
			switch (k.type) {
				case EXCHANGE -> out.add(task("recover-" + id, k.openedOn.plusDays(7), 2, "사후 처리",
					"불량 기기 회수 " + k.serial, "교환 기기(" + nz(k.newSerial) + ")는 보냈고, 불량 기기를 돌려받아야 합니다.",
					"devices", null, id, null));
				case RETURN -> out.add(task("recover-" + id, k.openedOn.plusDays(7), 2, "사후 처리", "반품 기기 회수 " + k.serial,
					"기기가 도착하면 회수 확인을 누릅니다. " + nz(k.reason), "devices", null, id, null));
				case REPAIR -> out.add(task("repair-" + id, k.openedOn.plusDays(14), 3, "사후 처리", "AS 결과 등록 " + k.serial,
					nz(k.reason) + (Boolean.TRUE.equals(k.freeWarranty) ? " · 무상" : " · 유상"), "devices", null, id, null));
			}
		}

		List<Deposit> unmatched = deposits.findByOwnerIdOrderByDepositedOnDescCreatedAtDesc(owner).stream()
			.filter(x -> x.requestId == null).toList();
		if (!unmatched.isEmpty()) {
			LocalDate oldest = unmatched.stream().map(x -> x.depositedOn).min(LocalDate::compareTo).orElse(asOf);
			out.add(task("deposits", oldest.plusDays(1), 1, "입금", "미확인 입금 " + unmatched.size() + "건 확인",
				"누구 돈인지 맞추지 못한 입금입니다. 신청 건과 연결하거나 입금자에게 확인하세요.", "deposits", null, null, null));
		}

		YearMonth last = YearMonth.from(asOf).minusMonths(1);
		boolean hasLast = d.cycles.values().stream().anyMatch(c -> c.month.equals(last.toString()));
		boolean closed = activities.findTop200ByOwnerIdOrderByCreatedAtDesc(owner).stream()
			.anyMatch(a -> a.action == 21 && a.detail != null && a.detail.startsWith(last.toString()));
		if (hasLast && !closed) {
			out.add(task("close-month-" + last, YearMonth.from(asOf).atDay(5), 3, "월 마감", last + " 월 마감 보고",
				"영업 기준·회계 기준 대수와 수익을 확인하고 마감을 기록합니다.", "report", null, null, last.toString()));
		}

		out.sort(Comparator.comparing(OpsTask::dueOn).thenComparingInt(OpsTask::priority).thenComparing(OpsTask::key));
		return new OpsTasks(asOf, out);
	}

	// ===================================================================== 내부 도구

	private record Data(UUID owner, Map<UUID, Shop> shops, Map<UUID, OrderCycle> cycles, List<OrderRequest> requests,
		List<Invoice> invoices, List<ServiceCase> cases) {
	}

	private Data load(UUID owner) {
		Map<UUID, Shop> s = new HashMap<>();
		for (Shop x : shops.findByOwnerIdOrderByCodeAsc(owner)) {
			s.put(x.id, x);
		}
		Map<UUID, OrderCycle> c = new LinkedHashMap<>();
		List<OrderCycle> cs = new ArrayList<>(cycles.findByOwnerIdOrderByMonthDesc(owner));
		cs.sort(Comparator.comparing((OrderCycle x) -> x.month));
		for (OrderCycle x : cs) {
			c.put(x.id, x);
		}
		return new Data(owner, s, c, requests.findByOwnerIdOrderByCreatedAtAsc(owner),
			invoices.findByOwnerIdOrderByCreatedAtAsc(owner), cases.findByOwnerIdOrderByOpenedOnDesc(owner));
	}

	private static OpsTask task(String key, LocalDate due, int priority, String category, String title, String detail,
		String page, String cycleId, String focusId, String month) {
		return new OpsTask(key, due, priority, category, title, detail, page, cycleId, focusId, month);
	}

	private static String shopName(Data d, OrderRequest r) {
		Shop s = d.shops.get(r.shopId);
		return s == null ? "" : s.name;
	}

	private static boolean monthOverlaps(String month, LocalDate from, LocalDate to) {
		YearMonth m = YearMonth.parse(month);
		return !m.atEndOfMonth().isBefore(from) && !m.atDay(1).isAfter(to);
	}

	private static boolean between(LocalDate d, LocalDate from, LocalDate to) {
		return d != null && !d.isBefore(from) && !d.isAfter(to);
	}

	private static LocalDate max(LocalDate a, LocalDate b) {
		return a.isAfter(b) ? a : b;
	}

	private static LocalDate min(LocalDate a, LocalDate b) {
		return a.isBefore(b) ? a : b;
	}

	private static String nz(String s) {
		return s == null ? "" : s;
	}
}
