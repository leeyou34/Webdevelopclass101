package io.github.leeyou34.todo.printer;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.leeyou34.todo.common.ApiException;
import io.github.leeyou34.todo.printer.Enums.CaseStatus;
import io.github.leeyou34.todo.printer.Enums.CaseType;
import io.github.leeyou34.todo.printer.Enums.CycleStatus;
import io.github.leeyou34.todo.printer.Enums.DeviceModel;
import io.github.leeyou34.todo.printer.Enums.DeviceStatus;
import io.github.leeyou34.todo.printer.Enums.InvoiceStatus;
import io.github.leeyou34.todo.printer.Enums.InvoiceType;
import io.github.leeyou34.todo.printer.Enums.RequestStatus;
import io.github.leeyou34.todo.printer.Enums.ShopType;
import io.github.leeyou34.todo.printer.PrinterDtos.ApprovalInput;
import io.github.leeyou34.todo.printer.PrinterDtos.AssignInput;
import io.github.leeyou34.todo.printer.PrinterDtos.CycleInput;
import io.github.leeyou34.todo.printer.PrinterDtos.Dashboard;
import io.github.leeyou34.todo.printer.PrinterDtos.DateInput;
import io.github.leeyou34.todo.printer.PrinterDtos.ExchangeInput;
import io.github.leeyou34.todo.printer.PrinterDtos.InvoiceRequestInput;
import io.github.leeyou34.todo.printer.PrinterDtos.ModelChangeInput;
import io.github.leeyou34.todo.printer.PrinterDtos.ModelCount;
import io.github.leeyou34.todo.printer.PrinterDtos.MonthlyReport;
import io.github.leeyou34.todo.printer.PrinterDtos.PaymentInput;
import io.github.leeyou34.todo.printer.PrinterDtos.PurchaseOrderInput;
import io.github.leeyou34.todo.printer.PrinterDtos.ReasonInput;
import io.github.leeyou34.todo.printer.PrinterDtos.ReceiveInput;
import io.github.leeyou34.todo.printer.PrinterDtos.RefundInput;
import io.github.leeyou34.todo.printer.PrinterDtos.RepairResultInput;
import io.github.leeyou34.todo.printer.PrinterDtos.RequestInput;
import io.github.leeyou34.todo.printer.PrinterDtos.ReturnToSenderInput;
import io.github.leeyou34.todo.printer.PrinterDtos.ShipInput;
import io.github.leeyou34.todo.printer.PrinterDtos.ShopInput;
import io.github.leeyou34.todo.printer.PrinterDtos.ShopMonthRow;
import io.github.leeyou34.todo.printer.PrinterDtos.Suggestion;

/**
 * 모바일 프린터 운영관리. 메서드 하나가 기획서 "동작 목록"의 동작 하나입니다(주석의 번호).
 * 모든 메서드는 로그인 사용자(owner)의 데이터 안에서만 동작합니다.
 */
@Service
@Transactional
public class PrinterService {

	/** 본사 앞 계산서 발행 후 이 일수가 지나도 입금이 없으면 "기한 초과"(기본값, 기획서에서 결정 대기) */
	static final int OVERDUE_DAYS = 30;

	private static final Pattern MONTH = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");
	private static final Pattern SERIAL = Pattern.compile("^[A-Z0-9]{7}(\\d{2})(\\d{2})\\d{3,}$");
	private static final Set<RequestStatus> CANCELLED = EnumSet.of(RequestStatus.CANCELLED_UNPAID,
		RequestStatus.CANCELLED, RequestStatus.REFUND_PENDING, RequestStatus.REFUNDED);

	private final ShopRepository shops;
	private final CycleRepository cycles;
	private final RequestRepository requests;
	private final PurchaseOrderRepository orders;
	private final DeviceRepository devices;
	private final CaseRepository cases;
	private final InvoiceRepository invoices;
	private final AdjustmentRepository adjustments;
	private final ActivityRepository activities;

	public PrinterService(ShopRepository shops, CycleRepository cycles, RequestRepository requests,
		PurchaseOrderRepository orders, DeviceRepository devices, CaseRepository cases, InvoiceRepository invoices,
		AdjustmentRepository adjustments, ActivityRepository activities) {
		this.shops = shops;
		this.cycles = cycles;
		this.requests = requests;
		this.orders = orders;
		this.devices = devices;
		this.cases = cases;
		this.invoices = invoices;
		this.adjustments = adjustments;
		this.activities = activities;
	}

	// ===================================================================== 조회

	@Transactional(readOnly = true)
	public List<Shop> listShops(UUID owner) {
		return shops.findByOwnerIdOrderByCodeAsc(owner);
	}

	@Transactional(readOnly = true)
	public List<OrderCycle> listCycles(UUID owner) {
		return cycles.findByOwnerIdOrderByMonthDesc(owner);
	}

	@Transactional(readOnly = true)
	public List<OrderRequest> listRequests(UUID owner, UUID cycleId) {
		return cycleId == null ? requests.findByOwnerIdOrderByCreatedAtAsc(owner)
			: requests.findByOwnerIdAndCycleIdOrderByCreatedAtAsc(owner, cycleId);
	}

	@Transactional(readOnly = true)
	public List<PurchaseOrder> listPurchaseOrders(UUID owner) {
		return orders.findByOwnerIdOrderByOrderedOnDesc(owner);
	}

	@Transactional(readOnly = true)
	public List<Device> listDevices(UUID owner, DeviceStatus status) {
		return status == null ? devices.findByOwnerIdOrderBySerialAsc(owner)
			: devices.findByOwnerIdAndStatusOrderBySerialAsc(owner, status);
	}

	@Transactional(readOnly = true)
	public List<ServiceCase> listCases(UUID owner) {
		return cases.findByOwnerIdOrderByOpenedOnDesc(owner);
	}

	@Transactional(readOnly = true)
	public List<Invoice> listInvoices(UUID owner) {
		return invoices.findByOwnerIdOrderByCreatedAtAsc(owner);
	}

	@Transactional(readOnly = true)
	public List<Adjustment> listAdjustments(UUID owner) {
		return adjustments.findByOwnerIdOrderByCreatedOnAsc(owner);
	}

	@Transactional(readOnly = true)
	public List<ActivityLog> listActivity(UUID owner) {
		return activities.findTop200ByOwnerIdOrderByCreatedAtDesc(owner);
	}

	// ===================================================================== 영업장

	public Shop createShop(UUID owner, ShopInput in) {
		String code = text(in.code(), "영업장 코드를 입력해 주세요.", 20);
		String name = text(in.name(), "영업장 이름을 입력해 주세요.", 50);
		if (shops.existsByOwnerIdAndCode(owner, code)) {
			throw ApiException.conflict("이미 있는 영업장 코드입니다.");
		}
		Shop s = new Shop();
		s.ownerId = owner;
		s.code = code;
		s.name = name;
		s.division = in.division();
		s.team = in.team();
		s.type = in.type() == null ? ShopType.SPECIALTY : in.type();
		s.managerName = in.managerName();
		s.phone = in.phone();
		s.address = in.address();
		s.openedOn = in.openedOn();
		shops.save(s);
		log(owner, 0, "영업장 등록", "shop", s.id, s.name);
		return s;
	}

