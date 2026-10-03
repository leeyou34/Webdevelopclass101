package io.github.leeyou34.todo.printer;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.leeyou34.todo.printer.Enums.DeviceStatus;
import io.github.leeyou34.todo.printer.PrinterDtos.ApprovalInput;
import io.github.leeyou34.todo.printer.PrinterDtos.AssignInput;
import io.github.leeyou34.todo.printer.PrinterDtos.CycleInput;
import io.github.leeyou34.todo.printer.PrinterDtos.Dashboard;
import io.github.leeyou34.todo.printer.PrinterDtos.DateInput;
import io.github.leeyou34.todo.printer.PrinterDtos.ExchangeInput;
import io.github.leeyou34.todo.printer.PrinterDtos.InvoiceRequestInput;
import io.github.leeyou34.todo.printer.PrinterDtos.ModelChangeInput;
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

/**
 * /printer/** — 모바일 프린터 운영관리 API (로그인 필요).
 * 주소 옆 번호는 기획서 "동작 목록"의 번호입니다.
 */
@RestController
@RequestMapping("/printer")
public class PrinterController {

	private final PrinterService service;
	private final PrinterInsightService insight;
	private final DemoDataService demo;
	private final OpsExtrasService extras;
	private final OpsChatService chat;

	public PrinterController(PrinterService service, PrinterInsightService insight, DemoDataService demo,
		OpsExtrasService extras, OpsChatService chat) {
		this.service = service;
		this.insight = insight;
		this.demo = demo;
		this.extras = extras;
		this.chat = chat;
	}

	// ---------------------------------------------------------------- 조회

	@GetMapping("/dashboard")
	public Dashboard dashboard(@AuthenticationPrincipal Jwt jwt,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
		return service.dashboard(owner(jwt), asOf);
	}

	/** 기간 분석: from~to (기본값: 이번 달 1일 ~ 오늘). 45일 이하는 일 단위, 넘으면 월 단위 추이 */
	@GetMapping("/analytics")
	public InsightDtos.Analytics analytics(@AuthenticationPrincipal Jwt jwt,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return insight.analytics(owner(jwt), from, to);
	}

	/** 일자·중요도별 운영 업무(오늘의 할 일에 표시) */
	@GetMapping("/tasks")
	public InsightDtos.OpsTasks tasks(@AuthenticationPrincipal Jwt jwt,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
		return insight.tasks(owner(jwt), asOf);
	}

	@GetMapping("/shops")
	public List<Shop> shops(@AuthenticationPrincipal Jwt jwt) {
		return service.listShops(owner(jwt));
	}

	@GetMapping("/cycles")
	public List<OrderCycle> cycles(@AuthenticationPrincipal Jwt jwt) {
		return service.listCycles(owner(jwt));
	}

	@GetMapping("/requests")
	public List<OrderRequest> requests(@AuthenticationPrincipal Jwt jwt,
		@RequestParam(required = false) UUID cycleId) {
		return service.listRequests(owner(jwt), cycleId);
	}

	@GetMapping("/purchase-orders")
	public List<PurchaseOrder> purchaseOrders(@AuthenticationPrincipal Jwt jwt) {
		return service.listPurchaseOrders(owner(jwt));
	}

	@GetMapping("/devices")
	public List<Device> devices(@AuthenticationPrincipal Jwt jwt,
		@RequestParam(required = false) DeviceStatus status) {
		return service.listDevices(owner(jwt), status);
	}

	@GetMapping("/cases")
	public List<ServiceCase> cases(@AuthenticationPrincipal Jwt jwt) {
		return service.listCases(owner(jwt));
	}

	@GetMapping("/invoices")
	public List<Invoice> invoices(@AuthenticationPrincipal Jwt jwt) {
		return service.listInvoices(owner(jwt));
	}

	@GetMapping("/adjustments")
	public List<Adjustment> adjustments(@AuthenticationPrincipal Jwt jwt) {
		return service.listAdjustments(owner(jwt));
	}

