package com.jeff.inventoryreporting.dto;

public record InventoryActivitySummary(
		long totalStockIn,
		long totalStockOut,
		long totalPositiveAdjustments,
		long totalNegativeAdjustments,
		long totalTransfersIn,
		long totalTransfersOut,
		long totalTransactions) {
}