	// ===================================================================== 1. 신청 기간 열기·마감

	public OrderCycle openCycle(UUID owner, CycleInput in) {
		String month = in.month() == null ? "" : in.month().trim();
		if (!MONTH.matcher(month).matches()) {
			throw ApiException.badRequest("대상 월은 2017-08 형식으로 입력해 주세요.");
		}
		if (cycles.findByOwnerIdAndMonth(owner, month).isPresent()) {
			throw ApiException.conflict("이미 열린 달입니다.");
		}
		OrderCycle c = new OrderCycle();
		c.ownerId = owner;
		c.month = month;
		c.closesOn = in.closesOn() != null ? in.closesOn() : YearMonth.parse(month).atDay(7);
		cycles.save(c);
		log(owner, 1, "신청 기간 열기", "cycle", c.id, month + " (마감 " + c.closesOn + ")");
		return c;
	}

	public OrderCycle closeCycle(UUID owner, UUID cycleId, DateInput in) {
		OrderCycle c = cycle(owner, cycleId);
		if (c.status != CycleStatus.OPEN) {
			throw ApiException.conflict("이미 마감된 신청 기간입니다.");
		}
		c.status = CycleStatus.CLOSED;
		c.closedOn = day(in == null ? null : in.date());
		int cancelled = 0;
		for (OrderRequest r : requests.findByOwnerIdAndCycleIdOrderByCreatedAtAsc(owner, c.id)) {
			if (r.status == RequestStatus.APPLIED) {
				r.status = RequestStatus.CANCELLED_UNPAID;
				r.note = "신청 기간 안에 입금하지 않아 자동 취소";
				cancelled++;
			}
		}
		log(owner, 1, "신청 기간 마감", "cycle", c.id, c.month + " · 미입금 자동 취소 " + cancelled + "건");
		return c;
	}

	// ===================================================================== 2. 신청 등록

	public OrderRequest createRequest(UUID owner, RequestInput in) {
		if (in.cycleId() == null || in.shopId() == null) {
			throw ApiException.badRequest("신청 기간과 영업장을 골라 주세요.");
		}
		OrderCycle c = cycle(owner, in.cycleId());
		if (c.status != CycleStatus.OPEN) {
			throw ApiException.conflict("마감된 신청 기간에는 신청을 받을 수 없습니다.");
		}
		Shop s = shop(owner, in.shopId());
		int android = qty(in.androidQty());
		int ios = qty(in.iosQty());
		if (android + ios == 0) {
			throw ApiException.badRequest("신청 수량을 1대 이상 입력해 주세요.");
		}
		OrderRequest r = new OrderRequest();
		r.ownerId = owner;
		r.cycleId = c.id;
		r.shopId = s.id;
		r.applicantName = text(in.applicantName(), "신청자 이름을 입력해 주세요.", 30);
		r.androidQty = android;
		r.iosQty = ios;
		r.personalAmount = Pricing.personalAmount(s.type, android, ios);
		r.hqAmount = Pricing.hqAmount(s.type, android, ios);
		requests.save(r);
		log(owner, 2, "신청 등록", "request", r.id,
			s.name + " · " + r.applicantName + " · 안드로이드 " + android + " / iOS " + ios);
		return r;
	}

	// ===================================================================== 3. 입금 확인

	public OrderRequest confirmPayment(UUID owner, UUID requestId, PaymentInput in) {
		OrderRequest r = request(owner, requestId);
		require(r, RequestStatus.APPLIED);
		long amount = in == null || in.amount() == null ? r.personalAmount : in.amount();
		if (amount != r.personalAmount) {
			throw ApiException.badRequest("입금액(" + won(amount) + ")이 신청 금액(" + won(r.personalAmount) + ")과 다릅니다.");
		}
		r.paidAmount = amount;
		r.paidOn = day(in == null ? null : in.date());
		r.status = RequestStatus.PAID;
		log(owner, 3, "입금 확인", "request", r.id, r.applicantName + " · " + won(amount));
		return r;
	}

	// ===================================================================== 4. 배송 리스트 확정

	public List<OrderRequest> confirmCycle(UUID owner, UUID cycleId, DateInput in) {
		OrderCycle c = cycle(owner, cycleId);
		if (c.status != CycleStatus.CLOSED) {
			throw ApiException.conflict("신청 기간을 먼저 마감해 주세요.");
		}
		List<OrderRequest> confirmed = new ArrayList<>();
		for (OrderRequest r : requests.findByOwnerIdAndCycleIdOrderByCreatedAtAsc(owner, c.id)) {
			if (r.status == RequestStatus.PAID) {
				r.status = RequestStatus.CONFIRMED;
				confirmed.add(r);
			}
		}
		if (confirmed.isEmpty()) {
			throw ApiException.conflict("확정할 신청(입금 확인된 건)이 없습니다.");
		}
		c.confirmedOn = day(in == null ? null : in.date());
		ModelCount mc = count(confirmed);
		log(owner, 4, "배송 리스트 확정", "cycle", c.id,
			c.month + " · " + confirmed.size() + "건 · 안드로이드 " + mc.android() + " / iOS " + mc.ios());
		return confirmed;
	}

	// ===================================================================== 5. 발주서 작성

	public PurchaseOrder createPurchaseOrder(UUID owner, UUID cycleId, PurchaseOrderInput in) {
		OrderCycle c = cycle(owner, cycleId);
		List<OrderRequest> targets = requests.findByOwnerIdAndCycleIdOrderByCreatedAtAsc(owner, c.id).stream()
			.filter(r -> r.status == RequestStatus.CONFIRMED).toList();
		if (targets.isEmpty()) {
			throw ApiException.conflict("발주할 확정 신청이 없습니다. 배송 리스트를 먼저 확정해 주세요.");
		}
		LocalDate orderedOn = day(in.orderedOn());
		PurchaseOrder po = new PurchaseOrder();
		po.ownerId = owner;
		po.cycleId = c.id;
		po.orderedOn = orderedOn;
		po.orderNo = blank(in.orderNo()) ? "ON-" + orderedOn.toString().replace("-", "") : in.orderNo().trim();
		po.bufferAndroid = qty(in.bufferAndroid());
		po.bufferIos = qty(in.bufferIos());
		ModelCount mc = count(targets);
		po.androidQty = mc.android() + po.bufferAndroid;
		po.iosQty = mc.ios() + po.bufferIos;
		po.boxNote = in.boxNote();
		orders.save(po);
		for (OrderRequest r : targets) {
			r.status = RequestStatus.ORDERED;
			r.purchaseOrderId = po.id;
		}
		log(owner, 5, "발주서 작성", "purchaseOrder", po.id, po.orderNo + " · 안드로이드 " + po.androidQty
			+ " / iOS " + po.iosQty + " (여유분 " + (po.bufferAndroid + po.bufferIos) + "대 포함)");
		return po;
	}

