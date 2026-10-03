package io.github.leeyou34.todo.printer;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import io.github.leeyou34.todo.printer.Enums.ShopType;

/** 영업장 수정·폐쇄, 입금 대조, 시리얼 조회, 챗봇 요청·응답 형식 */
public final class ExtraDtos {

	private ExtraDtos() {
	}

	public record ShopUpdateInput(String name, String division, String team, ShopType type, String managerName,
		String phone, String address) {
	}

	public record CloseShopInput(LocalDate date, String note) {
	}

	/** 은행 엑셀에서 복사한 여러 줄(날짜, 입금자, 금액). 날짜가 없는 줄은 defaultDate */
	public record DepositImportInput(String text, LocalDate defaultDate) {
	}

	public record DepositMatchInput(UUID requestId) {
	}

	public record DepositImportResult(int created, int matched, int unmatched, List<Deposit> deposits) {
	}

	public record DeviceLookup(Device device, Shop shop, OrderRequest request, PurchaseOrder purchaseOrder,
		List<ServiceCase> cases) {
	}

	public record ChatInput(String message) {
	}

	public record ChatLink(String label, String path) {
	}

	public record ChatReply(String answer, List<ChatLink> links, List<String> suggestions) {
	}
}
