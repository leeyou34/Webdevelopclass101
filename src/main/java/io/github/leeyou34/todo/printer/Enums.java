package io.github.leeyou34.todo.printer;

/**
 * 모바일 프린터 운영관리에서 쓰는 상태값 모음.
 * 이름은 영어, 화면 표시는 화면 쪽에서 한국어로 바꿉니다.
 */
public final class Enums {

	private Enums() {
	}

	/**
	 * 기종. 시리얼 앞 7자리로 구분합니다.
	 * 안드로이드 SPP-R210BKM: AMR7OKA (초기 라벨은 AMR70KA로 읽히는 경우가 있어 함께 인정)
	 * iOS SPP-R210iM: AMR7VKA (2016년 상반기 출고분은 AMB7VKA)
	 */
	public enum DeviceModel {
		ANDROID("AMR7OKA", "SPP-R210BKM/AMR (안드로이드용)", "AMR70KA"),
		IOS("AMR7VKA", "SPP-R210iM/AMR (아이폰용)", "AMB7VKA");

		public final String serialPrefix;
		public final String productName;
		private final String[] otherPrefixes;

		DeviceModel(String serialPrefix, String productName, String... otherPrefixes) {
			this.serialPrefix = serialPrefix;
			this.productName = productName;
			this.otherPrefixes = otherPrefixes;
		}

		public static DeviceModel fromSerial(String serial) {
			for (DeviceModel m : values()) {
				if (serial.startsWith(m.serialPrefix)) {
					return m;
				}
				for (String p : m.otherPrefixes) {
					if (serial.startsWith(p)) {
						return m;
					}
				}
			}
			return null;
		}
	}

	/**
	 * 영업장 구분.
	 * SPECIALTY 방판 특약점(개인 입금 + iOS 차액 본사), DIRECT 방판 직영 영업소(전액 본사),
	 * LIRICOS 리리코스 지사(개인·본사 분담 비율이 다름)
	 */
	public enum ShopType {
		SPECIALTY, DIRECT, LIRICOS
	}

	/** 신청 기간 상태 */
	public enum CycleStatus {
		OPEN, CLOSED
	}

	/** 신청 건 상태 */
	public enum RequestStatus {
		APPLIED, PAID, CONFIRMED, ORDERED, SHIPPING, RETURNED_TO_SENDER, DELIVERED, INVOICED, COMPLETED,
		CANCELLED_UNPAID, CANCELLED, REFUND_PENDING, REFUNDED
	}

	/** 기기 상태 */
	public enum DeviceStatus {
		IN_STOCK, ASSIGNED, SHIPPED, DELIVERED, AWAITING_RECOVERY, RECOVERED, IN_REPAIR, SCRAPPED
	}

	/** 사후 처리 종류: 반품, 불량 교환, AS 수리 */
	public enum CaseType {
		RETURN, EXCHANGE, REPAIR
	}

	public enum CaseStatus {
		OPEN, CLOSED, UNREPAIRABLE
	}

	/** 계산서 종류: 개인(카운슬러) 앞, 본사 앞 */
	public enum InvoiceType {
		PERSONAL, HQ
	}

	public enum InvoiceStatus {
		REQUESTED, ISSUED, PAID
	}
}
