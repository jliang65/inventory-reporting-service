package com.jeff.inventoryreporting.messaging;

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
		jobService.beginAttempt(message.getJobId());

		try {
			String resultPath = generateReport(message.getJobId());
			jobService.markCompleted(message.getJobId(), resultPath);
		} catch (Exception exception) {
			jobService.recordAttemptFailure(
					message.getJobId(),
					safeMessage(exception));
			throw exception;
		}
	}

	private String generateReport(Long jobId) {
		Job job = jobService.findJob(jobId);
		return reportGenerator.generate(job);
	}

	private String safeMessage(Exception exception) {
		String message = exception.getMessage();
		if (message == null || message.isBlank()) {
			return exception.getClass().getSimpleName();
		}
		return message;
	}
}
