package com.jeff.inventoryreporting.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.jeff.inventoryreporting.client.InventoryApiClient;
import com.jeff.inventoryreporting.dto.InventoryTransactionReportRow;
import com.jeff.inventoryreporting.entity.Job;

@Service
public class InventoryActivityReportGenerator {

	private static final String HEADER =
			"product,sku,location,transaction_type,quantity,"
			+ "previous_quantity,new_quantity,performed_by,reason,created_at";

	private final InventoryApiClient inventoryApiClient;
	private final Path reportsDirectory;

	public InventoryActivityReportGenerator(
			InventoryApiClient inventoryApiClient,
			@Value("${reports.directory}") String reportsDirectory) {
		this.inventoryApiClient = inventoryApiClient;
		this.reportsDirectory = Path.of(reportsDirectory);
	}

	public String generate(Job job) {
		List<InventoryTransactionReportRow> rows =
				inventoryApiClient.findTransactions(
						job.getStartDate(),
						job.getEndDate(),
						job.getLocationId());

		try {
			Files.createDirectories(reportsDirectory);

			String filename = "inventory-activity-" + job.getId() + ".csv";
			Path reportPath = reportsDirectory.resolve(filename);

			try (BufferedWriter writer = Files.newBufferedWriter(
					reportPath,
					StandardCharsets.UTF_8)) {
				writer.write(HEADER);
				writer.newLine();

				for (InventoryTransactionReportRow row : rows) {
					writer.write(toCsvRow(row));
					writer.newLine();
				}
			}

			return reportPath.toString();
		} catch (IOException exception) {
			throw new IllegalStateException(
					"Could not generate inventory activity report",
					exception);
		}
	}

	private String toCsvRow(InventoryTransactionReportRow row) {
		return String.join(",",
				escape(row.productName()),
				escape(row.productSku()),
				escape(row.locationName()),
				escape(row.transactionType()),
				String.valueOf(row.quantityChange()),
				String.valueOf(row.previousQuantity()),
				String.valueOf(row.newQuantity()),
				escape(row.performedByEmail()),
				escape(row.reason()),
				escape(row.createdAt() == null ? null : row.createdAt().toString()));
	}

	private String escape(String value) {
		if (value == null) {
			return "";
		}

		String escaped = value.replace("\"", "\"\"");
		if (escaped.contains(",")
				|| escaped.contains("\"")
				|| escaped.contains("\n")
				|| escaped.contains("\r")) {
			return "\"" + escaped + "\"";
		}

		return escaped;
	}
}