	// ===================================================================== 6. 품의 기록

	public PurchaseOrder recordApproval(UUID owner, UUID poId, ApprovalInput in) {
		PurchaseOrder po = order(owner, poId);
		po.approvalNo = text(in.approvalNo(), "품의 번호를 입력해 주세요.", 30);
		po.approvedOn = day(in.date());
		long revenue = po.androidQty * Pricing.SALE_ANDROID + po.iosQty * Pricing.SALE_IOS;
		long cost = po.androidQty * Pricing.COST_ANDROID + po.iosQty * Pricing.COST_IOS;
		log(owner, 6, "품의 기록", "purchaseOrder", po.id, po.approvalNo + " · 매출 " + won(revenue) + " · 매입 "
			+ won(cost) + " · 수익 " + won(revenue - cost));
		return po;
	}

	// ===================================================================== 7. 입고 등록

	public List<Device> receive(UUID owner, UUID poId, ReceiveInput in) {
		PurchaseOrder po = order(owner, poId);
		LocalDate date = day(in.date());
		List<String> serials = new ArrayList<>();
		if (in.serials() != null) {
			for (String s : in.serials()) {
				if (!blank(s)) {
					serials.add(s.trim().toUpperCase());
				}
			}
		}
		serials.addAll(generateSerials(owner, DeviceModel.ANDROID, qty(in.autoAndroid()), date));
		serials.addAll(generateSerials(owner, DeviceModel.IOS, qty(in.autoIos()), date));
		if (serials.isEmpty()) {
			throw ApiException.badRequest("입고할 시리얼을 입력하거나 자동 생성 대수를 넣어 주세요.");
		}
		int android = 0;
		int ios = 0;
		List<Device> created = new ArrayList<>();
		for (String serial : serials) {
			DeviceModel model = DeviceModel.fromSerial(serial);
			if (model == null || !SERIAL.matcher(serial).matches()) {
				throw ApiException.badRequest("시리얼 형식을 알 수 없습니다: " + serial);
			}
			if (devices.existsByOwnerIdAndSerial(owner, serial)) {
				throw ApiException.conflict("이미 등록된 시리얼입니다: " + serial);
			}
			if (model == DeviceModel.ANDROID) {
				android++;
			} else {
				ios++;
			}
			Device d = new Device();
			d.ownerId = owner;
			d.serial = serial;
			d.model = model;
			d.producedOn = producedOn(serial);
			d.purchaseOrderId = po.id;
			d.receivedOn = date;
			created.add(d);
		}
		if (po.receivedAndroid + android > po.androidQty || po.receivedIos + ios > po.iosQty) {
			throw ApiException.badRequest("발주 수량보다 많이 입고할 수 없습니다. (발주 안드로이드 " + po.androidQty
				+ " / iOS " + po.iosQty + ", 기입고 " + po.receivedAndroid + " / " + po.receivedIos + ")");
		}
		devices.saveAll(created);
		po.receivedAndroid += android;
		po.receivedIos += ios;
		po.receivedOn = date;
		log(owner, 7, "입고 등록", "purchaseOrder", po.id, po.orderNo + " · 안드로이드 " + android + " / iOS " + ios);
		return created;
	}

	// ===================================================================== 8. 시리얼 배정

	public List<Device> assign(UUID owner, UUID requestId, AssignInput in) {
		OrderRequest r = request(owner, requestId);
		require(r, RequestStatus.ORDERED);
		if (!devices.findByOwnerIdAndRequestId(owner, r.id).isEmpty()) {
			throw ApiException.conflict("이미 시리얼이 배정된 신청입니다.");
		}
		List<Device> picked = new ArrayList<>();
		if (in != null && in.serials() != null && !in.serials().isEmpty()) {
			for (String s : in.serials()) {
				Device d = deviceBySerial(owner, s);
				if (d.status != DeviceStatus.IN_STOCK) {
					throw ApiException.conflict("재고 상태가 아닌 기기입니다: " + d.serial);
				}
				picked.add(d);
			}
		} else {
			picked.addAll(pickStock(owner, DeviceModel.ANDROID, r.androidQty, r.purchaseOrderId));
			picked.addAll(pickStock(owner, DeviceModel.IOS, r.iosQty, r.purchaseOrderId));
		}
		ModelCount mc = countDevices(picked);
		if (mc.android() != r.androidQty || mc.ios() != r.iosQty) {
			throw ApiException.badRequest("배정한 기기 수가 신청 수량과 다릅니다. (신청 안드로이드 " + r.androidQty
				+ " / iOS " + r.iosQty + ", 배정 " + mc.android() + " / " + mc.ios() + ")");
		}
		for (Device d : picked) {
			d.status = DeviceStatus.ASSIGNED;
			d.requestId = r.id;
			d.shopId = r.shopId;
		}
		log(owner, 8, "시리얼 배정", "request", r.id, r.applicantName + " · "
			+ String.join(", ", picked.stream().map(d -> d.serial).toList()));
		return picked;
	}

	// ===================================================================== 9. 발송 등록

	public OrderRequest ship(UUID owner, UUID requestId, ShipInput in) {
		OrderRequest r = request(owner, requestId);
		require(r, RequestStatus.ORDERED, RequestStatus.RETURNED_TO_SENDER);
		List<Device> assigned = devices.findByOwnerIdAndRequestId(owner, r.id);
		if (assigned.size() != r.totalQty()) {
			throw ApiException.conflict("시리얼을 먼저 배정해 주세요.");
		}
		r.trackingNo = text(in.trackingNo(), "송장번호를 입력해 주세요.", 30);
		r.shippedOn = day(in.date());
		r.status = RequestStatus.SHIPPING;
		for (Device d : assigned) {
			d.status = DeviceStatus.SHIPPED;
		}
		log(owner, 9, "발송 등록", "request", r.id, r.applicantName + " · 송장 " + r.trackingNo);
		return r;
	}

	// ===================================================================== 10. 배송 완료

	public OrderRequest deliver(UUID owner, UUID requestId, DateInput in) {
		OrderRequest r = request(owner, requestId);
		require(r, RequestStatus.SHIPPING);
		r.deliveredOn = day(in == null ? null : in.date());
		r.status = RequestStatus.DELIVERED;
		for (Device d : devices.findByOwnerIdAndRequestId(owner, r.id)) {
			d.status = DeviceStatus.DELIVERED;
			d.deliveredOn = r.deliveredOn;
		}
		log(owner, 10, "배송 완료", "request", r.id, r.applicantName + " · " + r.deliveredOn);
		return r;
	}

	// ===================================================================== 11. 계산서 발행 요청