	@GetMapping("/activity")
	public List<ActivityLog> activity(@AuthenticationPrincipal Jwt jwt) {
		return service.listActivity(owner(jwt));
	}

	@GetMapping("/reports/monthly")
	public MonthlyReport monthly(@AuthenticationPrincipal Jwt jwt, @RequestParam String month) {
		return service.monthlyReport(owner(jwt), month);
	}

	// ---------------------------------------------------------------- 체험용 가상 데이터

	@PostMapping("/demo")
	public Map<String, Object> seedDemo(@AuthenticationPrincipal Jwt jwt) {
		return demo.seed(owner(jwt));
	}

	// ---------------------------------------------------------------- 영업장

	@PostMapping("/shops")
	public Shop createShop(@AuthenticationPrincipal Jwt jwt, @RequestBody ShopInput in) {
		return service.createShop(owner(jwt), in);
	}

	@PutMapping("/shops/{id}")
	public Shop updateShop(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
		@RequestBody ExtraDtos.ShopUpdateInput in) {
		return extras.updateShop(owner(jwt), id, in);
	}

	@PostMapping("/shops/{id}/close")
	public Shop closeShop(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
		@RequestBody(required = false) ExtraDtos.CloseShopInput in) {
		return extras.closeShop(owner(jwt), id, in);
	}

	@PostMapping("/shops/{id}/reopen")
	public Shop reopenShop(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return extras.reopenShop(owner(jwt), id);
	}

	// ---------------------------------------------------------------- 입금 대조 · 시리얼 조회 · 챗봇

	@GetMapping("/deposits")
	public List<Deposit> deposits(@AuthenticationPrincipal Jwt jwt) {
		return extras.listDeposits(owner(jwt));
	}

	@PostMapping("/deposits/import")
	public ExtraDtos.DepositImportResult importDeposits(@AuthenticationPrincipal Jwt jwt,
		@RequestBody ExtraDtos.DepositImportInput in) {
		return extras.importDeposits(owner(jwt), in);
	}

