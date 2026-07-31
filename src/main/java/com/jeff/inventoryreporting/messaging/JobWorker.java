package com.jeff.inventoryreporting.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.jeff.inventoryreporting.config.RabbitMqConfig;
import com.jeff.inventoryreporting.dto.JobMessage;
import com.jeff.inventoryreporting.service.JobService;

@Component
public class JobWorker {

	private final JobService jobService;

	public JobWorker(JobService jobService) {
		this.jobService = jobService;
	}

	@RabbitListener(queues = RabbitMqConfig.QUEUE)
	public void process(JobMessage message) {
		if (!jobService.markProcessing(message.getJobId())) {
			return;
		}

		try {
			fakeReportGeneration();
			jobService.markCompleted(
					message.getJobId(),
					"reports/inventory-activity-" + message.getJobId() + ".csv");
		} catch (Exception exception) {
			jobService.markFailed(message.getJobId(), exception.getMessage());
		}
	}

	private void fakeReportGeneration() {
		try {
			Thread.sleep(5_000);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Report generation was interrupted", exception);
		}
	}
}