	public List<Invoice> requestInvoices(UUID owner, UUID cycleId, InvoiceRequestInput in) {
		OrderCycle c = cycle(owner, cycleId);
		if (in.type() == null) {
			throw ApiException.badRequest("계산서 종류(개인 앞/본사 앞)를 골라 주세요.");
		}
		LocalDate planned = day(in.plannedOn());
		List<OrderRequest> inCycle = requests.findByOwnerIdAndCycleIdOrderByCreatedAtAsc(owner, c.id);
		List<Invoice> created = new ArrayList<>();
		if (in.type() == InvoiceType.PERSONAL) {
			for (OrderRequest r : inCycle) {
				boolean delivered = r.status == RequestStatus.DELIVERED || r.status == RequestStatus.INVOICED;
				if (delivered && r.personalAmount > 0
					&& invoices.findByOwnerIdAndRequestId(owner, r.id).isEmpty()) {
					created.add(newInvoice(owner, InvoiceType.PERSONAL, c.id, r.id, r.personalAmount, planned));
				}
			}
			if (created.isEmpty()) {
				throw ApiException.conflict("개인 앞 계산서를 요청할 배송 완료 건이 없습니다.");
			}
		} else {
			boolean exists = invoices.findByOwnerIdAndCycleId(owner, c.id).stream()
				.anyMatch(i -> i.type == InvoiceType.HQ);
			if (exists) {
				throw ApiException.conflict("이 달의 본사 앞 계산서는 이미 요청했습니다.");
			}
			long amount = inCycle.stream()
				.filter(r -> EnumSet.of(RequestStatus.DELIVERED, RequestStatus.INVOICED).contains(r.status))
				.mapToLong(r -> r.hqAmount).sum();
			if (amount == 0) {
				throw ApiException.conflict("본사 앞으로 청구할 금액이 없습니다.");
			}
			created.add(newInvoice(owner, InvoiceType.HQ, c.id, null, amount, planned));
		}
		long total = created.stream().mapToLong(i -> i.amount).sum();
		log(owner, 11, "계산서 발행 요청", "cycle", c.id, c.month + " · " + label(in.type()) + " " + created.size()
			+ "장 · " + won(total));
		return created;
	}

	// ===================================================================== 12. 계산서 발행 완료

	public Invoice issueInvoice(UUID owner, UUID invoiceId, DateInput in) {
		Invoice inv = invoice(owner, invoiceId);
		if (inv.status != InvoiceStatus.REQUESTED) {
			throw ApiException.conflict("이미 발행된 계산서입니다.");
		}
		inv.status = InvoiceStatus.ISSUED;
		inv.issuedOn = day(in == null ? null : in.date());
		if (inv.type == InvoiceType.PERSONAL) {
			request(owner, inv.requestId).invoicedOn = firstDate(request(owner, inv.requestId).invoicedOn,
				inv.issuedOn);
			recompute(owner, request(owner, inv.requestId));
		} else {
			for (OrderRequest r : requests.findByOwnerIdAndCycleIdOrderByCreatedAtAsc(owner, inv.cycleId)) {
				if (r.hqAmount > 0 && EnumSet.of(RequestStatus.DELIVERED, RequestStatus.INVOICED).contains(r.status)) {
					r.invoicedOn = firstDate(r.invoicedOn, inv.issuedOn);
					recompute(owner, r);
				}
			}
		}
		log(owner, 12, "계산서 발행 완료", "invoice", inv.id, label(inv.type) + " · " + won(inv.amount) + " · "
			+ inv.issuedOn);
		return inv;
	}

	// ===================================================================== 13. 수금 확인

	public Invoice confirmInvoicePayment(UUID owner, UUID invoiceId, DateInput in) {
		Invoice inv = invoice(owner, invoiceId);
		if (inv.type != InvoiceType.HQ) {
			throw ApiException.badRequest("개인 앞 계산서는 신청 때 입금받아 수금 확인이 필요 없습니다.");
		}
		if (inv.status != InvoiceStatus.ISSUED) {
			throw ApiException.conflict("발행된 본사 앞 계산서만 수금 확인할 수 있습니다.");
		}
		inv.status = InvoiceStatus.PAID;
		inv.paidOn = day(in == null ? null : in.date());
		for (OrderRequest r : requests.findByOwnerIdAndCycleIdOrderByCreatedAtAsc(owner, inv.cycleId)) {
			recompute(owner, r);
		}
		log(owner, 13, "수금 확인", "invoice", inv.id, "본사 앞 · " + won(inv.amount) + " · " + inv.paidOn);
		return inv;
	}

	// ===================================================================== 14. 취소

	public OrderRequest cancel(UUID owner, UUID requestId, ReasonInput in) {
		OrderRequest r = request(owner, requestId);
		require(r, RequestStatus.APPLIED, RequestStatus.PAID, RequestStatus.CONFIRMED, RequestStatus.ORDERED);
		releaseAssigned(owner, r);
		String reason = blank(in.reason()) ? "사유 미기재" : in.reason().trim();
		if (r.status == RequestStatus.APPLIED) {
			r.status = RequestStatus.CANCELLED;
		} else {
			r.status = RequestStatus.REFUND_PENDING;
			r.refundDue += r.paidAmount;
		}
		r.note = "취소: " + reason;
		log(owner, 14, "취소", "request", r.id, r.applicantName + " · " + reason
			+ (r.refundDue > 0 ? " · 환불 대기 " + won(r.refundDue - r.refundedTotal) : ""));
		return r;
	}

	// ===================================================================== 15. 기종 변경

	public OrderRequest changeModel(UUID owner, UUID requestId, ModelChangeInput in) {
		OrderRequest r = request(owner, requestId);
		require(r, RequestStatus.APPLIED, RequestStatus.PAID, RequestStatus.CONFIRMED, RequestStatus.ORDERED);
		if (!devices.findByOwnerIdAndRequestId(owner, r.id).isEmpty()) {
			throw ApiException.conflict("시리얼이 이미 배정되어 기종을 바꿀 수 없습니다. 취소 후 다시 신청해 주세요.");
		}
		int android = qty(in.androidQty());
		int ios = qty(in.iosQty());
		if (android + ios == 0) {
			throw ApiException.badRequest("수량을 1대 이상 입력해 주세요.");
		}
		Shop s = shop(owner, r.shopId);
		String before = "안드로이드 " + r.androidQty + " / iOS " + r.iosQty;
		r.androidQty = android;
		r.iosQty = ios;
		r.personalAmount = Pricing.personalAmount(s.type, android, ios);
		r.hqAmount = Pricing.hqAmount(s.type, android, ios);
		String money = "";
		if (r.paidAmount > 0) {
			long diff = r.personalAmount - r.paidAmount;
			if (diff < 0) {
				r.refundDue += -diff;
				r.paidAmount = r.personalAmount;
				money = " · 차액 환불 대기 " + won(-diff);
			} else if (diff > 0) {
				r.note = "기종 변경으로 추가 입금 " + won(diff) + " 필요";
				money = " · 추가 입금 필요 " + won(diff);
			}
		}
		log(owner, 15, "기종 변경", "request", r.id, before + " → 안드로이드 " + android + " / iOS " + ios + money);
		return r;
	}

