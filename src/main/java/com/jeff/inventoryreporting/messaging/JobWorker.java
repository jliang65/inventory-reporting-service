package com.jeff.inventoryreporting.messaging;

import java.util.Optional;
import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.jeff.inventoryreporting.config.RabbitMqConfig;
import com.jeff.inventoryreporting.dto.JobMessage;
import com.jeff.inventoryreporting.entity.Job;
import com.jeff.inventoryreporting.service.InventoryActivityReportGenerator;
import com.jeff.inventoryreporting.service.JobService;

@Component
public class JobWorker {

	private final JobService jobService;
	private final InventoryActivityReportGenerator reportGenerator;

	public JobWorker(
			JobService jobService,
			InventoryActivityReportGenerator reportGenerator) {
		this.jobService = jobService;
		this.reportGenerator = reportGenerator;
	}

	@RabbitListener(
			queues = RabbitMqConfig.QUEUE,
			containerFactory = "jobListenerContainerFactory")
	public void process(JobMessage message) {
		Long jobId = message.getJobId();
		Optional<UUID> claim = jobService.tryClaim(jobId);

		if (claim.isEmpty()) {
			return;
		}

		UUID processingToken = claim.get();
		String temporaryReport = null;

		try {
			temporaryReport = generateReport(jobId, processingToken);
			if (!jobService.tryMarkCompleted(
					jobId,
					processingToken,
					temporaryReport)) {
				// This attempt lost the lease because another worker already
				// completed the job. Delete the temp file and return so the
				// message is acked. Throwing here would retry a finished job.
				reportGenerator.delete(temporaryReport);
				return;
			}
		} catch (Exception exception) {
			reportGenerator.delete(temporaryReport);
			jobService.releaseAfterFailure(
					jobId,
					processingToken,
					safeMessage(exception));
			throw exception;
		}

		publishReport(jobId, temporaryReport);
	}

	private String generateReport(Long jobId, UUID processingToken) {
		Job job = jobService.findJob(jobId);
		return reportGenerator.generate(job, processingToken);
	}

	private void publishReport(Long jobId, String temporaryReport) {
		//Uncomment this to create a 15 second delay to see the temporary report file
		//try { Thread.sleep(15_000); } catch (InterruptedException ignored) {}

		String published = null;

		try {
			published = reportGenerator.publish(jobId, temporaryReport);
			jobService.updateResultPath(jobId, published);
		} catch (RuntimeException exception) {
			if (published != null) {
				reportGenerator.restore(jobId, temporaryReport);
			}
			throw exception;
		}
	}

	private String safeMessage(Exception exception) {
		String message = exception.getMessage();
		if (message == null || message.isBlank()) {
			return exception.getClass().getSimpleName();
		}
		return message;
	}
}
