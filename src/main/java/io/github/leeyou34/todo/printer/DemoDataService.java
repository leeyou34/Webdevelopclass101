package io.github.leeyou34.todo.printer;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.leeyou34.todo.common.ApiException;
import io.github.leeyou34.todo.printer.Enums.InvoiceType;
import io.github.leeyou34.todo.printer.Enums.ShopType;
import io.github.leeyou34.todo.printer.PrinterDtos.ApprovalInput;
import io.github.leeyou34.todo.printer.PrinterDtos.CycleInput;
import io.github.leeyou34.todo.printer.PrinterDtos.DateInput;
import io.github.leeyou34.todo.printer.PrinterDtos.ExchangeInput;
import io.github.leeyou34.todo.printer.PrinterDtos.InvoiceRequestInput;
import io.github.leeyou34.todo.printer.PrinterDtos.PaymentInput;
import io.github.leeyou34.todo.printer.PrinterDtos.PurchaseOrderInput;
import io.github.leeyou34.todo.printer.PrinterDtos.ReasonInput;
import io.github.leeyou34.todo.printer.PrinterDtos.ReceiveInput;
import io.github.leeyou34.todo.printer.PrinterDtos.RefundInput;
import io.github.leeyou34.todo.printer.PrinterDtos.RequestInput;
import io.github.leeyou34.todo.printer.PrinterDtos.ReturnToSenderInput;
import io.github.leeyou34.todo.printer.PrinterDtos.ShipInput;
import io.github.leeyou34.todo.printer.PrinterDtos.ShopInput;

/**
 * 체험용 가상 데이터. 영업장·신청자 이름과 연락처는 모두 지어낸 값이고(가린 형태로 표기),
 * 업무 흐름만 2016~2017년 실제 운영 방식을 본뜹니다.
 *
 * - 두 달 전: 신청부터 본사 수금까지 끝난 달
 * - 지난달: 배송·계산서까지 끝났고 본사 미수, 반품·교환·AS·반송·취소가 섞인 달
 * - 이번 달: 신청을 받는 중인 달(직접 마감·확정·발주를 해 볼 수 있음)
 */
@Service
@Transactional
public class DemoDataService {

	private static final String[][] SHOPS = {
		{ "S001", "한빛(방)", "서울", "남부1팀", "김0진", "031-000-2814", "경기도 안산시 ***", "SPECIALTY" },
		{ "S002", "새봄(방)", "서울", "남부1팀", "이0숙", "031-000-5521", "경기도 시흥시 ***", "SPECIALTY" },
		{ "S003", "푸른솔(방)", "서울", "북부2팀", "박0희", "02-000-7310", "서울시 노원구 ***", "SPECIALTY" },
		{ "S004", "다온(방)", "경인", "인천팀", "최0영", "032-000-4402", "인천시 남동구 ***", "SPECIALTY" },
		{ "S005", "가람(방)", "경인", "인천팀", "정0미", "032-000-9183", "인천시 부평구 ***", "SPECIALTY" },
		{ "S006", "누리(방)", "부산", "동부팀", "한0자", "051-000-2267", "부산시 해운대구 ***", "SPECIALTY" },
		{ "S007", "온새미(방)", "부산", "동부팀", "윤0경", "051-000-6630", "부산시 동래구 ***", "SPECIALTY" },
		{ "S008", "중앙(영)", "서울", "직영", "직영 영업소", "02-000-1100", "서울시 중구 ***", "DIRECT" },
	};

	private final PrinterService svc;
	private final ShopRepository shops;
	private final DeviceRepository devices;

	public DemoDataService(PrinterService svc, ShopRepository shops, DeviceRepository devices) {
		this.svc = svc;
		this.shops = shops;
		this.devices = devices;
	}