	// ===================================================================== 16. 반송 처리

	public OrderRequest returnToSender(UUID owner, UUID requestId, ReturnToSenderInput in) {
		OrderRequest r = request(owner, requestId);
		require(r, RequestStatus.SHIPPING);
		r.status = RequestStatus.RETURNED_TO_SENDER;
		for (Device d : devices.findByOwnerIdAndRequestId(owner, r.id)) {
			d.status = DeviceStatus.ASSIGNED;
		}
		String reason = blank(in.reason()) ? "주소 오기" : in.reason().trim();
		if (!blank(in.correctedAddress())) {
			shop(owner, r.shopId).address = in.correctedAddress().trim();
		}
		r.note = "반송: " + reason;
		log(owner, 16, "반송 처리", "request", r.id, r.applicantName + " · " + reason + " · 재발송 대기");
		return r;
	}

	// ===================================================================== 17. 반품 접수 → 회수 확인

	public ServiceCase openReturn(UUID owner, String serial, ReasonInput in) {
		Device d = deliveredDevice(owner, serial);
		ServiceCase k = newCase(owner, CaseType.RETURN, d, in.reason(), day(in.date()));
		d.status = DeviceStatus.AWAITING_RECOVERY;
		log(owner, 17, "반품 접수", "case", k.id, d.serial + " · " + k.reason);
		return k;
	}

	public ServiceCase recover(UUID owner, UUID caseId, DateInput in) {
		ServiceCase k = kase(owner, caseId);
		if (k.status != CaseStatus.OPEN || k.type == CaseType.REPAIR) {
			throw ApiException.conflict("회수를 기다리는 반품·교환 건이 아닙니다.");
		}
		LocalDate date = day(in == null ? null : in.date());
		Device old = devices.findByIdAndOwnerId(k.deviceId, owner).orElseThrow();
		k.status = CaseStatus.CLOSED;
		k.closedOn = date;
		if (k.type == CaseType.EXCHANGE) {
			old.status = DeviceStatus.RECOVERED;
			log(owner, 19, "교환 기기 회수", "case", k.id, old.serial + " 회수 완료");
			return k;
		}
		// 반품: 기기는 재고로 돌아오고, 신청 건 수량·금액이 줄며, 돌려줄 돈이 생깁니다.
		OrderRequest r = request(owner, k.requestId);
		Shop s = shop(owner, r.shopId);
		if (old.model == DeviceModel.ANDROID) {
			r.androidQty--;
		} else {
			r.iosQty--;
		}
		long hqBefore = r.hqAmount;
		r.personalAmount = Pricing.personalAmount(s.type, r.androidQty, r.iosQty);
		r.hqAmount = Pricing.hqAmount(s.type, r.androidQty, r.iosQty);
		r.refundDue += Pricing.personalRefundPerUnit(s.type);
		long hqDiff = hqBefore - r.hqAmount;
		if (hqDiff > 0 && hqInvoiceIssued(owner, r.cycleId)) {
			newAdjustment(owner, r.id, -hqDiff, "반품 " + old.serial + " 본사 앞 차감", date);
		}
		if (r.totalQty() == 0) {
			r.status = r.refundDue > r.refundedTotal ? RequestStatus.REFUND_PENDING : RequestStatus.REFUNDED;
		}
		old.status = DeviceStatus.IN_STOCK;
		old.requestId = null;
		old.shopId = null;
		old.deliveredOn = null;
		log(owner, 17, "반품 회수", "case", k.id, old.serial + " 재고 복귀 · 환불 대기 " + won(r.refundDue - r.refundedTotal));
		return k;
	}

	// ===================================================================== 18. 환불 완료

	public OrderRequest refund(UUID owner, UUID requestId, RefundInput in) {
		OrderRequest r = request(owner, requestId);
		long pending = r.refundDue - r.refundedTotal;
		if (pending <= 0) {
			throw ApiException.conflict("돌려줄 금액이 없는 신청입니다.");
		}
		long amount = in == null || in.amount() == null ? pending : in.amount();
		if (amount <= 0 || amount > pending) {
			throw ApiException.badRequest("환불액은 1원 이상, 남은 환불액(" + won(pending) + ") 이하로 입력해 주세요.");
		}
		LocalDate date = day(in == null ? null : in.date());
		r.refundedTotal += amount;
		if (r.status == RequestStatus.REFUND_PENDING && r.refundedTotal >= r.refundDue) {
			r.status = RequestStatus.REFUNDED;
		}
		String adj = "";
		if (r.invoicedOn != null) {
			Adjustment a = newAdjustment(owner, r.id, -amount, "환불 " + r.applicantName, date);
			adj = " · " + a.month + " 계산서에 조정 반영";
		}
		log(owner, 18, "환불 완료", "request", r.id, r.applicantName + " · " + won(amount) + adj);
		return r;
	}

	// ===================================================================== 19. 교환 출고 → 회수 확인

	public ServiceCase exchange(UUID owner, String serial, ExchangeInput in) {
		Device old = deliveredDevice(owner, serial);
		Device fresh;
		if (!blank(in.newSerial())) {
			fresh = deviceBySerial(owner, in.newSerial());
			if (fresh.status != DeviceStatus.IN_STOCK || fresh.model != old.model) {
				throw ApiException.conflict("같은 기종의 재고 기기만 교체로 보낼 수 있습니다.");
			}
		} else {
			fresh = pickStock(owner, old.model, 1, null).stream().findFirst()
				.orElseThrow(() -> ApiException.conflict("교체로 보낼 같은 기종 재고가 없습니다."));
		}
		LocalDate date = day(in.date());
		ServiceCase k = newCase(owner, CaseType.EXCHANGE, old, blank(in.reason()) ? "기기 불량" : in.reason(), date);
		k.newDeviceId = fresh.id;
		k.newSerial = fresh.serial;
		fresh.status = DeviceStatus.DELIVERED;
		fresh.requestId = old.requestId;
		fresh.shopId = old.shopId;
		fresh.deliveredOn = date;
		old.status = DeviceStatus.AWAITING_RECOVERY;
		log(owner, 19, "교환 출고", "case", k.id, old.serial + " → " + fresh.serial + " (선배송, 회수 대기)");
		return k;
	}

	// ===================================================================== 20. AS 접수 → 결과 등록

	public ServiceCase openRepair(UUID owner, String serial, ReasonInput in) {
		Device d = deliveredDevice(owner, serial);
		LocalDate date = day(in.date());
		ServiceCase k = newCase(owner, CaseType.REPAIR, d, blank(in.reason()) ? "증상 미기재" : in.reason(), date);
		k.freeWarranty = d.producedOn != null && date.isBefore(d.producedOn.plusYears(2));
		d.status = DeviceStatus.IN_REPAIR;
		log(owner, 20, "AS 접수", "case", k.id, d.serial + " · " + k.reason + " · "
			+ (Boolean.TRUE.equals(k.freeWarranty) ? "무상" : "유상"));
		return k;
	}

