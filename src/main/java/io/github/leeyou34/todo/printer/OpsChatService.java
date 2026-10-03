package io.github.leeyou34.todo.printer;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.leeyou34.todo.common.ApiException;
import io.github.leeyou34.todo.printer.Enums.CaseStatus;
import io.github.leeyou34.todo.printer.Enums.CaseType;
import io.github.leeyou34.todo.printer.Enums.DeviceStatus;
import io.github.leeyou34.todo.printer.Enums.InvoiceStatus;
import io.github.leeyou34.todo.printer.Enums.InvoiceType;
import io.github.leeyou34.todo.printer.Enums.RequestStatus;
import io.github.leeyou34.todo.printer.ExtraDtos.ChatLink;
import io.github.leeyou34.todo.printer.ExtraDtos.ChatReply;
import io.github.leeyou34.todo.printer.ExtraDtos.DeviceLookup;
import io.github.leeyou34.todo.printer.InsightDtos.Analytics;
import io.github.leeyou34.todo.printer.InsightDtos.OpsTask;
import io.github.leeyou34.todo.printer.InsightDtos.PeriodSummary;

/**
 * 업무 전용 안내 챗봇(규칙 기반). 외부 AI를 쓰지 않고, 질문에서 업무 낱말을 찾아
 * 이 계정의 실제 데이터와 업무 매뉴얼(2017 인수인계 가이드서 기준)로 답합니다.
 * 질문 내용은 서버 밖으로 나가지 않습니다.
 */
@Service
@Transactional(readOnly = true)
public class OpsChatService {

	private static final Pattern SERIAL = Pattern.compile("\\b(AM[A-Z0-9]{5}\\d{6,})\\b", Pattern.CASE_INSENSITIVE);
	private static final Pattern MONTH = Pattern.compile("(?:(\\d{4})년\\s*)?(\\d{1,2})월");

	static final List<String> EXAMPLES = List.of("오늘 할 일 알려줘", "미수금 현황", "재고 몇 대야?", "지난달 실적",
		"반품 절차", "가격표", "AMR7OKA 시리얼 조회 (시리얼 입력)", "미확인 입금");

	private final PrinterInsightService insight;
	private final OpsExtrasService extras;
	private final ShopRepository shops;
	private final RequestRepository requests;
	private final DeviceRepository devices;
	private final InvoiceRepository invoices;
	private final CaseRepository cases;
	private final DepositRepository deposits;
	private final PurchaseOrderRepository orders;

	public OpsChatService(PrinterInsightService insight, OpsExtrasService extras,
		ShopRepository shops, RequestRepository requests, DeviceRepository devices, InvoiceRepository invoices,
		CaseRepository cases, DepositRepository deposits, PurchaseOrderRepository orders) {
		this.insight = insight;
		this.extras = extras;
		this.shops = shops;
		this.requests = requests;
		this.devices = devices;
		this.invoices = invoices;
		this.cases = cases;
		this.deposits = deposits;
		this.orders = orders;
	}

