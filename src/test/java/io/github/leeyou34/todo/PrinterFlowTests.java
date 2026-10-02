package io.github.leeyou34.todo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

/**
 * 모바일 프린터 운영관리: 한 달 사이클 전체 흐름, 예외(취소·반송·반품·교환·AS), 사용자 간 분리를 확인합니다.
 */
@SpringBootTest
class PrinterFlowTests {

	@Autowired
	WebApplicationContext context;

	MockMvc mvc;

	String token;

	@BeforeEach
	void setUp() throws Exception {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
		token = login();
	}

	@Test
	void fullMonthlyCycle() throws Exception {
		String shopA = id(call("/printer/shops", shopJson("A01", "한빛(방)", "SPECIALTY")));
		String shopD = id(call("/printer/shops", shopJson("D01", "중앙(영)", "DIRECT")));
		String cycle = id(call("/printer/cycles", "{\"month\":\"2017-08\",\"closesOn\":\"2017-08-07\"}"));

		// 2. 신청 3건: 특약점 안드로이드 2, 특약점 iOS 1, 직영 안드로이드 1 + 미입금 1건
		String r1 = id(call("/printer/requests", requestJson(cycle, shopA, "김0아", 2, 0)));
		String r2 = id(call("/printer/requests", requestJson(cycle, shopA, "박0연", 0, 1)));
		String r3 = id(call("/printer/requests", requestJson(cycle, shopD, "직영", 1, 0)));
		String r4 = id(call("/printer/requests", requestJson(cycle, shopA, "최0은", 1, 0)));
		assertThat(numField(get("/printer/requests?cycleId=" + cycle), r2, "personalAmount")).isEqualTo(143000);
		assertThat(numField(get("/printer/requests?cycleId=" + cycle), r2, "hqAmount")).isEqualTo(30000);
		assertThat(numField(get("/printer/requests?cycleId=" + cycle), r3, "hqAmount")).isEqualTo(143000);

		// 3. 입금 확인 (금액이 다르면 거부)
		call("/printer/requests/" + r1 + "/payment", "{\"amount\":100000}", status().isBadRequest());
		for (String r : List.of(r1, r2, r3)) {
			call("/printer/requests/" + r + "/payment", "{\"date\":\"2017-08-03\"}");
		}

		// 1. 마감 → 미입금 자동 취소, 4. 확정
		call("/printer/cycles/" + cycle + "/close", "{\"date\":\"2017-08-08\"}");
		assertThat(field(get("/printer/requests?cycleId=" + cycle), r4, "status")).isEqualTo("CANCELLED_UNPAID");
		call("/printer/requests", requestJson(cycle, shopA, "늦은 신청", 1, 0), status().isConflict());
		call("/printer/cycles/" + cycle + "/confirm", "{}");

		// 5. 발주서(여유분 안드로이드 1), 6. 품의, 7. 입고(자동 시리얼)
		String po = id(call("/printer/cycles/" + cycle + "/purchase-orders",
			"{\"orderedOn\":\"2017-08-08\",\"bufferAndroid\":1}"));
		String poBody = get("/printer/purchase-orders");
		assertThat(str(poBody, "$[0].orderNo")).isEqualTo("ON-20170808");
		assertThat(num(poBody, "$[0].androidQty")).isEqualTo(4);
		call("/printer/purchase-orders/" + po + "/approval", "{\"approvalNo\":\"영업팀-17-0040\"}");
		call("/printer/purchase-orders/" + po + "/receipts", "{\"date\":\"2017-08-22\",\"autoAndroid\":4,\"autoIos\":1}");
		call("/printer/purchase-orders/" + po + "/receipts", "{\"autoAndroid\":1}", status().isBadRequest());

		// 8~10. 배정 → 발송 → (r2 반송 후 재발송) → 배송 완료
		for (String r : List.of(r1, r2, r3)) {
			call("/printer/requests/" + r + "/assign", "{}");
			call("/printer/requests/" + r + "/ship", "{\"trackingNo\":\"600011112222\",\"date\":\"2017-08-26\"}");
		}
		call("/printer/requests/" + r2 + "/return-to-sender", "{\"reason\":\"주소 오기\"}");
		call("/printer/requests/" + r2 + "/ship", "{\"trackingNo\":\"600011113333\",\"date\":\"2017-08-28\"}");
		for (String r : List.of(r1, r2, r3)) {
			call("/printer/requests/" + r + "/deliver", "{\"date\":\"2017-08-29\"}");
		}

		// 11~13. 개인 앞 계산서 2장, 본사 앞 1장(30,000 + 143,000)
		String personal = call("/printer/cycles/" + cycle + "/invoices", "{\"type\":\"PERSONAL\"}");
		assertThat((List<?>) JsonPath.read(personal, "$")).hasSize(2);
		for (Object pid : (List<?>) JsonPath.read(personal, "$[*].id")) {
			call("/printer/invoices/" + pid + "/issue", "{\"date\":\"2017-09-10\"}");
		}
		String hq = call("/printer/cycles/" + cycle + "/invoices", "{\"type\":\"HQ\"}");
		assertThat(((Number) JsonPath.read(hq, "$[0].amount")).longValue()).isEqualTo(173000);
		String hqId = JsonPath.read(hq, "$[0].id");
		call("/printer/invoices/" + hqId + "/issue", "{\"date\":\"2017-09-10\"}");
		assertThat(field(get("/printer/requests?cycleId=" + cycle), r1, "status")).isEqualTo("COMPLETED");
		assertThat(field(get("/printer/requests?cycleId=" + cycle), r2, "status")).isEqualTo("INVOICED");

		// 본사 미수 → 기한 초과 경고 → 수금 확인 → 완료
		String dash = get("/printer/dashboard?asOf=2017-10-20");
		assertThat(num(dash, "$.overdueCount")).isEqualTo(1);
		call("/printer/invoices/" + hqId + "/payment", "{\"date\":\"2017-10-21\"}");
		assertThat(field(get("/printer/requests?cycleId=" + cycle), r2, "status")).isEqualTo("COMPLETED");
		assertThat(field(get("/printer/requests?cycleId=" + cycle), r3, "status")).isEqualTo("COMPLETED");

		// 21. 월 마감: 영업 기준 8월 4대, 회계 기준 9월 4대, 수익 = 13,000원 × 4대
		String aug = call("/printer/months/2017-08/close", "");
		assertThat(num(aug, "$.salesBasis.android") + num(aug, "$.salesBasis.ios")).isEqualTo(4);
		assertThat(num(aug, "$.profit")).isEqualTo(52000);
		assertThat(num(aug, "$.cancelled")).isEqualTo(1);
		String sep = get("/printer/reports/monthly?month=2017-09");
		assertThat(num(sep, "$.accountingBasis.android") + num(sep, "$.accountingBasis.ios")).isEqualTo(4);

		// 기록이 남았는지
		assertThat((List<?>) JsonPath.read(get("/printer/activity"), "$")).hasSizeGreaterThan(15);
	}

