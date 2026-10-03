package io.github.leeyou34.todo.printer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.leeyou34.todo.common.ApiException;
import io.github.leeyou34.todo.printer.Enums.RequestStatus;
import io.github.leeyou34.todo.printer.ExtraDtos.CloseShopInput;
import io.github.leeyou34.todo.printer.ExtraDtos.DepositImportInput;
import io.github.leeyou34.todo.printer.ExtraDtos.DepositImportResult;
import io.github.leeyou34.todo.printer.ExtraDtos.DepositMatchInput;
import io.github.leeyou34.todo.printer.ExtraDtos.DeviceLookup;
import io.github.leeyou34.todo.printer.ExtraDtos.ShopUpdateInput;
import io.github.leeyou34.todo.printer.PrinterDtos.PaymentInput;

/**
 * 실제 업무 자료에서 확인한 보조 업무:
 * 영업장 정보 변경·폐쇄(폐점, 명칭 변경), 통장 입금 내역 대조(미확인 입금자 처리), 시리얼(바코드) 조회
 */
@Service
@Transactional
public class OpsExtrasService {

	private static final Pattern DATE = Pattern.compile(
		"(\\d{4})[-./](\\d{1,2})[-./](\\d{1,2})|(\\d{1,2})/(\\d{1,2})/(\\d{4})|(\\d{1,2})[-./](\\d{1,2})");
	private static final Pattern AMOUNT = Pattern.compile("(?<![\\d-])(\\d{1,3}(?:,\\d{3})+|\\d{4,9})(?:원)?(?![\\d-])");

	private final PrinterService svc;
	private final ShopRepository shops;
	private final RequestRepository requests;
	private final DepositRepository deposits;
	private final DeviceRepository devices;
	private final PurchaseOrderRepository orders;
	private final CaseRepository cases;

	public OpsExtrasService(PrinterService svc, ShopRepository shops, RequestRepository requests,
		DepositRepository deposits, DeviceRepository devices, PurchaseOrderRepository orders, CaseRepository cases) {
		this.svc = svc;
		this.shops = shops;
		this.requests = requests;
		this.deposits = deposits;
		this.devices = devices;
		this.orders = orders;
		this.cases = cases;
	}

	// ===================================================================== 영업장 변경·폐쇄

	public Shop updateShop(UUID owner, UUID id, ShopUpdateInput in) {
		Shop s = shop(owner, id);
		String before = s.name;
		if (in.name() != null && !in.name().isBlank()) {
			s.name = limit(in.name(), 50);
		}
		if (in.division() != null) {
			s.division = limit(in.division(), 20);
		}
		if (in.team() != null) {
			s.team = limit(in.team(), 30);
		}
		if (in.type() != null) {
			s.type = in.type();
		}
		if (in.managerName() != null) {
			s.managerName = limit(in.managerName(), 20);
		}
		if (in.phone() != null) {
			s.phone = limit(in.phone(), 20);
		}
		if (in.address() != null) {
			s.address = limit(in.address(), 120);
		}
		svc.log(owner, 0, "영업장 정보 변경", "shop", s.id, before.equals(s.name) ? s.name : before + " → " + s.name);
		return s;
	}

	public Shop closeShop(UUID owner, UUID id, CloseShopInput in) {
		Shop s = shop(owner, id);
		if (!s.active) {
			throw ApiException.conflict("이미 폐쇄된 영업장입니다.");
		}
		s.active = false;
		s.closedOn = in == null || in.date() == null ? LocalDate.now() : in.date();
		s.note = in == null || in.note() == null ? "폐쇄" : limit(in.note(), 200);
		svc.log(owner, 0, "영업장 폐쇄", "shop", s.id, s.name + " · " + s.note);
		return s;
	}

	public Shop reopenShop(UUID owner, UUID id) {
		Shop s = shop(owner, id);
		s.active = true;
		s.closedOn = null;
		svc.log(owner, 0, "영업장 재개", "shop", s.id, s.name);
		return s;
	}