	public ChatReply reply(UUID owner, String message, LocalDate today) {
		String raw = message == null ? "" : message.trim();
		if (raw.length() > 300) {
			raw = raw.substring(0, 300);
		}
		String q = raw.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
		LocalDate asOf = today == null ? LocalDate.now() : today;

		if (q.isEmpty() || has(q, "도움", "help", "뭘물어", "무엇을물어", "사용법", "안녕")) {
			return help();
		}
		Matcher sm = SERIAL.matcher(raw);
		if (sm.find()) {
			return serial(owner, sm.group(1).toUpperCase(Locale.ROOT));
		}
		boolean howTo = has(q, "절차", "어떻게", "방법", "순서", "하나요", "해야돼", "해야해", "해야하");
		if (has(q, "직영", "리리코스") && (howTo || has(q, "입금", "청구", "계산서"))) {
			return channelGuide();
		}
		if (has(q, "미입금", "입금안", "입금대기", "입금전", "안낸", "입금을안", "입금기다")) {
			return waitingPayments(owner);
		}
		if (has(q, "미확인", "입금자")) {
			return unmatchedDeposits(owner);
		}
		if (has(q, "반품", "교환", "as", "a/s", "수리", "고장", "불량")) {
			return howTo ? afterSalesGuide(q) : afterSalesStatus(owner, q);
		}
		if (has(q, "환불")) {
			return howTo ? refundGuide() : refunds(owner);
		}
		if (has(q, "할일", "할거", "업무", "밀린", "챙길", "오늘뭐", "뭐해")) {
			return tasks(owner, asOf);
		}
		if (has(q, "미수", "수금", "독촉", "안들어온", "못받")) {
			return receivables(owner, asOf);
		}
		if (has(q, "재고")) {
			return stock(owner);
		}
		if (has(q, "수익", "이익", "마진", "벌었", "남았")) {
			return performance(owner, raw, q, asOf);
		}
		if (has(q, "가격", "단가", "얼마", "금액표", "가격표", "vat")) {
			return prices();
		}
		if (has(q, "신청기간", "마감", "일정", "언제", "달력", "스케줄")) {
			return schedule();
		}
		if (has(q, "박스", "포장")) {
			return boxes();
		}
		if (has(q, "실적", "매출", "수익", "몇대", "대수", "배송량", "얼마나팔")) {
			return performance(owner, raw, q, asOf);
		}
		if (has(q, "계산서")) {
			return howTo ? invoiceGuide() : invoiceStatus(owner);
		}
		if (has(q, "발주", "품의")) {
			return howTo ? orderGuide() : orderStatus(owner);
		}
		ChatReply shop = shopSummary(owner, raw, asOf);
		if (shop != null) {
			return shop;
		}
		ChatReply person = applicant(owner, raw);
		if (person != null) {
			return person;
		}
		return new ChatReply("질문을 업무 항목과 연결하지 못했습니다. 아래처럼 물어봐 주세요. 시리얼 번호를 그대로 붙여 넣어도 됩니다.",
			List.of(), EXAMPLES);
	}

	// ----------------------------------------------------------------- 데이터 답변

	private ChatReply tasks(UUID owner, LocalDate asOf) {
		List<OpsTask> all = insight.tasks(owner, asOf).tasks();
		List<OpsTask> due = all.stream().filter(t -> !t.dueOn().isAfter(asOf.plusDays(3))).toList();
		if (due.isEmpty()) {
			return reply("앞으로 3일 안에 할 일이 없습니다." + (all.isEmpty() ? "" : " 그 뒤로 " + all.size() + "건이 있습니다."),
				List.of(new ChatLink("할 일 전체 보기", "/")), List.of("미수금 현황", "재고 몇 대야?"));
		}
		long overdue = due.stream().filter(t -> t.dueOn().isBefore(asOf)).count();
		StringBuilder sb = new StringBuilder();
		sb.append("3일 안에 할 일 ").append(due.size()).append("건");
		if (overdue > 0) {
			sb.append(" (밀린 일 ").append(overdue).append("건 포함)");
		}
		sb.append("입니다. 급한 순서로 보여 드립니다.\n");
		List<ChatLink> links = new ArrayList<>();
		due.stream().sorted(Comparator.comparingInt(OpsTask::priority).thenComparing(OpsTask::dueOn)).limit(6)
			.forEach(t -> {
				sb.append("• [").append(t.priority() == 1 ? "긴급" : t.priority() == 2 ? "중요" : "보통").append("] ")
					.append(t.title()).append(" — ").append(dueText(t.dueOn(), asOf)).append('\n');
				if (links.size() < 3) {
					links.add(new ChatLink(t.title(), link(t)));
				}
			});
		return reply(sb.toString().trim(), links, List.of("미수금 현황", "미확인 입금"));
	}

