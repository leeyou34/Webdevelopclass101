package io.github.leeyou34.todo.printer;

import io.github.leeyou34.todo.printer.Enums.DeviceModel;
import io.github.leeyou34.todo.printer.Enums.ShopType;

/**
 * 가격표(원, VAT 미포함). 2017 인수인계 가이드서의 방문판매팀 기준.
 * - 특약점: 안드로이드 개인 143,000 / iOS 개인 143,000 + 본사 30,000
 * - 직영 영업소: 전액 본사 앞(안드로이드 143,000, iOS 173,000)
 * - 매입 단가: 안드로이드 130,000, iOS 160,000
 */
public final class Pricing {

	public static final long SALE_ANDROID = 143_000;
	public static final long SALE_IOS = 173_000;
	public static final long PERSONAL_SHARE = 143_000;
	public static final long COST_ANDROID = 130_000;
	public static final long COST_IOS = 160_000;

	private Pricing() {
	}

	public static long personalAmount(ShopType type, int android, int ios) {
		return type == ShopType.DIRECT ? 0 : (long) (android + ios) * PERSONAL_SHARE;
	}

	public static long hqAmount(ShopType type, int android, int ios) {
		if (type == ShopType.DIRECT) {
			return android * SALE_ANDROID + ios * SALE_IOS;
		}
		return ios * (SALE_IOS - PERSONAL_SHARE);
	}

	public static long sale(DeviceModel model) {
		return model == DeviceModel.IOS ? SALE_IOS : SALE_ANDROID;
	}

	public static long cost(DeviceModel model) {
		return model == DeviceModel.IOS ? COST_IOS : COST_ANDROID;
	}

	/** 기기 한 대를 돌려받을 때 개인에게 돌려줄 금액 */
	public static long personalRefundPerUnit(ShopType type) {
		return type == ShopType.DIRECT ? 0 : PERSONAL_SHARE;
	}
}