	@PostMapping("/deposits/{id}/match")
	public Deposit matchDeposit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
		@RequestBody ExtraDtos.DepositMatchInput in) {
		return extras.matchDeposit(owner(jwt), id, in);
	}

	@GetMapping("/devices/lookup")
	public ExtraDtos.DeviceLookup lookup(@AuthenticationPrincipal Jwt jwt, @RequestParam String serial) {
		return extras.lookup(owner(jwt), serial);
	}

	@PostMapping("/chat")
	public ExtraDtos.ChatReply chat(@AuthenticationPrincipal Jwt jwt, @RequestBody ExtraDtos.ChatInput in,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
		return chat.reply(owner(jwt), in == null ? null : in.message(), asOf);
	}

	// ---------------------------------------------------------------- 동작 1~21

	@PostMapping("/cycles") // 1
	public OrderCycle openCycle(@AuthenticationPrincipal Jwt jwt, @RequestBody CycleInput in) {
		return service.openCycle(owner(jwt), in);
	}

	@PostMapping("/cycles/{id}/close") // 1
	public OrderCycle closeCycle(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
		@RequestBody(required = false) DateInput in) {
		return service.closeCycle(owner(jwt), id, in);
	}

	@PostMapping("/requests") // 2
	public OrderRequest createRequest(@AuthenticationPrincipal Jwt jwt, @RequestBody RequestInput in) {
		return service.createRequest(owner(jwt), in);
	}

	@PostMapping("/requests/{id}/payment") // 3
	public OrderRequest pay(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
		@RequestBody(required = false) PaymentInput in) {
		return service.confirmPayment(owner(jwt), id, in);
	}

	@PostMapping("/cycles/{id}/confirm") // 4
	public List<OrderRequest> confirm(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
		@RequestBody(required = false) DateInput in) {
		return service.confirmCycle(owner(jwt), id, in);
	}

	@PostMapping("/cycles/{id}/purchase-orders") // 5
	public PurchaseOrder order(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
		@RequestBody PurchaseOrderInput in) {
		return service.createPurchaseOrder(owner(jwt), id, in);
	}

	@PostMapping("/purchase-orders/{id}/approval") // 6
	public PurchaseOrder approve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
		@RequestBody ApprovalInput in) {
		return service.recordApproval(owner(jwt), id, in);
	}

	@PostMapping("/purchase-orders/{id}/receipts") // 7
	public List<Device> receive(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
		@RequestBody ReceiveInput in) {
		return service.receive(owner(jwt), id, in);
	}

	@PostMapping("/requests/{id}/assign") // 8
	public List<Device> assign(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
		@RequestBody(required = false) AssignInput in) {
		return service.assign(owner(jwt), id, in);
	}

	@PostMapping("/requests/{id}/ship") // 9
	public OrderRequest ship(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @RequestBody ShipInput in) {
		return service.ship(owner(jwt), id, in);
	}

	@PostMapping("/requests/{id}/deliver") // 10
	public OrderRequest deliver(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
		@RequestBody(required = false) DateInput in) {
		return service.deliver(owner(jwt), id, in);
	}

	@PostMapping("/cycles/{id}/invoices") // 11
	public List<Invoice> requestInvoices(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
		@RequestBody InvoiceRequestInput in) {
		return service.requestInvoices(owner(jwt), id, in);
	}

	@PostMapping("/invoices/{id}/issue") // 12
	public Invoice issue(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
		@RequestBody(required = false) DateInput in) {
		return service.issueInvoice(owner(jwt), id, in);
	}

	@PostMapping("/invoices/{id}/payment") // 13
	public Invoice invoicePaid(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
		@RequestBody(required = false) DateInput in) {
		return service.confirmInvoicePayment(owner(jwt), id, in);
	}

	@PostMapping("/requests/{id}/cancel") // 14
	public OrderRequest cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @RequestBody ReasonInput in) {
		return service.cancel(owner(jwt), id, in);
	}

	@PostMapping("/requests/{id}/change-model") // 15
	public OrderRequest changeModel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
		@RequestBody ModelChangeInput in) {
		return service.changeModel(owner(jwt), id, in);
	}

	@PostMapping("/requests/{id}/return-to-sender") // 16
	public OrderRequest returnToSender(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
		@RequestBody ReturnToSenderInput in) {
		return service.returnToSender(owner(jwt), id, in);
	}

	@PostMapping("/devices/{serial}/return") // 17
	public ServiceCase openReturn(@AuthenticationPrincipal Jwt jwt, @PathVariable String serial,
		@RequestBody ReasonInput in) {
		return service.openReturn(owner(jwt), serial, in);
	}

	@PostMapping("/cases/{id}/recover") // 17, 19
	public ServiceCase recover(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
		@RequestBody(required = false) DateInput in) {
		return service.recover(owner(jwt), id, in);
	}

	@PostMapping("/requests/{id}/refund") // 18
	public OrderRequest refund(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
		@RequestBody(required = false) RefundInput in) {
		return service.refund(owner(jwt), id, in);
	}

	@PostMapping("/devices/{serial}/exchange") // 19
	public ServiceCase exchange(@AuthenticationPrincipal Jwt jwt, @PathVariable String serial,
		@RequestBody ExchangeInput in) {
		return service.exchange(owner(jwt), serial, in);
	}

	@PostMapping("/devices/{serial}/repair") // 20
	public ServiceCase openRepair(@AuthenticationPrincipal Jwt jwt, @PathVariable String serial,
		@RequestBody ReasonInput in) {
		return service.openRepair(owner(jwt), serial, in);
	}

	@PostMapping("/cases/{id}/repair-result") // 20
	public ServiceCase repairResult(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
		@RequestBody RepairResultInput in) {
		return service.repairResult(owner(jwt), id, in);
	}

	@PostMapping("/months/{month}/close") // 21
	public MonthlyReport closeMonth(@AuthenticationPrincipal Jwt jwt, @PathVariable String month) {
		return service.closeMonth(owner(jwt), month);
	}

	private static UUID owner(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