	private ChatReply receivables(UUID owner, LocalDate asOf) {
		List<Invoice> open = invoices.findByOwnerIdOrderByCreatedAtAsc(owner).stream()
			.filter(i -> i.type == InvoiceType.HQ && i.status == InvoiceStatus.ISSUED).toList();
		long unmatched = deposits.findByOwnerIdOrderByDepositedOnDescCreatedAtDesc(owner).stream()
			.filter(d -> d.requestId == null).count();
		long waitingPay = requests.findByOwnerIdAndStatus(owner, RequestStatus.APPLIED).stream()
			.filter(r -> r.personalAmount > 0).count();
		StringBuilder sb = new StringBuilder();
		if (open.isEmpty()) {
			sb.append("본사 앞 미수금은 없습니다.");
		} else {
			long total = open.stream().mapToLong(i -> i.amount).sum();
			sb.append("본사 앞 미수금은 ").append(open.size()).append("건, ").append(PrinterService.won(total))
				.append("(VAT 별도, 받을 돈은 VAT 포함 ").append(PrinterService.won(Pricing.withVat(total)))
				.append(")입니다.\n");
			for (Invoice i : open) {
				LocalDate due = i.issuedOn.plusDays(PrinterService.OVERDUE_DAYS);
				sb.append("• 발행 ").append(i.issuedOn).append(" · ").append(PrinterService.won(i.amount)).append(" · ")
					.append(due.isBefore(asOf) ? "기한 " + due + " 지남, 독촉 필요" : "기한 " + due).append('\n');
			}
		}
		sb.append("\n입금을 기다리는 신청은 ").append(waitingPay).append("건");
		if (unmatched > 0) {
			sb.append(", 누구 돈인지 확인 못 한 입금이 ").append(unmatched).append("건");
		}
		sb.append(" 있습니다.");
		return reply(sb.toString().trim(), List.of(new ChatLink("계산서 화면", "/ops/orders"),
			new ChatLink("입금 대조 화면", "/ops/deposits")), List.of("미확인 입금", "계산서 일정"));
	}

	private ChatReply waitingPayments(UUID owner) {
		Map<UUID, String> names = new HashMap<>();
		shops.findByOwnerIdOrderByCodeAsc(owner).forEach(x -> names.put(x.id, x.name));
		List<OrderRequest> list = requests.findByOwnerIdAndStatus(owner, RequestStatus.APPLIED).stream()
			.filter(r -> r.personalAmount > 0).toList();
		if (list.isEmpty()) {
			return reply("입금을 기다리는 신청이 없습니다.", List.of(new ChatLink("입금 대조 화면", "/ops/deposits")),
				List.of("미확인 입금", "오늘 할 일"));
		}
		StringBuilder sb = new StringBuilder("아직 입금이 확인되지 않은 신청이 " + list.size() + "건입니다(받을 돈은 VAT 포함).\n");
		list.stream().limit(10).forEach(r -> sb.append("• ").append(names.getOrDefault(r.shopId, "")).append(' ')
			.append(r.applicantName).append(" · ").append(PrinterService.won(Pricing.withVat(r.personalAmount))).append('\n'));
		sb.append("마감일까지 입금되지 않으면 마감 때 자동 취소됩니다.");
		return reply(sb.toString(), List.of(new ChatLink("입금 대조 화면", "/ops/deposits"),
			new ChatLink("신청 화면", "/ops/orders")), List.of("미확인 입금", "신청 마감 언제야?"));
	}

	private ChatReply channelGuide() {
		return reply("""
			영업장 구분별 대금 처리입니다(공급가액, VAT 별도).
			• 방판 직영 영업소: 개인 입금이 없습니다. 신청을 넣으면 바로 "입금 확인" 상태가 되고, 대금(안드로이드 143,000 / iOS 173,000)은 다음 달 본사 앞 계산서로 청구합니다.
			• 리리코스 지사: 개인이 안드로이드 54,000 / iOS 64,000을 VAT 포함(59,400 / 70,400)으로 입금하고, 나머지(89,000 / 109,000)는 본사 앞 계산서로 청구합니다.
			• 방판 특약점: 개인이 143,000(VAT 포함 157,300)을 입금하고, iOS는 차액 30,000을 본사 앞 계산서로 청구합니다.""",
			List.of(new ChatLink("영업장 화면", "/ops/shops")), List.of("가격표", "계산서 일정"));
	}

	private ChatReply unmatchedDeposits(UUID owner) {
		List<Deposit> list = deposits.findByOwnerIdOrderByDepositedOnDescCreatedAtDesc(owner).stream()
			.filter(d -> d.requestId == null).toList();
		if (list.isEmpty()) {
			return reply("확인 못 한 입금은 없습니다. 통장 입금 내역은 \"입금 대조\" 화면에 붙여 넣으면 신청 건과 자동으로 맞춥니다.",
				List.of(new ChatLink("입금 대조 화면", "/ops/deposits")), List.of("미수금 현황"));
		}
		StringBuilder sb = new StringBuilder("누구 돈인지 확인 못 한 입금이 " + list.size() + "건 있습니다.\n");
		list.stream().limit(6).forEach(d -> sb.append("• ").append(d.depositedOn).append(" · ").append(d.depositorName)
			.append(" · ").append(PrinterService.won(d.amount)).append(d.note == null ? "" : " (" + d.note + ")").append('\n'));
		sb.append("입금자명은 \"영업장명+신청자명\"으로 받는 것이 원칙입니다. 화면에서 맞는 신청 건을 골라 연결하세요.");
		return reply(sb.toString(), List.of(new ChatLink("입금 대조 화면", "/ops/deposits")), List.of("미수금 현황"));
	}

