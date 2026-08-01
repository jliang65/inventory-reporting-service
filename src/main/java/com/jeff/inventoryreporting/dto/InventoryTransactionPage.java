package com.jeff.inventoryreporting.dto;

import java.util.List;

public record InventoryTransactionPage(
		List<InventoryTransactionReportRow> content,
		int number,
		int totalPages,
		long totalElements,
		boolean last) {
}
