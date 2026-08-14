package com.jeff.inventoryreporting.client;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.jeff.inventoryreporting.dto.InventoryTransactionPage;
import com.jeff.inventoryreporting.dto.InventoryTransactionReportRow;

@Component
public class InventoryApiClient {

	private final RestClient restClient;

	public InventoryApiClient(
			RestClient.Builder builder,
			@Value("${inventory.api.base-url}") String baseUrl,
			@Value("${inventory.api.token}") String token) {
		this.restClient = builder
				.baseUrl(baseUrl)
				.defaultHeader(
						HttpHeaders.AUTHORIZATION,
						"Bearer " + token)
				.build();
	}

	public void checkLocationId(Long locationId) {
		try {
			restClient.get()
					.uri("/api/locations/{id}", locationId)
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientResponseException exception) {
			if (exception.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
				throw new IllegalArgumentException("Location not found");
			}
			throw exception;
		}
	}

	public List<InventoryTransactionReportRow> findTransactions(
			LocalDate startDate,
			LocalDate endDate,
			Long locationId) {

		List<InventoryTransactionReportRow> allRows = new ArrayList<>();
		int page = 0;
		boolean last;

		do {
			InventoryTransactionPage response = fetchPage(
					startDate,
					endDate,
					locationId,
					page);

			if (response.content() != null) {
				allRows.addAll(response.content());
			}
			last = response.last();
			page++;
		} while (!last);

		return allRows;
	}

	private InventoryTransactionPage fetchPage(
			LocalDate startDate,
			LocalDate endDate,
			Long locationId,
			int pageNumber) {

		InventoryTransactionPage response = restClient.get()
				.uri(uriBuilder -> {
					uriBuilder
							.path("/api/inventory/transactions")
							.queryParam("page", pageNumber)
							.queryParam("size", 100)
							.queryParam("sort", "createdAt,asc");

					if (startDate != null) {
						uriBuilder.queryParam("startDate", startDate);
					}

					if (endDate != null) {
						uriBuilder.queryParam("endDate", endDate);
					}

					if (locationId != null) {
						uriBuilder.queryParam("locationId", locationId);
					}

					return uriBuilder.build();
				})
				.retrieve()
				.body(InventoryTransactionPage.class);

		if (response == null) {
			throw new IllegalStateException(
					"Inventory API returned an empty response");
		}

		return response;
	}
}
