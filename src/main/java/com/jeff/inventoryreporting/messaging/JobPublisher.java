package com.jeff.inventoryreporting.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.jeff.inventoryreporting.config.RabbitMqConfig;
import com.jeff.inventoryreporting.dto.JobMessage;

@Component
public class JobPublisher {

	private final RabbitTemplate rabbitTemplate;

	public JobPublisher(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	public void publish(Long jobId) {
		rabbitTemplate.convertAndSend(
				RabbitMqConfig.EXCHANGE,
				RabbitMqConfig.ROUTING_KEY,
				new JobMessage(jobId));
	}
}
