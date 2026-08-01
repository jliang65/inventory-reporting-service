package com.jeff.inventoryreporting.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.jeff.inventoryreporting.config.RabbitMqConfig;
import com.jeff.inventoryreporting.dto.JobMessage;
import com.jeff.inventoryreporting.service.JobService;

@Component
public class DeadLetterWorker {

	private final JobService jobService;

	public DeadLetterWorker(JobService jobService) {
		this.jobService = jobService;
	}

	@RabbitListener(queues = RabbitMqConfig.DEAD_LETTER_QUEUE)
	public void processDeadLetter(JobMessage message) {
		jobService.markFailedAfterRetries(message.getJobId());
	}
}