	// ===================================================================== 입금 대조

	@Transactional(readOnly = true)
	public List<Deposit> listDeposits(UUID owner) {
		return deposits.findByOwnerIdOrderByDepositedOnDescCreatedAtDesc(owner);
	}

	/**
	 * 통장 입금 내역을 붙여 넣으면 한 줄씩 신청 건과 맞춰 봅니다.
	 * 금액(VAT 포함)이 같고, 입금자명에 신청자 이름이 들어 있는 "입금 대기" 신청이 하나뿐일 때만 자동으로 입금 확인합니다.
	 */
	public DepositImportResult importDeposits(UUID owner, DepositImportInput in) {
		if (in == null || in.text() == null || in.text().isBlank()) {
			throw ApiException.badRequest("입금 내역을 붙여 넣어 주세요. 한 줄에 날짜, 입금자, 금액 순서입니다.");
		}
		LocalDate fallback = in.defaultDate() == null ? LocalDate.now() : in.defaultDate();
		List<Deposit> created = new ArrayList<>();
		int matched = 0;
		for (String raw : in.text().split("\\r?\\n")) {
			String line = raw.trim();
			if (line.isEmpty()) {
				continue;
			}
			Deposit d = parse(owner, line, fallback);
			if (d == null) {
				continue;
			}
			deposits.save(d);
			if (autoMatch(owner, d)) {
				matched++;
			}
			created.add(d);
		}
		if (created.isEmpty()) {
			throw ApiException.badRequest("읽을 수 있는 입금 내역이 없습니다. 예: 2017-08-03  한빛서0윤  157,300");
		}
		svc.log(owner, 3, "입금 내역 대조", "deposit", null,
			created.size() + "건 중 " + matched + "건 자동 입금 확인, 미확인 " + (created.size() - matched) + "건");
		return new DepositImportResult(created.size(), matched, created.size() - matched, created);
	}

	public Deposit matchDeposit(UUID owner, UUID depositId, DepositMatchInput in) {
		Deposit d = deposits.findByIdAndOwnerId(depositId, owner)
			.orElseThrow(() -> ApiException.notFound("입금 내역을 찾을 수 없습니다."));
		if (d.requestId != null) {
			throw ApiException.conflict("이미 신청 건과 맞춘 입금입니다.");
		}
		if (in == null || in.requestId() == null) {
			throw ApiException.badRequest("맞출 신청 건을 골라 주세요.");
		}
		svc.confirmPayment(owner, in.requestId(), new PaymentInput(d.amount, d.depositedOn));
		d.requestId = in.requestId();
		d.matchedOn = LocalDate.now();
		d.note = "직접 확인";
		return d;
	}

	private boolean autoMatch(UUID owner, Deposit d) {
		String who = norm(d.depositorName);
		List<OrderRequest> candidates = new ArrayList<>();
		for (OrderRequest r : requests.findByOwnerIdAndStatus(owner, RequestStatus.APPLIED)) {
			if (Pricing.withVat(r.personalAmount) != d.amount || r.personalAmount == 0) {
				continue;
			}
			if (who.contains(norm(r.applicantName))) {
				candidates.add(r);
			}
		}
		if (candidates.size() > 1) {
			// 동명이인이면 영업장명까지 들어 있는 건만 남김
			List<OrderRequest> narrowed = candidates.stream()
				.filter(r -> shops.findByIdAndOwnerId(r.shopId, owner).map(s -> who.contains(norm(shortName(s.name))))
					.orElse(false))
				.toList();
			candidates = new ArrayList<>(narrowed);
		}
		if (candidates.size() != 1) {
			d.note = candidates.isEmpty() ? "맞는 신청 없음" : "후보 " + candidates.size() + "건, 직접 확인 필요";
			return false;
		}
		OrderRequest r = candidates.get(0);
		svc.confirmPayment(owner, r.id, new PaymentInput(d.amount, d.depositedOn));
		d.requestId = r.id;
		d.matchedOn = LocalDate.now();
		d.note = "자동 확인";
		return true;
	}