	public Map<String, Object> seed(UUID owner) {
		if (shops.existsByOwnerId(owner)) {
			throw ApiException.conflict("이미 데이터가 있는 계정입니다. 체험 데이터는 빈 계정에서만 만들 수 있습니다.");
		}
		LocalDate today = LocalDate.now();
		YearMonth thisMonth = YearMonth.from(today);
		List<Shop> s = new ArrayList<>();
		for (String[] row : SHOPS) {
			s.add(svc.createShop(owner, new ShopInput(row[0], row[1], row[2], row[3], ShopType.valueOf(row[7]), row[4],
				row[5], row[6], LocalDate.of(2010, 3, 1))));
		}

		// 두 달 전: 처음부터 끝까지 정상 처리
		YearMonth q = thisMonth.minusMonths(2);
		OrderCycle cq = svc.openCycle(owner, new CycleInput(q.toString(), q.atDay(7)));
		List<OrderRequest> rq = new ArrayList<>();
		rq.add(req(owner, cq, s.get(0), "김0아", 2, 0));
		rq.add(req(owner, cq, s.get(1), "박0연", 1, 1));
		rq.add(req(owner, cq, s.get(3), "최0은", 3, 0));
		rq.add(req(owner, cq, s.get(5), "정0라", 1, 0));
		rq.add(req(owner, cq, s.get(7), "직영 영업소", 2, 1));
		for (OrderRequest r : rq) {
			svc.confirmPayment(owner, r.id, new PaymentInput(null, q.atDay(4)));
		}
		runToDelivery(owner, cq, q, rq, today, false);
		invoice(owner, cq, q.plusMonths(1).atDay(10), today, true, q.plusMonths(1).atDay(25));

		// 지난달: 예외가 섞인 달
		YearMonth p = thisMonth.minusMonths(1);
		OrderCycle cp = svc.openCycle(owner, new CycleInput(p.toString(), p.atDay(7)));
		List<OrderRequest> rp = new ArrayList<>();
		rp.add(req(owner, cp, s.get(0), "이0진", 2, 0));
		rp.add(req(owner, cp, s.get(2), "강0희", 1, 1));
		rp.add(req(owner, cp, s.get(3), "윤0서", 2, 0));
		rp.add(req(owner, cp, s.get(4), "임0주", 1, 0));
		rp.add(req(owner, cp, s.get(6), "조0린", 1, 0));
		OrderRequest unpaid = req(owner, cp, s.get(5), "한0율", 1, 0);
		for (OrderRequest r : rp) {
			svc.confirmPayment(owner, r.id, new PaymentInput(null, p.atDay(5)));
		}
		svc.closeCycle(owner, cp.id, new DateInput(p.atDay(8))); // 미입금 1건 자동 취소
		svc.confirmCycle(owner, cp.id, new DateInput(p.atDay(8)));
		OrderRequest cancelled = rp.remove(rp.size() - 1);
		svc.cancel(owner, cancelled.id, new ReasonInput("영업장 사정으로 신청 취소", p.atDay(9)));
		svc.refund(owner, cancelled.id, new RefundInput(null, p.atDay(12)));
		runToDelivery(owner, cp, p, rp, today, true);
		invoice(owner, cp, cap(thisMonth.atDay(10), today), today, false, null);
		LocalDate after = cap(thisMonth.atDay(3), today);
		String returned = devices.findByOwnerIdAndRequestId(owner, rp.get(1).id).get(0).serial;
		ServiceCase ret = svc.openReturn(owner, returned, new ReasonInput("사용하지 않게 되어 반품", after));
		svc.recover(owner, ret.id, new DateInput(after));
		String broken = devices.findByOwnerIdAndRequestId(owner, rp.get(2).id).get(0).serial;
		svc.exchange(owner, broken, new ExchangeInput(null, "블루투스 연결 지연(불량 로트)", after));
		String repair = devices.findByOwnerIdAndRequestId(owner, rp.get(0).id).get(0).serial;
		svc.openRepair(owner, repair, new ReasonInput("영수증 출력 안 됨", after));

		// 이번 달: 신청 받는 중
		OrderCycle cm = svc.openCycle(owner, new CycleInput(thisMonth.toString(), thisMonth.atDay(7)));
		OrderRequest m1 = req(owner, cm, s.get(0), "서0윤", 1, 0);
		OrderRequest m2 = req(owner, cm, s.get(1), "문0희", 0, 1);
		OrderRequest m3 = req(owner, cm, s.get(4), "배0진", 2, 0);
		req(owner, cm, s.get(6), "권0아", 1, 0);
		req(owner, cm, s.get(7), "직영 영업소", 1, 0);
		LocalDate payDay = cap(thisMonth.atDay(3), today);
		for (OrderRequest r : List.of(m1, m2, m3)) {
			svc.confirmPayment(owner, r.id, new PaymentInput(null, payDay));
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("shops", s.size());
		out.put("cycles", List.of(q.toString(), p.toString(), thisMonth.toString()));
		out.put("unpaidCancelled", unpaid.applicantName);
		return out;
	}

	/** 확정 → 발주 → 품의 → 입고 → 배정 → 발송 → 배송 완료. withReturnToSender면 한 건을 반송 후 재발송 */
	private void runToDelivery(UUID owner, OrderCycle c, YearMonth m, List<OrderRequest> rs, LocalDate today,
		boolean withReturnToSender) {
		if (c.status == Enums.CycleStatus.OPEN) {
			svc.closeCycle(owner, c.id, new DateInput(m.atDay(8)));
			svc.confirmCycle(owner, c.id, new DateInput(m.atDay(8)));
		}
		PurchaseOrder po = svc.createPurchaseOrder(owner, c.id,
			new PurchaseOrderInput(null, m.atDay(8), 2, 1, "39A 3개, 27A 2개"));
		svc.recordApproval(owner, po.id, new ApprovalInput("영업팀-" + m.toString().replace("-", "") + "-01", m.atDay(9)));
		svc.receive(owner, po.id, new ReceiveInput(m.atDay(22), null, po.androidQty, po.iosQty));
		int n = 0;
		for (OrderRequest r : rs) {
			svc.assign(owner, r.id, null);
			svc.ship(owner, r.id, new ShipInput("6" + String.format("%011d", 30000000L + (++n) * 7919L + m.getMonthValue()),
				m.atDay(26)));
		}
		if (withReturnToSender) {
			OrderRequest r = rs.get(rs.size() - 1);
			svc.returnToSender(owner, r.id, new ReturnToSenderInput("주소 오기로 반송", null, m.atDay(27)));
			svc.ship(owner, r.id, new ShipInput("6" + String.format("%011d", 39999999L + m.getMonthValue()), m.atDay(28)));
		}
		for (OrderRequest r : rs) {
			svc.deliver(owner, r.id, new DateInput(cap(m.atDay(28), today)));
		}
	}

	private void invoice(UUID owner, OrderCycle c, LocalDate issueOn, LocalDate today, boolean hqPaid,
		LocalDate paidOn) {
		LocalDate issue = cap(issueOn, today);
		for (Invoice i : svc.requestInvoices(owner, c.id, new InvoiceRequestInput(InvoiceType.PERSONAL, issue))) {
			svc.issueInvoice(owner, i.id, new DateInput(issue));
		}
		for (Invoice i : svc.requestInvoices(owner, c.id, new InvoiceRequestInput(InvoiceType.HQ, issue))) {
			svc.issueInvoice(owner, i.id, new DateInput(issue));
			if (hqPaid) {
				svc.confirmInvoicePayment(owner, i.id, new DateInput(cap(paidOn, today)));
			}
		}
	}

	private OrderRequest req(UUID owner, OrderCycle c, Shop shop, String name, int android, int ios) {
		return svc.createRequest(owner, new RequestInput(c.id, shop.id, name, android, ios));
	}

	private static LocalDate cap(LocalDate d, LocalDate today) {
		return d.isAfter(today) ? today : d;
	}
}
