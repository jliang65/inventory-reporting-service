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
import com.jeff.inventoryreporting.dto.InventoryActivitySummary;
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

		InventoryActivitySummary summary = calculateSummary(rows);

		try {
			Files.createDirectories(reportsDirectory);

			String filename = "inventory-activity-" + job.getId() + ".csv";
			Path reportPath = reportsDirectory.resolve(filename);

			try (BufferedWriter writer = Files.newBufferedWriter(
					reportPath,
					StandardCharsets.UTF_8)) {
				writeReportDetails(writer, job);
				writer.newLine();

				writeSummary(writer, summary);
				writer.newLine();

				writer.write("Transactions");
				writer.newLine();
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

	private InventoryActivitySummary calculateSummary(
			List<InventoryTransactionReportRow> rows) {

		long totalStockIn = 0;
		long totalStockOut = 0;
		long totalPositiveAdjustments = 0;
		long totalNegativeAdjustments = 0;
		long totalTransfersIn = 0;
		long totalTransfersOut = 0;

		for (InventoryTransactionReportRow row : rows) {
			String type = row.transactionType();
			if (type == null) {
				continue;
			}

			long quantity = Math.abs(quantityValue(row.quantityChange()));

			switch (type) {
				case "STOCK_IN" -> totalStockIn += quantity;
				case "STOCK_OUT" -> totalStockOut += quantity;
				case "TRANSFER_IN" -> totalTransfersIn += quantity;
				case "TRANSFER_OUT" -> totalTransfersOut += quantity;
				case "ADJUSTMENT" -> {
					long adjustment =
							quantityValue(row.newQuantity())
									- quantityValue(row.previousQuantity());
					if (adjustment > 0) {
						totalPositiveAdjustments += adjustment;
					} else if (adjustment < 0) {
						totalNegativeAdjustments += Math.abs(adjustment);
					}
				}
				default -> {
				}
			}
		}

		return new InventoryActivitySummary(
				totalStockIn,
				totalStockOut,
				totalPositiveAdjustments,
				totalNegativeAdjustments,
				totalTransfersIn,
				totalTransfersOut,
				rows.size());
	}

	private void writeReportDetails(
			BufferedWriter writer,
			Job job) throws IOException {

		writer.write("Inventory Activity Report");
		writer.newLine();
		writer.write(
				"Start Date,"
						+ escape(job.getStartDate().toString()));
		writer.newLine();
		writer.write(
				"End Date,"
						+ escape(job.getEndDate().toString()));
		writer.newLine();

		String locationValue =
				job.getLocationId() == null
						? "All Locations"
						: job.getLocationId().toString();
		writer.write(
				"Location,"
						+ escape(locationValue));
		writer.newLine();
	}

	private void writeSummary(
			BufferedWriter writer,
			InventoryActivitySummary summary) throws IOException {

		writer.write("Summary");
		writer.newLine();
		writer.write("Metric,Value");
		writer.newLine();
		writeMetric(writer, "Total Stock In", summary.totalStockIn());
		writeMetric(writer, "Total Stock Out", summary.totalStockOut());
		writeMetric(
				writer,
				"Positive Adjustments",
				summary.totalPositiveAdjustments());
		writeMetric(
				writer,
				"Negative Adjustments",
				summary.totalNegativeAdjustments());
		writeMetric(writer, "Total Transfers In", summary.totalTransfersIn());
		writeMetric(writer, "Total Transfers Out", summary.totalTransfersOut());
		writeMetric(writer, "Total Transactions", summary.totalTransactions());
	}

	private void writeMetric(
			BufferedWriter writer,
			String name,
			long value) throws IOException {

		writer.write(escape(name) + "," + value);
		writer.newLine();
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

	private long quantityValue(Integer value) {
		return value == null ? 0 : value;
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
