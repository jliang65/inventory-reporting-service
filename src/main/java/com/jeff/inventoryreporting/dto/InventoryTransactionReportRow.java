package com.jeff.inventoryreporting.dto;

import java.time.LocalDateTime;

public record InventoryTransactionReportRow(
		Long id,
		Long productId,
		String productSku,
		String productName,
		Long locationId,
		String locationName,
		String transactionType,
		Integer quantityChange,
		Integer previousQuantity,
		Integer newQuantity,
		Long relatedTransactionId,
		String reason,
		Long performedByUserId,
		String performedByEmail,
		LocalDateTime createdAt) {
}