	private static Deposit parse(UUID owner, String line, LocalDate fallback) {
		String rest = line;
		LocalDate date = fallback;
		Matcher dm = DATE.matcher(rest);
		if (dm.find()) {
			LocalDate parsed = toDate(dm, fallback);
			if (parsed != null) {
				date = parsed;
				rest = rest.substring(0, dm.start()) + " " + rest.substring(dm.end());
			}
		}
		Matcher am = AMOUNT.matcher(rest);
		Long amount = null;
		int aStart = -1;
		int aEnd = -1;
		while (am.find()) {
			long v = Long.parseLong(am.group(1).replace(",", ""));
			if (v >= 1000) {
				amount = v;
				aStart = am.start();
				aEnd = am.end();
			}
		}
		if (amount == null) {
			return null;
		}
		String name = (rest.substring(0, aStart) + " " + rest.substring(aEnd)).replaceAll("[\\t,|]+", " ").trim()
			.replaceAll("\\s{2,}", " ");
		if (name.isEmpty()) {
			return null;
		}
		Deposit d = new Deposit();
		d.ownerId = owner;
		d.depositedOn = date;
		d.depositorName = name.length() > 40 ? name.substring(0, 40) : name;
		d.amount = amount;
		return d;
	}

	private static LocalDate toDate(Matcher m, LocalDate fallback) {
		try {
			if (m.group(1) != null) {
				return LocalDate.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
			}
			if (m.group(4) != null) {
				return LocalDate.of(Integer.parseInt(m.group(6)), Integer.parseInt(m.group(4)), Integer.parseInt(m.group(5)));
			}
			return LocalDate.of(fallback.getYear(), Integer.parseInt(m.group(7)), Integer.parseInt(m.group(8)));
		} catch (RuntimeException e) {
			return null;
		}
	}

	// ===================================================================== 시리얼(바코드) 조회

	@Transactional(readOnly = true)
	public DeviceLookup lookup(UUID owner, String serial) {
		if (serial == null || serial.isBlank()) {
			throw ApiException.badRequest("시리얼을 입력하거나 바코드를 찍어 주세요.");
		}
		String key = serial.trim().toUpperCase();
		Device d = devices.findByOwnerIdAndSerial(owner, key)
			.orElseThrow(() -> ApiException.notFound("등록되지 않은 시리얼입니다: " + key));
		Shop shop = d.shopId == null ? null : shops.findByIdAndOwnerId(d.shopId, owner).orElse(null);
		OrderRequest req = d.requestId == null ? null : requests.findByIdAndOwnerId(d.requestId, owner).orElse(null);
		PurchaseOrder po = d.purchaseOrderId == null ? null
			: orders.findByIdAndOwnerId(d.purchaseOrderId, owner).orElse(null);
		List<ServiceCase> related = cases.findByOwnerIdOrderByOpenedOnDesc(owner).stream()
			.filter(k -> key.equals(k.serial) || key.equals(k.newSerial)).toList();
		return new DeviceLookup(d, shop, req, po, related);
	}

	// ===================================================================== 도구

	private Shop shop(UUID owner, UUID id) {
		return shops.findByIdAndOwnerId(id, owner).orElseThrow(() -> ApiException.notFound("영업장을 찾을 수 없습니다."));
	}

	private static String limit(String s, int max) {
		String t = s.trim();
		return t.length() > max ? t.substring(0, max) : t;
	}

	/** 비교용: 공백·괄호 표기 제거 */
	static String norm(String s) {
		return s == null ? "" : s.replaceAll("\\s+|\\(방\\)|\\(영\\)|\\(특\\)|특약점|영업소|[()·.\\-]", "").toUpperCase();
	}

	static String shortName(String shopName) {
		return shopName.replaceAll("\\(.*?\\)", "").replace("특약점", "").replace("영업소", "").trim();
	}
}
