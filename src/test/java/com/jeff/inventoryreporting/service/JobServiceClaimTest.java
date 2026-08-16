package com.jeff.inventoryreporting.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.jeff.inventoryreporting.entity.Job;
import com.jeff.inventoryreporting.entity.JobStatus;
import com.jeff.inventoryreporting.entity.JobType;
import com.jeff.inventoryreporting.repository.JobRepository;

@SpringBootTest
class JobServiceClaimTest {

	@Autowired
	private JobService jobService;

	@Autowired
	private JobRepository jobRepository;

	private Long jobId;

	@BeforeEach
	void setUp() {
		Job job = new Job(JobType.INVENTORY_ACTIVITY, null, null, null);
		jobId = jobRepository.save(job).getId();
	}

	@AfterEach
	void tearDown() {
		if (jobId != null) {
			jobRepository.deleteById(jobId);
		}
	}

	@Test
	void tryClaim_whenJobIsUnclaimed_returnsToken() {
		Optional<UUID> token = jobService.tryClaim(jobId);

		assertTrue(token.isPresent());

		Job claimed = jobRepository.findById(jobId).orElseThrow();

		assertEquals(JobStatus.PROCESSING, claimed.getStatus());
		assertEquals(token.get(), claimed.getProcessingToken());
		assertNotNull(claimed.getLeaseExpiresAt());
		assertEquals(1, claimed.getAttemptCount());
	}

	@Test
	void tryClaim_whenJobHasActiveLease_returnsEmpty() {
		Optional<UUID> firstClaim = jobService.tryClaim(jobId);
		Optional<UUID> secondClaim = jobService.tryClaim(jobId);

		assertTrue(firstClaim.isPresent());
		assertTrue(secondClaim.isEmpty());
		assertEquals(firstClaim.get(), jobRepository.findById(jobId).orElseThrow().getProcessingToken());
	}

	@Test
	void tryClaim_whenCalledConcurrently_onlyOneSucceeds() throws Exception {
		int threadCount = 8;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Optional<UUID>>> results = new ArrayList<>();

		for (int i = 0; i < threadCount; i++) {
			results.add(executor.submit(() -> {
				start.await();
				return jobService.tryClaim(jobId);
			}));
		}

		start.countDown();
		executor.shutdown();
		assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

		long successfulClaims = results.stream()
				.map(this::join)
				.filter(Optional::isPresent)
				.count();

		assertEquals(1, successfulClaims);
		assertEquals(1, jobRepository.findById(jobId).orElseThrow().getAttemptCount());
	}

	@Test
	void releaseAfterFailure_allowsAnotherClaim() {
		UUID firstToken = jobService.tryClaim(jobId).orElseThrow();

		jobService.releaseAfterFailure(
				jobId,
				firstToken,
				"Temporary failure");

		Optional<UUID> secondClaim = jobService.tryClaim(jobId);

		assertTrue(secondClaim.isPresent());
		assertNotEquals(firstToken, secondClaim.get());
	}

	@Test
	void markCompleted_withWrongToken_throwsException() {
		jobService.tryClaim(jobId).orElseThrow();

		UUID wrongToken = UUID.randomUUID();

		assertThrows(
				IllegalStateException.class,
				() -> jobService.markCompleted(
						jobId,
						wrongToken,
						"reports/test.csv"));
	}

	@Test
	void markCompleted_withCorrectToken_completesJob() {
		UUID token = jobService.tryClaim(jobId).orElseThrow();

		jobService.markCompleted(
				jobId,
				token,
				"reports/test.csv");

		Job completed = jobRepository.findById(jobId).orElseThrow();

		assertEquals(JobStatus.COMPLETED, completed.getStatus());
		assertNull(completed.getProcessingToken());
		assertNull(completed.getLeaseExpiresAt());
		assertEquals("reports/test.csv", completed.getResultPath());
	}

	private Optional<UUID> join(Future<Optional<UUID>> future) {
		try {
			return future.get(5, TimeUnit.SECONDS);
		} catch (Exception exception) {
			throw new IllegalStateException(exception);
		}
	}
}