	private ChatReply stock(UUID owner) {
		List<Device> all = devices.findByOwnerIdOrderBySerialAsc(owner);
		long a = all.stream().filter(d -> d.status == DeviceStatus.IN_STOCK && d.model == Enums.DeviceModel.ANDROID).count();
		long i = all.stream().filter(d -> d.status == DeviceStatus.IN_STOCK && d.model == Enums.DeviceModel.IOS).count();
		long assigned = all.stream().filter(d -> d.status == DeviceStatus.ASSIGNED).count();
		long waiting = requests.findByOwnerIdAndStatus(owner, RequestStatus.ORDERED).stream()
			.filter(r -> devices.findByOwnerIdAndRequestId(owner, r.id).isEmpty()).mapToInt(OrderRequest::totalQty).sum();
		String text = "재고는 " + (a + i) + "대입니다(안드로이드 " + a + " · iOS " + i + ").\n배정만 하고 아직 보내지 않은 기기 "
			+ assigned + "대, 시리얼 배정을 기다리는 신청 " + waiting + "대가 있습니다.";
		return reply(text, List.of(new ChatLink("기기 화면", "/ops/devices"), new ChatLink("배송 작업(스캔)", "/ops/scan")),
			List.of("오늘 할 일 알려줘"));
	}

	private ChatReply refunds(UUID owner) {
		List<OrderRequest> list = requests.findByOwnerIdOrderByCreatedAtAsc(owner).stream()
			.filter(r -> r.refundDue > r.refundedTotal).toList();
		if (list.isEmpty()) {
			return reply("돌려줄 돈이 남은 신청은 없습니다.", List.of(), List.of("환불 절차"));
		}
		StringBuilder sb = new StringBuilder("환불할 신청이 " + list.size() + "건 있습니다(VAT 포함 금액).\n");
		list.stream().limit(6).forEach(r -> sb.append("• ").append(shopName(owner, r)).append(' ').append(r.applicantName)
			.append(" · ").append(PrinterService.won(r.refundDue - r.refundedTotal)).append('\n'));
		return reply(sb.toString().trim(), List.of(new ChatLink("신청 화면", "/ops/orders")), List.of("환불 절차"));
	}

	private ChatReply afterSalesStatus(UUID owner, String q) {
		List<ServiceCase> open = cases.findByOwnerIdOrderByOpenedOnDesc(owner).stream()
			.filter(k -> k.status == CaseStatus.OPEN).toList();
		long ex = open.stream().filter(k -> k.type == CaseType.EXCHANGE).count();
		long re = open.stream().filter(k -> k.type == CaseType.RETURN).count();
		long rp = open.stream().filter(k -> k.type == CaseType.REPAIR).count();
		String text = "진행 중인 사후 처리는 교환 회수 대기 " + ex + "건, 반품 회수 대기 " + re + "건, AS " + rp
			+ "건입니다. 절차가 궁금하면 \"반품 절차\", \"교환 절차\", \"AS 절차\"라고 물어보세요.";
		return reply(text, List.of(new ChatLink("기기·사후 처리 화면", "/ops/devices")),
			List.of("반품 절차", "교환 절차", "AS 절차"));
	}