	public ServiceCase repairResult(UUID owner, UUID caseId, RepairResultInput in) {
		ServiceCase k = kase(owner, caseId);
		if (k.type != CaseType.REPAIR || k.status != CaseStatus.OPEN) {
			throw ApiException.conflict("진행 중인 AS 건이 아닙니다.");
		}
		Device d = devices.findByIdAndOwnerId(k.deviceId, owner).orElseThrow();
		boolean repaired = in.repaired() == null || in.repaired();
		k.closedOn = day(in.date());
		k.status = repaired ? CaseStatus.CLOSED : CaseStatus.UNREPAIRABLE;
		d.status = repaired ? DeviceStatus.DELIVERED : DeviceStatus.SCRAPPED;
		log(owner, 20, "AS 결과 등록", "case", k.id, d.serial + " · " + (repaired ? "수리 완료" : "수리 불가"));
		return k;
	}

	// ===================================================================== 21. 월 마감

	@Transactional(readOnly = true)
	public MonthlyReport monthlyReport(UUID owner, String month) {
		if (month == null || !MONTH.matcher(month).matches()) {
			throw ApiException.badRequest("대상 월은 2017-08 형식으로 입력해 주세요.");
		}
		YearMonth ym = YearMonth.parse(month);
		List<OrderRequest> all = requests.findByOwnerIdOrderByCreatedAtAsc(owner);
		ModelCount sales = count(all.stream().filter(r -> in(ym, r.deliveredOn) && !CANCELLED.contains(r.status)).toList());
		ModelCount accounting = count(all.stream().filter(r -> in(ym, r.invoicedOn)).toList());
		Optional<OrderCycle> cycle = cycles.findByOwnerIdAndMonth(owner, month);
		List<OrderRequest> inCycle = cycle.map(c -> all.stream().filter(r -> r.cycleId.equals(c.id)).toList())
			.orElse(List.of());
		List<OrderRequest> live = inCycle.stream().filter(r -> !CANCELLED.contains(r.status)).toList();
		Map<UUID, int[]> byShop = new LinkedHashMap<>();
		for (OrderRequest r : live) {
			int[] v = byShop.computeIfAbsent(r.shopId, k -> new int[2]);
			v[0] += r.androidQty;
			v[1] += r.iosQty;
		}
		List<ShopMonthRow> rows = new ArrayList<>();
		for (Map.Entry<UUID, int[]> e : byShop.entrySet()) {
			Shop s = shop(owner, e.getKey());
			rows.add(new ShopMonthRow(s.code, s.name, e.getValue()[0], e.getValue()[1]));
		}
		rows.sort(Comparator.comparing(ShopMonthRow::shopCode));
		long revenue = live.stream().mapToLong(r -> r.personalAmount + r.hqAmount).sum();
		long cost = live.stream().mapToLong(r -> r.androidQty * Pricing.COST_ANDROID + r.iosQty * Pricing.COST_IOS)
			.sum();
		int cancelled = (int) inCycle.stream().filter(r -> CANCELLED.contains(r.status)).count();
		List<ServiceCase> monthCases = cases.findByOwnerIdOrderByOpenedOnDesc(owner).stream()
			.filter(k -> in(ym, k.openedOn)).toList();
		long adj = adjustments.findByOwnerIdOrderByCreatedOnAsc(owner).stream()
			.filter(a -> a.month.equals(month)).mapToLong(a -> a.amount).sum();
		return new MonthlyReport(month, sales, accounting, rows, revenue, cost, revenue - cost, cancelled,
			(int) monthCases.stream().filter(k -> k.type == CaseType.RETURN).count(),
			(int) monthCases.stream().filter(k -> k.type == CaseType.EXCHANGE).count(),
			(int) monthCases.stream().filter(k -> k.type == CaseType.REPAIR).count(), adj);
	}

	public MonthlyReport closeMonth(UUID owner, String month) {
		MonthlyReport report = monthlyReport(owner, month);
		log(owner, 21, "월 마감", "month", null, month + " · 영업 기준 " + report.salesBasis().total() + "대 · 회계 기준 "
			+ report.accountingBasis().total() + "대 · 수익 " + won(report.profit()));
		return report;
	}

	// ===================================================================== 대시보드