	@Test
	void exceptionsAfterDelivery() throws Exception {
		String shop = id(call("/printer/shops", shopJson("A01", "새봄(방)", "SPECIALTY")));
		String cycle = id(call("/printer/cycles", "{\"month\":\"2017-05\"}"));
		String r1 = id(call("/printer/requests", requestJson(cycle, shop, "이0진", 2, 0)));
		String r2 = id(call("/printer/requests", requestJson(cycle, shop, "강0희", 1, 0)));
		call("/printer/requests/" + r1 + "/payment", "{}");
		call("/printer/requests/" + r2 + "/payment", "{}");
		call("/printer/cycles/" + cycle + "/close", "{}");
		call("/printer/cycles/" + cycle + "/confirm", "{}");

		// 14. 발주 전 취소 → 환불 대기 → 18. 환불 완료
		call("/printer/requests/" + r2 + "/cancel", "{\"reason\":\"개인 사정\"}");
		assertThat(field(get("/printer/requests?cycleId=" + cycle), r2, "status")).isEqualTo("REFUND_PENDING");
		call("/printer/requests/" + r2 + "/refund", "{}");
		assertThat(field(get("/printer/requests?cycleId=" + cycle), r2, "status")).isEqualTo("REFUNDED");

		String po = id(call("/printer/cycles/" + cycle + "/purchase-orders", "{\"bufferAndroid\":2}"));
		call("/printer/purchase-orders/" + po + "/receipts",
			"{\"serials\":[\"AMR7OKA17050001\",\"AMR7OKA17050002\",\"AMR7OKA17050003\",\"AMR7OKA17050004\"]}");
		call("/printer/requests/" + r1 + "/assign", "{\"serials\":[\"AMR7OKA17050001\",\"AMR7OKA17050002\"]}");
		call("/printer/requests/" + r1 + "/ship", "{\"trackingNo\":\"1\"}");
		call("/printer/requests/" + r1 + "/deliver", "{\"date\":\"2017-05-23\"}");

		// 19. 불량 교환: 새 기기 선배송 → 회수 대기 → 회수
		String ex = id(call("/printer/devices/AMR7OKA17050001/exchange", "{\"date\":\"2017-08-21\"}"));
		assertThat(num(get("/printer/dashboard?asOf=2017-08-25"), "$.unrecoveredExchanges")).isEqualTo(1);
		call("/printer/cases/" + ex + "/recover", "{\"date\":\"2017-09-01\"}");
		assertThat(num(get("/printer/dashboard?asOf=2017-09-02"), "$.unrecoveredExchanges")).isEqualTo(0);

		// 20. AS: 생산 2년 이내 무상, 지나면 유상
		String free = call("/printer/devices/AMR7OKA17050002/repair", "{\"reason\":\"출력 불량\",\"date\":\"2018-01-10\"}");
		assertThat((Boolean) JsonPath.read(free, "$.freeWarranty")).isTrue();
		call("/printer/cases/" + id(free) + "/repair-result", "{\"repaired\":true}");
		String paid = call("/printer/devices/AMR7OKA17050002/repair", "{\"date\":\"2019-06-01\"}");
		assertThat((Boolean) JsonPath.read(paid, "$.freeWarranty")).isFalse();
		call("/printer/cases/" + id(paid) + "/repair-result", "{\"repaired\":true}");

		// 17. 반품: 회수하면 재고로 돌아오고 143,000원 환불 대기
		List<?> swapped = JsonPath.read(get("/printer/cases"), "$[?(@.type=='EXCHANGE')].newSerial");
		String exchangedTo = swapped.get(0).toString();
		String ret = id(call("/printer/devices/" + exchangedTo + "/return", "{\"reason\":\"사용 안 함\"}"));
		call("/printer/cases/" + ret + "/recover", "{}");
		String req = get("/printer/requests?cycleId=" + cycle);
		assertThat(numField(req, r1, "androidQty")).isEqualTo(1);
		assertThat(numField(req, r1, "refundDue") - numField(req, r1, "refundedTotal")).isEqualTo(143000);
		assertThat(num(get("/printer/dashboard"), "$.pendingRefunds")).isEqualTo(143000);

		// 상태에 맞지 않는 동작은 거부
		call("/printer/requests/" + r1 + "/cancel", "{\"reason\":\"늦은 취소\"}", status().isConflict());
	}