	private ChatReply performance(UUID owner, String raw, String q, LocalDate asOf) {
		LocalDate from;
		LocalDate to;
		String label;
		Matcher m = MONTH.matcher(raw);
		if (m.find()) {
			int year = m.group(1) == null ? asOf.getYear() : Integer.parseInt(m.group(1));
			int month = Integer.parseInt(m.group(2));
			if (month < 1 || month > 12) {
				throw ApiException.badRequest("월은 1~12 사이로 물어봐 주세요.");
			}
			YearMonth ym = YearMonth.of(year, month);
			if (m.group(1) == null && ym.isAfter(YearMonth.from(asOf))) {
				ym = ym.minusYears(1);
			}
			from = ym.atDay(1);
			to = ym.atEndOfMonth();
			label = ym.getYear() + "년 " + ym.getMonthValue() + "월";
		} else if (has(q, "지난달", "전월", "저번달")) {
			YearMonth ym = YearMonth.from(asOf).minusMonths(1);
			from = ym.atDay(1);
			to = ym.atEndOfMonth();
			label = "지난달(" + ym + ")";
		} else if (has(q, "올해", "금년", "연간")) {
			from = asOf.withDayOfYear(1);
			to = asOf;
			label = "올해";
		} else {
			from = asOf.withDayOfMonth(1);
			to = asOf;
			label = "이번 달";
		}
		Analytics a = insight.analytics(owner, from, to);
		PeriodSummary s = a.summary();
		int units = s.delivered().android() + s.delivered().ios();
		int before = a.previous().delivered().android() + a.previous().delivered().ios();
		String text = label + " 배송 " + units + "대(안드로이드 " + s.delivered().android() + " · iOS " + s.delivered().ios()
			+ "), 매출 " + PrinterService.won(s.revenue()) + ", 수익 " + PrinterService.won(s.profit()) + ", 수금 "
			+ PrinterService.won(s.collected()) + "입니다(모두 VAT 별도).\n바로 앞 같은 기간은 " + before + "대였습니다."
			+ (s.cancelled() > 0 ? " 이 기간 신청 중 취소는 " + s.cancelled() + "건입니다." : "");
		return reply(text, List.of(new ChatLink("대시보드", "/ops"), new ChatLink("월 마감", "/ops/report")),
			List.of("올해 실적", "지난달 실적"));
	}

	private ChatReply invoiceStatus(UUID owner) {
		List<Invoice> list = invoices.findByOwnerIdOrderByCreatedAtAsc(owner);
		long requested = list.stream().filter(i -> i.status == InvoiceStatus.REQUESTED).count();
		long issuedHq = list.stream().filter(i -> i.type == InvoiceType.HQ && i.status == InvoiceStatus.ISSUED).count();
		return reply("발행을 기다리는 계산서 " + requested + "장, 발행했지만 본사 입금을 기다리는 계산서 " + issuedHq
			+ "장이 있습니다. 개인 앞(카운슬러)은 배송 완료 후, 본사 앞은 다음 달 10일 전후로 발행합니다.",
			List.of(new ChatLink("계산서 화면", "/ops/orders"), new ChatLink("세금계산서 발행리스트 엑셀", "/ops/documents")),
			List.of("계산서 절차", "미수금 현황"));
	}

	private ChatReply orderStatus(UUID owner) {
		List<PurchaseOrder> list = orders.findByOwnerIdOrderByOrderedOnDesc(owner);
		if (list.isEmpty()) {
			return reply("아직 발주서가 없습니다. 배송 리스트를 확정하면 발주서를 만들 수 있습니다.", List.of(new ChatLink("신청·발주", "/ops/orders")),
				List.of("발주 절차"));
		}
		PurchaseOrder po = list.get(0);
		int left = po.androidQty + po.iosQty - po.receivedAndroid - po.receivedIos;
		return reply("가장 최근 발주는 " + po.orderNo + "(" + po.orderedOn + ")입니다. 안드로이드 " + po.androidQty + "대, iOS "
			+ po.iosQty + "대이고, 품의 " + (po.approvalNo == null ? "미기록" : po.approvalNo) + ", 입고 "
			+ (left == 0 ? "완료" : left + "대 남음") + "입니다.",
			List.of(new ChatLink("신청·발주 화면", "/ops/orders"), new ChatLink("발주서·품의서 엑셀", "/ops/documents")),
			List.of("발주 절차"));
	}