	@Transactional(readOnly = true)
	public Dashboard dashboard(UUID owner, LocalDate asOfInput) {
		LocalDate asOf = day(asOfInput);
		YearMonth ym = YearMonth.from(asOf);
		List<OrderRequest> all = requests.findByOwnerIdOrderByCreatedAtAsc(owner);
		List<OrderCycle> cycleList = cycles.findByOwnerIdOrderByMonthDesc(owner);
		OrderCycle current = cycleList.stream().filter(c -> c.month.equals(ym.toString())).findFirst()
			.orElse(cycleList.isEmpty() ? null : cycleList.get(0));
		Map<String, Long> byStatus = new LinkedHashMap<>();
		if (current != null) {
			for (OrderRequest r : all) {
				if (r.cycleId.equals(current.id)) {
					byStatus.merge(r.status.name(), 1L, Long::sum);
				}
			}
		}
		List<Device> deviceList = devices.findByOwnerIdOrderBySerialAsc(owner);
		ModelCount stock = countDevices(deviceList.stream().filter(d -> d.status == DeviceStatus.IN_STOCK).toList());
		List<Invoice> invoiceList = invoices.findByOwnerIdOrderByCreatedAtAsc(owner);
		List<Invoice> receivable = invoiceList.stream()
			.filter(i -> i.type == InvoiceType.HQ && i.status == InvoiceStatus.ISSUED).toList();
		List<Invoice> overdue = receivable.stream()
			.filter(i -> i.issuedOn != null && i.issuedOn.plusDays(OVERDUE_DAYS).isBefore(asOf)).toList();
		List<ServiceCase> caseList = cases.findByOwnerIdOrderByOpenedOnDesc(owner);
		int unrecovered = (int) caseList.stream()
			.filter(k -> k.type == CaseType.EXCHANGE && k.status == CaseStatus.OPEN).count();
		int repairs = (int) caseList.stream().filter(k -> k.type == CaseType.REPAIR && k.status == CaseStatus.OPEN)
			.count();
		long pendingRefunds = all.stream().mapToLong(r -> Math.max(0, r.refundDue - r.refundedTotal)).sum();

		List<Suggestion> s = new ArrayList<>();
		for (OrderCycle c : cycleList) {
			if (c.status == CycleStatus.OPEN && asOf.isAfter(c.closesOn)) {
				s.add(new Suggestion("action", c.month + " 신청 기간(마감 " + c.closesOn + ")이 지났습니다. 마감하면 미입금 신청이 자동 취소됩니다.",
					"cycle", c.id.toString()));
			}
			List<OrderRequest> rs = all.stream().filter(r -> r.cycleId.equals(c.id)).toList();
			long paid = rs.stream().filter(r -> r.status == RequestStatus.PAID).count();
			if (c.status == CycleStatus.CLOSED && paid > 0) {
				s.add(new Suggestion("action", c.month + " 입금 확인된 신청 " + paid + "건을 배송 리스트로 확정하세요.", "cycle",
					c.id.toString()));
			}
			long confirmed = rs.stream().filter(r -> r.status == RequestStatus.CONFIRMED).count();
			if (confirmed > 0) {
				s.add(new Suggestion("action", c.month + " 확정된 " + confirmed + "건으로 발주서를 작성하세요.", "cycle",
					c.id.toString()));
			}
			long toInvoice = rs.stream().filter(r -> r.status == RequestStatus.DELIVERED && r.personalAmount > 0
				&& invoiceList.stream().noneMatch(i -> r.id.equals(i.requestId))).count();
			if (toInvoice > 0) {
				s.add(new Suggestion("action", c.month + " 배송 완료 " + toInvoice + "건의 개인 앞 계산서 발행을 요청하세요.",
					"cycle", c.id.toString()));
			}
		}
		for (OrderRequest r : all) {
			if (r.status == RequestStatus.ORDERED && devices.findByOwnerIdAndRequestId(owner, r.id).isEmpty()) {
				s.add(new Suggestion("action", r.applicantName + " 신청에 시리얼을 배정하세요(입고 후).", "request",
					r.id.toString()));
			} else if (r.status == RequestStatus.RETURNED_TO_SENDER) {
				s.add(new Suggestion("warning", r.applicantName + " 신청이 반송됐습니다. 주소를 확인해 재발송하세요.", "request",
					r.id.toString()));
			}
			if (r.refundDue > r.refundedTotal) {
				s.add(new Suggestion("warning", r.applicantName + "에게 돌려줄 돈 " + won(r.refundDue - r.refundedTotal)
					+ "이 남아 있습니다.", "request", r.id.toString()));
			}
		}
		for (Invoice i : invoiceList) {
			if (i.status == InvoiceStatus.REQUESTED) {
				s.add(new Suggestion("action", label(i.type) + " 계산서 " + won(i.amount) + " 발행 처리가 남았습니다.", "invoice",
					i.id.toString()));
			}
		}
		for (Invoice i : overdue) {
			s.add(new Suggestion("danger", "본사 앞 계산서 " + won(i.amount) + "이 발행 후 " + OVERDUE_DAYS
				+ "일이 지나도록 입금되지 않았습니다. 독촉하세요.", "invoice", i.id.toString()));
		}
		if (unrecovered > 0) {
			s.add(new Suggestion("warning", "교환해 보냈지만 아직 회수하지 못한 불량 기기가 " + unrecovered + "대입니다.", "case", null));
		}
		return new Dashboard(asOf, ym.toString(), byStatus,
			count(all.stream().filter(r -> in(ym, r.deliveredOn) && !CANCELLED.contains(r.status)).toList()),
			count(all.stream().filter(r -> in(ym, r.invoicedOn)).toList()), stock,
			receivable.stream().mapToLong(i -> i.amount).sum(), receivable.size(),
			overdue.stream().mapToLong(i -> i.amount).sum(), overdue.size(), unrecovered, repairs, pendingRefunds, s);
	}

	// ===================================================================== 내부 도구

	/** 배송 이후 신청 건 상태를 계산서 발행·수금 여부에 맞춰 다시 정합니다. */
	private void recompute(UUID owner, OrderRequest r) {
		if (!EnumSet.of(RequestStatus.DELIVERED, RequestStatus.INVOICED, RequestStatus.COMPLETED).contains(r.status)) {
			return;
		}
		boolean personalIssued = invoices.findByOwnerIdAndRequestId(owner, r.id).stream()
			.anyMatch(i -> i.status != InvoiceStatus.REQUESTED);
		Optional<Invoice> hq = invoices.findByOwnerIdAndCycleId(owner, r.cycleId).stream()
			.filter(i -> i.type == InvoiceType.HQ).findFirst();
		boolean hqIssued = hq.isPresent() && hq.get().status != InvoiceStatus.REQUESTED;
		boolean hqPaid = hq.isPresent() && hq.get().status == InvoiceStatus.PAID;
		boolean personalDone = r.personalAmount == 0 || personalIssued;
		boolean hqDone = r.hqAmount == 0 || hqIssued;
		boolean anyIssued = (r.personalAmount > 0 && personalIssued) || (r.hqAmount > 0 && hqIssued);
		if (personalDone && hqDone && (r.hqAmount == 0 || hqPaid)) {
			r.status = RequestStatus.COMPLETED;
		} else if (anyIssued) {
			r.status = RequestStatus.INVOICED;
		} else {
			r.status = RequestStatus.DELIVERED;
		}
	}

	private boolean hqInvoiceIssued(UUID owner, UUID cycleId) {
		return invoices.findByOwnerIdAndCycleId(owner, cycleId).stream()
			.anyMatch(i -> i.type == InvoiceType.HQ && i.status != InvoiceStatus.REQUESTED);
	}

	private void releaseAssigned(UUID owner, OrderRequest r) {
		for (Device d : devices.findByOwnerIdAndRequestId(owner, r.id)) {
			if (d.status == DeviceStatus.ASSIGNED) {
				d.status = DeviceStatus.IN_STOCK;
				d.requestId = null;
				d.shopId = null;
			}
		}
	}

	private List<Device> pickStock(UUID owner, DeviceModel model, int n, UUID preferredPo) {
		if (n == 0) {
			return List.of();
		}
		List<Device> stock = new ArrayList<>(devices.findByOwnerIdAndStatusOrderBySerialAsc(owner, DeviceStatus.IN_STOCK)
			.stream().filter(d -> d.model == model).toList());
		stock.sort(Comparator.comparing((Device d) -> preferredPo != null && preferredPo.equals(d.purchaseOrderId) ? 0 : 1)
			.thenComparing(d -> d.serial));
		if (stock.size() < n) {
			throw ApiException.conflict(label(model) + " 재고가 부족합니다. (필요 " + n + "대, 재고 " + stock.size() + "대)");
		}
		return stock.subList(0, n);
	}

	private List<String> generateSerials(UUID owner, DeviceModel model, int n, LocalDate date) {
		List<String> out = new ArrayList<>();
		String base = model.serialPrefix + String.format("%02d%02d", date.getYear() % 100, date.getMonthValue());
		int seq = 1;
		while (out.size() < n) {
			String serial = base + String.format("%04d", seq++);
			if (!devices.existsByOwnerIdAndSerial(owner, serial)) {
				out.add(serial);
			}
		}
		return out;
	}

