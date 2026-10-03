package io.github.leeyou34.todo.printer;

import io.github.leeyou34.todo.printer.Enums.DeviceModel;
import io.github.leeyou34.todo.printer.Enums.ShopType;

/**
 * 가격표(원). 2017 인수인계 가이드서의 가격표 기준이며, 금액은 모두 공급가액(VAT 별도)입니다.
 * 개인 입금은 VAT 포함 금액(예: 143,000 → 157,300)으로 받습니다.
 *
 * <pre>
 *                 안드로이드                    iOS
 * 방판 특약점     개인 143,000                  개인 143,000 + 본사 30,000
 * 방판 직영       본사 143,000                  본사 173,000
 * 리리코스 지사   개인 54,000 + 본사 89,000      개인 64,000 + 본사 109,000
 * 매입(빅솔론)    130,000                       160,000
 * </pre>
 */
public final class Pricing {

	public static final long SALE_ANDROID = 143_000;
	public static final long SALE_IOS = 173_000;
	public static final long PERSONAL_SHARE = 143_000;
	public static final long COST_ANDROID = 130_000;
	public static final long COST_IOS = 160_000;
	public static final long LIRICOS_PERSONAL_ANDROID = 54_000;
	public static final long LIRICOS_PERSONAL_IOS = 64_000;

	private Pricing() {
	}

	public static long personalAmount(ShopType type, int android, int ios) {
		return switch (type) {
			case DIRECT -> 0;
			case LIRICOS -> android * LIRICOS_PERSONAL_ANDROID + ios * LIRICOS_PERSONAL_IOS;
			default -> (long) (android + ios) * PERSONAL_SHARE;
		};
	}

	public static long hqAmount(ShopType type, int android, int ios) {
		return android * SALE_ANDROID + ios * SALE_IOS - personalAmount(type, android, ios);
	}

	public static long sale(DeviceModel model) {
		return model == DeviceModel.IOS ? SALE_IOS : SALE_ANDROID;
	}

	public static long cost(DeviceModel model) {
		return model == DeviceModel.IOS ? COST_IOS : COST_ANDROID;
	}

	/** 기기 한 대를 돌려받을 때 개인에게 돌려줄 공급가액 */
	public static long personalRefundPerUnit(ShopType type, DeviceModel model) {
		return model == DeviceModel.IOS ? personalAmount(type, 0, 1) : personalAmount(type, 1, 0);
	}

	/** VAT 10% 포함 금액 */
	public static long withVat(long supply) {
		return supply * 11 / 10;
	}

	/** VAT 포함 금액 → 공급가액 */
	public static long withoutVat(long gross) {
		return Math.round(gross / 1.1);
	}
}