	private ChatReply serial(UUID owner, String serial) {
		DeviceLookup l;
		try {
			l = extras.lookup(owner, serial);
		} catch (ApiException e) {
			Enums.DeviceModel model = Enums.DeviceModel.fromSerial(serial);
			return reply(serial + "은(는) 등록되지 않은 시리얼입니다."
				+ (model == null ? " 시리얼 형식도 확인해 주세요." : " 앞자리로 보면 " + model.productName + "입니다. 입고 처리가 안 된 기기일 수 있습니다."),
				List.of(new ChatLink("바코드 스캔", "/ops/scan")), List.of());
		}
		Device d = l.device();
		StringBuilder sb = new StringBuilder(d.serial + " · " + d.model.productName + "\n");
		sb.append("상태: ").append(PrinterService.label(d.status));
		if (l.shop() != null) {
			sb.append(" · 영업장: ").append(l.shop().name);
		}
		if (l.request() != null) {
			sb.append(" · 신청자: ").append(l.request().applicantName);
		}
		if (d.deliveredOn != null) {
			sb.append("\n수령일: ").append(d.deliveredOn);
		}
		if (d.producedOn != null) {
			LocalDate warranty = d.producedOn.plusYears(2);
			sb.append(" · 무상 AS: ").append(warranty).append("까지");
		}
		if (l.purchaseOrder() != null) {
			sb.append("\n발주: ").append(l.purchaseOrder().orderNo);
		}
		if (!l.cases().isEmpty()) {
			sb.append("\n사후 처리 이력 ").append(l.cases().size()).append("건");
		}
		return reply(sb.toString(), List.of(new ChatLink("이 기기 보기", "/ops/devices?q=" + d.serial)),
			List.of("반품 절차", "AS 절차"));
	}

	private ChatReply shopSummary(UUID owner, String raw, LocalDate asOf) {
		String text = OpsExtrasService.norm(raw);
		Shop hit = null;
		for (Shop s : shops.findByOwnerIdOrderByCodeAsc(owner)) {
			String key = OpsExtrasService.norm(OpsExtrasService.shortName(s.name));
			if (key.length() >= 2 && text.contains(key)) {
				hit = s;
				break;
			}
		}
		if (hit == null) {
			return null;
		}
		Shop s = hit;
		List<OrderRequest> rs = requests.findByOwnerIdOrderByCreatedAtAsc(owner).stream()
			.filter(r -> r.shopId.equals(s.id)).toList();
		int year = rs.stream().filter(r -> r.deliveredOn != null && r.deliveredOn.getYear() == asOf.getYear())
			.mapToInt(OrderRequest::totalQty).sum();
		long open = rs.stream().filter(r -> !EnumSetHolder.DONE.contains(r.status)).count();
		long owed = rs.stream().mapToLong(r -> Math.max(0, r.refundDue - r.refundedTotal)).sum();
		String type = switch (s.type) {
			case DIRECT -> "방판 직영 영업소";
			case LIRICOS -> "리리코스 지사";
			default -> "방판 특약점";
		};
		String body = s.name + "(" + type + (s.active ? "" : ", 폐쇄 " + s.closedOn) + ")\n올해 받은 기기 " + year
			+ "대, 처리 중인 신청 " + open + "건" + (owed > 0 ? ", 환불할 돈 " + PrinterService.won(owed) : "") + "입니다."
			+ (s.phone == null ? "" : "\n연락처: " + s.phone);
		return reply(body, List.of(new ChatLink("영업장 화면", "/ops/shops")), List.of("오늘 할 일 알려줘"));
	}

	private ChatReply applicant(UUID owner, String raw) {
		String text = OpsExtrasService.norm(raw);
		List<OrderRequest> hits = requests.findByOwnerIdOrderByCreatedAtAsc(owner).stream()
			.filter(r -> r.applicantName != null && r.applicantName.length() >= 2
				&& text.contains(OpsExtrasService.norm(r.applicantName)))
			.toList();
		if (hits.isEmpty()) {
			return null;
		}
		OrderRequest r = hits.get(hits.size() - 1);
		String body = shopName(owner, r) + " " + r.applicantName + " 신청(안드로이드 " + r.androidQty + " · iOS " + r.iosQty
			+ ")은 지금 \"" + PrinterService.label(r.status) + "\" 상태입니다."
			+ (r.trackingNo == null ? "" : " 송장 " + r.trackingNo + ".")
			+ (hits.size() > 1 ? " (같은 이름의 신청이 " + hits.size() + "건 있어 가장 최근 건을 보여 드렸습니다.)" : "");
		return reply(body, List.of(new ChatLink("이 신청 보기", "/ops/orders?cycle=" + r.cycleId + "&focus=" + r.id)),
			List.of());
	}

	// ----------------------------------------------------------------- 매뉴얼 답변

	private static ChatReply help() {
		return new ChatReply("모바일 프린터 운영 업무만 답하는 안내 챗봇입니다. 이 계정의 실제 데이터와 업무 매뉴얼로 답하고, "
			+ "질문은 외부로 보내지 않습니다. 이런 것을 물어보세요.", List.of(), EXAMPLES);
	}