	private static LocalDate producedOn(String serial) {
		var m = SERIAL.matcher(serial);
		if (!m.matches()) {
			return null;
		}
		int yy = Integer.parseInt(m.group(1));
		int mm = Integer.parseInt(m.group(2));
		if (mm < 1 || mm > 12) {
			return null;
		}
		return LocalDate.of(2000 + yy, mm, 1);
	}

	private Invoice newInvoice(UUID owner, InvoiceType type, UUID cycleId, UUID requestId, long amount, LocalDate planned) {
		Invoice i = new Invoice();
		i.ownerId = owner;
		i.type = type;
		i.cycleId = cycleId;
		i.requestId = requestId;
		i.amount = amount;
		i.plannedOn = planned;
		return invoices.save(i);
	}

	private ServiceCase newCase(UUID owner, CaseType type, Device d, String reason, LocalDate date) {
		ServiceCase k = new ServiceCase();
		k.ownerId = owner;
		k.type = type;
		k.deviceId = d.id;
		k.serial = d.serial;
		k.requestId = d.requestId;
		k.shopId = d.shopId;
		k.reason = blank(reason) ? "사유 미기재" : reason.trim();
		k.openedOn = date;
		return cases.save(k);
	}

	/** 계산서 마감(매월 10일) 전 변동은 그달, 지난 변동은 다음 달 계산서에 반영합니다. */
	private Adjustment newAdjustment(UUID owner, UUID requestId, long amount, String reason, LocalDate date) {
		Adjustment a = new Adjustment();
		a.ownerId = owner;
		a.requestId = requestId;
		a.amount = amount;
		a.reason = reason;
		a.createdOn = date;
		a.month = (date.getDayOfMonth() <= 10 ? YearMonth.from(date) : YearMonth.from(date).plusMonths(1)).toString();
		return adjustments.save(a);
	}

	private void log(UUID owner, int action, String label, String targetType, UUID targetId, String detail) {
		ActivityLog a = new ActivityLog();
		a.ownerId = owner;
		a.action = action;
		a.label = label;
		a.targetType = targetType;
		a.targetId = targetId;
		a.detail = detail != null && detail.length() > 300 ? detail.substring(0, 300) : detail;
		activities.save(a);
	}

	private void require(OrderRequest r, RequestStatus... allowed) {
		for (RequestStatus s : allowed) {
			if (r.status == s) {
				return;
			}
		}
		throw ApiException.conflict("지금 상태(" + label(r.status) + ")에서는 할 수 없는 동작입니다.");
	}

	private Device deliveredDevice(UUID owner, String serial) {
		Device d = deviceBySerial(owner, serial);
		if (d.status != DeviceStatus.DELIVERED) {
			throw ApiException.conflict("배송 완료 상태의 기기만 처리할 수 있습니다: " + d.serial);
		}
		return d;
	}

	private Device deviceBySerial(UUID owner, String serial) {
		if (blank(serial)) {
			throw ApiException.badRequest("시리얼을 입력해 주세요.");
		}
		return devices.findByOwnerIdAndSerial(owner, serial.trim().toUpperCase())
			.orElseThrow(() -> ApiException.notFound("등록되지 않은 시리얼입니다: " + serial));
	}

	private OrderCycle cycle(UUID owner, UUID id) {
		return cycles.findByIdAndOwnerId(id, owner).orElseThrow(() -> ApiException.notFound("신청 기간을 찾을 수 없습니다."));
	}

	private Shop shop(UUID owner, UUID id) {
		return shops.findByIdAndOwnerId(id, owner).orElseThrow(() -> ApiException.notFound("영업장을 찾을 수 없습니다."));
	}

	private OrderRequest request(UUID owner, UUID id) {
		return requests.findByIdAndOwnerId(id, owner).orElseThrow(() -> ApiException.notFound("신청 건을 찾을 수 없습니다."));
	}

	private PurchaseOrder order(UUID owner, UUID id) {
		return orders.findByIdAndOwnerId(id, owner).orElseThrow(() -> ApiException.notFound("발주서를 찾을 수 없습니다."));
	}

	private ServiceCase kase(UUID owner, UUID id) {
		return cases.findByIdAndOwnerId(id, owner).orElseThrow(() -> ApiException.notFound("사후 처리 건을 찾을 수 없습니다."));
	}

	private Invoice invoice(UUID owner, UUID id) {
		return invoices.findByIdAndOwnerId(id, owner).orElseThrow(() -> ApiException.notFound("계산서를 찾을 수 없습니다."));
	}

	private static ModelCount count(List<OrderRequest> rs) {
		int a = 0;
		int i = 0;
		for (OrderRequest r : rs) {
			a += r.androidQty;
			i += r.iosQty;
		}
		return new ModelCount(a, i);
	}

	private static ModelCount countDevices(List<Device> ds) {
		int a = 0;
		int i = 0;
		for (Device d : ds) {
			if (d.model == DeviceModel.ANDROID) {
				a++;
			} else {
				i++;
			}
		}
		return new ModelCount(a, i);
	}

	private static boolean in(YearMonth ym, LocalDate date) {
		return date != null && YearMonth.from(date).equals(ym);
	}

	private static LocalDate firstDate(LocalDate current, LocalDate candidate) {
		return current == null || candidate.isBefore(current) ? candidate : current;
	}

	private static LocalDate day(LocalDate d) {
		return d == null ? LocalDate.now() : d;
	}

	private static int qty(Integer n) {
		int v = n == null ? 0 : n;
		if (v < 0 || v > 500) {
			throw ApiException.badRequest("수량은 0에서 500 사이로 입력해 주세요.");
		}
		return v;
	}

	private static boolean blank(String s) {
		return s == null || s.isBlank();
	}

	private static String text(String s, String message, int max) {
		if (blank(s)) {
			throw ApiException.badRequest(message);
		}
		String t = s.trim();
		if (t.length() > max) {
			throw ApiException.badRequest(max + "자 이내로 입력해 주세요.");
		}
		return t;
	}

	static String won(long amount) {
		return String.format("%,d원", amount);
	}

	static String label(RequestStatus s) {
		return switch (s) {
			case APPLIED -> "신청";
			case PAID -> "입금 확인";
			case CONFIRMED -> "확정";
			case ORDERED -> "발주";
			case SHIPPING -> "배송 중";
			case RETURNED_TO_SENDER -> "반송";
			case DELIVERED -> "배송 완료";
			case INVOICED -> "계산서 발행";
			case COMPLETED -> "완료";
			case CANCELLED_UNPAID -> "미입금 취소";
			case CANCELLED -> "취소";
			case REFUND_PENDING -> "환불 대기";
			case REFUNDED -> "환불 완료";
		};
	}

	static String label(InvoiceType t) {
		return t == InvoiceType.PERSONAL ? "개인 앞" : "본사 앞";
	}

	static String label(DeviceModel m) {
		return m == DeviceModel.IOS ? "iOS" : "안드로이드";
	}
}
