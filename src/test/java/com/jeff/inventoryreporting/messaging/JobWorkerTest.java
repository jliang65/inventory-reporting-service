package com.jeff.inventoryreporting.messaging;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jeff.inventoryreporting.dto.JobMessage;
import com.jeff.inventoryreporting.entity.Job;
import com.jeff.inventoryreporting.entity.JobType;
import com.jeff.inventoryreporting.service.InventoryActivityReportGenerator;
import com.jeff.inventoryreporting.service.JobService;

@ExtendWith(MockitoExtension.class)
class JobWorkerTest {

	@Mock
	private JobService jobService;

	@Mock
	private InventoryActivityReportGenerator reportGenerator;

	private JobWorker worker;
	private Job job;
	private UUID token;

	@BeforeEach
	void setUp() {
		worker = new JobWorker(jobService, reportGenerator);
		job = new Job(JobType.INVENTORY_ACTIVITY, null, null, null);
		job.setId(7L);
		token = UUID.randomUUID();
	}

	@Test
	void process_whenLeaseIsLost_deletesTheTempFileAndDoesNotPublish() {
		when(jobService.tryClaim(7L)).thenReturn(Optional.of(token));
		when(jobService.findJob(7L)).thenReturn(job);
		when(reportGenerator.generate(job, token)).thenReturn("reports/attempt.csv.tmp");
		when(jobService.tryMarkCompleted(7L, token, "reports/attempt.csv.tmp"))
				.thenReturn(false);

		worker.process(new JobMessage(7L));

		verify(reportGenerator).delete("reports/attempt.csv.tmp");
		verify(reportGenerator, never()).publish(7L, "reports/attempt.csv.tmp");
		verify(jobService, never()).releaseAfterFailure(
				Mockito.any(),
				Mockito.any(),
				Mockito.any());
		verify(jobService, never()).updateResultPath(Mockito.any(), Mockito.any());
	}

	@Test
	void process_whenLeaseIsHeld_publishesAfterTheJobIsCompleted() {
		when(jobService.tryClaim(7L)).thenReturn(Optional.of(token));
		when(jobService.findJob(7L)).thenReturn(job);
		when(reportGenerator.generate(job, token)).thenReturn("reports/attempt.csv.tmp");
		when(jobService.tryMarkCompleted(7L, token, "reports/attempt.csv.tmp"))
				.thenReturn(true);
		when(reportGenerator.publish(7L, "reports/attempt.csv.tmp"))
				.thenReturn("reports/inventory-activity-7.csv");

		worker.process(new JobMessage(7L));

		InOrder order = Mockito.inOrder(jobService, reportGenerator);
		order.verify(jobService).tryMarkCompleted(7L, token, "reports/attempt.csv.tmp");
		order.verify(reportGenerator).publish(7L, "reports/attempt.csv.tmp");
		order.verify(jobService).updateResultPath(7L, "reports/inventory-activity-7.csv");
		verify(reportGenerator, never()).delete(Mockito.any());
	}

	@Test
	void process_whenPublishingThePathFails_restoresTheTempFile() {
		when(jobService.tryClaim(7L)).thenReturn(Optional.of(token));
		when(jobService.findJob(7L)).thenReturn(job);
		when(reportGenerator.generate(job, token)).thenReturn("reports/attempt.csv.tmp");
		when(jobService.tryMarkCompleted(7L, token, "reports/attempt.csv.tmp"))
				.thenReturn(true);
		when(reportGenerator.publish(7L, "reports/attempt.csv.tmp"))
				.thenReturn("reports/inventory-activity-7.csv");
		Mockito.doThrow(new IllegalStateException("database unavailable"))
				.when(jobService)
				.updateResultPath(7L, "reports/inventory-activity-7.csv");

		assertThrows(
				IllegalStateException.class,
				() -> worker.process(new JobMessage(7L)));

		verify(reportGenerator).restore(7L, "reports/attempt.csv.tmp");
		verify(jobService, never()).releaseAfterFailure(
				Mockito.any(),
				Mockito.any(),
				Mockito.any());
	}
}