	@Test
	void usersCannotSeeOrTouchEachOthersData() throws Exception {
		String shop = id(call("/printer/shops", shopJson("A01", "가람(방)", "SPECIALTY")));
		String cycle = id(call("/printer/cycles", "{\"month\":\"2017-07\"}"));
		String r = id(call("/printer/requests", requestJson(cycle, shop, "정0미", 1, 0)));

		String mine = token;
		token = login();
		assertThat((List<?>) JsonPath.read(get("/printer/requests"), "$")).isEmpty();
		assertThat((List<?>) JsonPath.read(get("/printer/shops"), "$")).isEmpty();
		call("/printer/requests/" + r + "/payment", "{}", status().isNotFound());
		call("/printer/cycles/" + cycle + "/close", "{}", status().isNotFound());
		call("/printer/requests", requestJson(cycle, shop, "끼어들기", 1, 0), status().isNotFound());

		token = mine;
		assertThat(str(get("/printer/requests"), "$[0].status")).isEqualTo("APPLIED");
	}

	@Test
	void demoDataBuildsAConsistentPicture() throws Exception {
		String out = call("/printer/demo", "");
		assertThat(num(out, "$.shops")).isEqualTo(8);
		String dash = get("/printer/dashboard");
		assertThat(num(dash, "$.unrecoveredExchanges")).isEqualTo(1);
		assertThat(num(dash, "$.openRepairs")).isEqualTo(1);
		assertThat(num(dash, "$.receivablesCount")).isEqualTo(1);
		assertThat(num(dash, "$.pendingRefunds")).isGreaterThan(0);
		assertThat((List<?>) JsonPath.read(dash, "$.suggestions")).isNotEmpty();
		call("/printer/demo", "", status().isConflict());
	}

	// ------------------------------------------------------------------ helpers

	private String login() throws Exception {
		String email = "printer-" + UUID.randomUUID() + "@example.com";
		mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
			.content("{\"username\":\"운영자\",\"email\":\"" + email + "\",\"password\":\"password123\"}"))
			.andExpect(status().isOk());
		String body = mvc.perform(post("/auth/signin").contentType(MediaType.APPLICATION_JSON)
			.content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.token");
	}

	private String call(String path, String json) throws Exception {
		return call(path, json, status().isOk());
	}

	private String call(String path, String json, ResultMatcher expected) throws Exception {
		return mvc.perform(post(path).header("Authorization", "Bearer " + token)
			.contentType(MediaType.APPLICATION_JSON).content(json.isEmpty() ? "{}" : json))
			.andExpect(expected).andReturn().getResponse().getContentAsString();
	}

	private String get(String path) throws Exception {
		return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path)
			.header("Authorization", "Bearer " + token))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
	}

	private static String id(String body) {
		return JsonPath.read(body, "$.id");
	}

	private static String str(String body, String path) {
		return JsonPath.read(body, path).toString();
	}

	private static String field(String listBody, String id, String name) {
		List<?> v = JsonPath.read(listBody, "$[?(@.id=='" + id + "')]." + name);
		return v.get(0).toString();
	}

	private static long numField(String listBody, String id, String name) {
		List<?> v = JsonPath.read(listBody, "$[?(@.id=='" + id + "')]." + name);
		return ((Number) v.get(0)).longValue();
	}

	private static long num(String body, String path) {
		return ((Number) JsonPath.read(body, path)).longValue();
	}

	private static String shopJson(String code, String name, String type) {
		return "{\"code\":\"" + code + "\",\"name\":\"" + name + "\",\"type\":\"" + type + "\"}";
	}

	private static String requestJson(String cycle, String shop, String name, int android, int ios) {
		return "{\"cycleId\":\"" + cycle + "\",\"shopId\":\"" + shop + "\",\"applicantName\":\"" + name
			+ "\",\"androidQty\":" + android + ",\"iosQty\":" + ios + "}";
	}
}
