package com.jeff.inventoryreporting.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.jeff.inventoryreporting.client.InventoryApiClient;
import com.jeff.inventoryreporting.entity.Job;
import com.jeff.inventoryreporting.entity.JobType;

class InventoryActivityReportGeneratorTest {

	@TempDir
	Path reportsDirectory;

	private InventoryActivityReportGenerator generator;

	@BeforeEach
	void setUp() {
		InventoryApiClient inventoryApiClient = mock(InventoryApiClient.class);
		when(inventoryApiClient.findTransactions(any(), any(), any()))
				.thenReturn(List.of());
		generator = new InventoryActivityReportGenerator(
				inventoryApiClient,
				reportsDirectory.toString());
	}

	@Test
	void generate_writesASeparateTempFilePerAttempt() {
		Job job = job(42L);
		UUID firstToken = UUID.randomUUID();
		UUID secondToken = UUID.randomUUID();

		String firstReport = generator.generate(job, firstToken);
		String secondReport = generator.generate(job, secondToken);

		assertNotEquals(firstReport, secondReport);
		assertTrue(Files.exists(Path.of(firstReport)));
		assertTrue(Files.exists(Path.of(secondReport)));
		assertTrue(firstReport.contains(firstToken.toString()));
		assertTrue(secondReport.contains(secondToken.toString()));
		assertFalse(Files.exists(publishedPath(42L)));
	}

	@Test
	void publish_movesOnlyTheWinningAttemptOntoTheSharedName() throws Exception {
		Job job = job(42L);
		UUID winner = UUID.randomUUID();
		UUID loser = UUID.randomUUID();

		String winningReport = generator.generate(job, winner);
		String losingReport = generator.generate(job, loser);
		String winningContents = Files.readString(Path.of(winningReport));

		String published = generator.publish(42L, winningReport);

		assertEquals(publishedPath(42L).toString(), published);
		assertEquals(winningContents, Files.readString(publishedPath(42L)));
		assertFalse(Files.exists(Path.of(winningReport)));
		assertTrue(Files.exists(Path.of(losingReport)));
	}

	@Test
	void restore_movesThePublishedFileBackToTheTempPath() throws Exception {
		Job job = job(42L);
		UUID token = UUID.randomUUID();
		String temporaryReport = generator.generate(job, token);

		generator.publish(42L, temporaryReport);
		generator.restore(42L, temporaryReport);

		assertFalse(Files.exists(publishedPath(42L)));
		assertTrue(Files.exists(Path.of(temporaryReport)));
	}

	private Job job(Long id) {
		Job job = new Job(JobType.INVENTORY_ACTIVITY, null, null, null);
		job.setId(id);
		return job;
	}

	private Path publishedPath(Long jobId) {
		return reportsDirectory.resolve("inventory-activity-" + jobId + ".csv");
	}
}
