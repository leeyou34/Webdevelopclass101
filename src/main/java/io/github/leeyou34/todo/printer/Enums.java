package io.github.leeyou34.todo.printer;

/**
 * 모바일 프린터 운영관리에서 쓰는 상태값 모음.
 * 이름은 영어, 화면 표시는 화면 쪽에서 한국어로 바꿉니다.
 */
public final class Enums {

	private Enums() {
	}

	/** 기종. 시리얼 앞 7자리로 구분합니다(AMR7OKA 안드로이드, AMR7VKA iOS). */
	public enum DeviceModel {
		ANDROID("AMR7OKA"), IOS("AMR7VKA");

		public final String serialPrefix;

		DeviceModel(String serialPrefix) {
			this.serialPrefix = serialPrefix;
		}

		public static DeviceModel fromSerial(String serial) {
			for (DeviceModel m : values()) {
				if (serial.startsWith(m.serialPrefix)) {
					return m;
				}
			}
			return null;
		}
	}

	/** 영업장 구분. 직영 영업소는 대금 전액을 고객사 본사에 청구합니다. */
	public enum ShopType {
		SPECIALTY, DIRECT
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