	private static ChatReply schedule() {
		return new ChatReply("""
			한 달 업무 일정입니다.
			• 1~7일: 신청과 입금(VAT 포함) 받기, 7일 마감 — 입금 안 된 신청은 마감 때 자동 취소
			• 8일: 배송 리스트 확정 → 빅솔론 발주서 발송·박스 요청 → 품의서 상신
			• 10일 전후: 전월 본사 앞 계산서 발행, 전월 개인 앞 계산서 마감 확인
			• 20일 전후: 기기 입고(발주 후 약 2주), 시리얼 정리
			• 말일까지: 송장 요청 → 영업장 앞 배송 → 고객지원팀에 배송 리스트 공유
			• 월말: 개인 앞 계산서 발행 요청, 출고 수량·본사 앞 계산서 발행분 보고, 수익 보고""",
			List.of(new ChatLink("할 일 보기", "/")), List.of("가격표", "박스 기준"));
	}

	private static ChatReply prices() {
		return new ChatReply("""
			가격표(공급가액, VAT 별도)입니다. 개인 입금은 VAT 포함 금액으로 받습니다(143,000 → 157,300).
			• 방판 특약점: 안드로이드 개인 143,000 / iOS 개인 143,000 + 본사 30,000
			• 방판 직영 영업소: 안드로이드 본사 143,000 / iOS 본사 173,000
			• 리리코스 지사: 안드로이드 개인 54,000 + 본사 89,000 / iOS 개인 64,000 + 본사 109,000
			• 매입(빅솔론): 안드로이드 130,000 / iOS 160,000 → 대당 수익 13,000""", List.of(),
			List.of("일정", "계산서 절차"));
	}

	private static ChatReply boxes() {
		return new ChatReply("포장 박스는 3개입·5개입·8개입 세 종류입니다. 영업장별 수량에 맞춰 빈칸이 가장 적게 남는 조합을 고르고, "
			+ "부족하면 발주 메일에 박스 요청을 함께 적습니다. 문서 출력의 송장요청서에 영업장별 박스 구성이 자동으로 계산되어 들어갑니다.",
			List.of(new ChatLink("문서 출력", "/ops/documents")), List.of("일정"));
	}

	private static ChatReply afterSalesGuide(String q) {
		if (has(q, "교환", "불량")) {
			return new ChatReply("""
				불량 교환 절차입니다.
				1) 같은 기종 재고로 교체 기기를 먼저 보냅니다(선배송).
				2) 불량 기기는 "회수 대기"로 두고, 돌려받으면 "회수 확인"을 누릅니다.
				3) 같은 발주분에서 불량이 몰리면(예: 2017년 5~7월 안드로이드 204대) 발주 단위로 교체 대상 시리얼 목록을 만들어 빅솔론과 공유합니다.""",
				List.of(new ChatLink("기기·사후 처리", "/ops/devices")), List.of("반품 절차", "AS 절차"));
		}
		if (has(q, "as", "a/s", "수리", "고장")) {
			return new ChatReply("""
				AS 절차입니다.
				1) 시리얼로 기기를 찾아 AS 접수(생산 후 2년 안이면 무상).
				2) 에버린트(빅솔론 서비스)로 기기를 보내고, 요청 내역은 "AS 요청현황" 엑셀로 공유합니다.
				3) 결과가 오면 "수리 완료" 또는 "수리 불가"를 등록합니다. 수리 불가면 교환으로 이어 갑니다.""",
				List.of(new ChatLink("기기·사후 처리", "/ops/devices"), new ChatLink("AS 요청현황 엑셀", "/ops/documents")),
				List.of("교환 절차"));
		}
		return new ChatReply("""
			반품 절차입니다.
			1) 시리얼로 기기를 찾아 반품 접수.
			2) 기기가 도착하면 "회수 확인" → 재고로 돌아가고, 신청 금액에서 1대분이 빠지며 환불 대기로 잡힙니다.
			3) 본사 앞 계산서가 이미 나갔으면 차감 조정이 자동으로 기록됩니다.
			4) 환불을 보내고 "환불"을 누릅니다. 계산서 발행 후 환불이면 다음 계산서에 조정이 반영됩니다.""",
			List.of(new ChatLink("기기·사후 처리", "/ops/devices")), List.of("환불 절차", "교환 절차"));
	}

