package io.github.leeyou34.todo.printer;

import java.time.LocalDate;
import java.util.List;

import io.github.leeyou34.todo.printer.PrinterDtos.ModelCount;

/** 기간 분석과 업무 할 일 응답 형식 */
public final class InsightDtos {

	private InsightDtos() {
	}

	/**
	 * 기간 합계.
	 * 신청·취소는 신청 월(사이클) 기준, 배송·매출·수익은 배송 완료일 기준, 계산서는 발행일 기준,
	 * 수금은 개인 입금일과 본사 수금일 기준, 반품·교환·AS는 접수일 기준입니다.
	 */
	public record PeriodSummary(
		int requests,
		int cancelled,
		ModelCount delivered,
		ModelCount invoiced,
		long revenue,
		long cost,
		long profit,
		long collected,
		int returns,
		int exchanges,
		int repairs) {
	}

	/** 추이 그래프 한 칸 (일 또는 월) */
	public record SeriesPoint(String key, String label, int android, int ios, long revenue, long profit, long collected) {
	}

	public record ShopRow(String shopCode, String shopName, int android, int ios, long revenue) {
	}

	public record Analytics(
		LocalDate from,
		LocalDate to,
		String unit,
		PeriodSummary summary,
		LocalDate previousFrom,
		LocalDate previousTo,
		PeriodSummary previous,
		List<SeriesPoint> series,
		List<ShopRow> byShop) {
	}

	/**
	 * 운영 업무 한 건. priority 1 = 긴급(돈·고객과 직접 관련), 2 = 정기 업무, 3 = 행정·기록.
	 * page/cycleId/focusId는 화면에서 해당 위치로 이동할 때 씁니다.
	 */
	public record OpsTask(
		String key,
		LocalDate dueOn,
		int priority,
		String category,
		String title,
		String detail,
		String page,
		String cycleId,
		String focusId,
		String month) {
	}

	public record OpsTasks(LocalDate asOf, List<OpsTask> tasks) {
	}
}