	private static ChatReply refundGuide() {
		return new ChatReply("""
			환불 절차입니다.
			• 입금 후 취소, 기종 변경 차액, 반품 회수 때 돌려줄 돈이 생깁니다(VAT 포함 금액).
			• 신청 화면에서 "환불"을 눌러 보낸 금액을 기록합니다. 나눠서 보내도 됩니다.
			• 계산서가 이미 나간 건은 그 달 계산서에 조정이 들어갑니다(10일까지는 그 달, 이후는 다음 달).
			• 고객사 사정으로 발주분을 한꺼번에 돌려줄 때는 "환불 품의서"를 엑셀로 만들어 상신합니다.""",
			List.of(new ChatLink("신청 화면", "/ops/orders"), new ChatLink("환불 품의서 엑셀", "/ops/documents")),
			List.of("반품 절차"));
	}

	private static ChatReply invoiceGuide() {
		return new ChatReply("""
			계산서 절차입니다.
			• 개인 앞(카운슬러): 배송이 끝나면 신청자별로 발행을 요청합니다. 발행리스트 엑셀을 경영지원팀에 보냅니다.
			• 본사 앞: iOS 차액(대당 30,000)과 직영 영업소 대금을 모아 다음 달 10일 전후로 발행하고, 본사 담당자에게 내역을 공유합니다.
			• 발행 후 30일이 지나도 본사 입금이 없으면 미수로 잡혀 독촉 대상이 됩니다.
			• 이미 발행한 뒤 바뀐 내용은 재발행하거나 다음 달 계산서에서 조정합니다.""",
			List.of(new ChatLink("세금계산서 발행리스트 엑셀", "/ops/documents")), List.of("미수금 현황"));
	}

	private static ChatReply orderGuide() {
		return new ChatReply("""
			발주·품의 절차입니다.
			1) 마감 후 입금 확인된 신청으로 배송 리스트를 확정합니다.
			2) 발주서를 만들어 빅솔론 담당자에게 메일로 보냅니다(발주 No. = ON-발주일, 단가 130,000/160,000, VAT 별도). 박스가 부족하면 함께 요청합니다.
			3) 그룹웨어에 품의서를 상신하고 발주서를 첨부합니다. 500대 이상이면 공장 직배송·출장 내용을 품의에 넣습니다.
			4) 품의 번호를 시스템에 기록합니다.""",
			List.of(new ChatLink("발주서·품의서 엑셀", "/ops/documents")), List.of("일정", "박스 기준"));
	}

	// ----------------------------------------------------------------- 도구

	private String shopName(UUID owner, OrderRequest r) {
		return shops.findByIdAndOwnerId(r.shopId, owner).map(s -> s.name).orElse("");
	}

	private static String link(OpsTask t) {
		if ("devices".equals(t.page())) {
			return "/ops/devices" + (t.focusId() == null ? "" : "?focus=" + t.focusId());
		}
		if ("report".equals(t.page())) {
			return "/ops/report" + (t.month() == null ? "" : "?month=" + t.month());
		}
		if ("deposits".equals(t.page())) {
			return "/ops/deposits";
		}
		StringBuilder sb = new StringBuilder("/ops/orders");
		String sep = "?";
		if (t.cycleId() != null) {
			sb.append(sep).append("cycle=").append(t.cycleId());
			sep = "&";
		}
		if (t.focusId() != null) {
			sb.append(sep).append("focus=").append(t.focusId());
		}
		return sb.toString();
	}

	private static String dueText(LocalDate due, LocalDate today) {
		long d = java.time.temporal.ChronoUnit.DAYS.between(today, due);
		if (d < 0) {
			return -d + "일 지남";
		}
		return d == 0 ? "오늘까지" : d == 1 ? "내일까지" : due.getMonthValue() + "/" + due.getDayOfMonth() + "까지";
	}

	private static boolean has(String q, String... words) {
		for (String w : words) {
			if (q.contains(w)) {
				return true;
			}
		}
		return false;
	}

	private static ChatReply reply(String text, List<ChatLink> links, List<String> suggestions) {
		return new ChatReply(text, links, suggestions);
	}

	private static final class EnumSetHolder {
		static final java.util.Set<RequestStatus> DONE = java.util.EnumSet.of(RequestStatus.COMPLETED,
			RequestStatus.CANCELLED, RequestStatus.CANCELLED_UNPAID, RequestStatus.REFUNDED);
	}
}
