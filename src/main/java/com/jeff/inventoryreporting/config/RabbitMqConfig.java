package com.jeff.inventoryreporting.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

	public static final String EXCHANGE = "inventory.reporting";
	public static final String QUEUE = "inventory.reporting.jobs";
	public static final String ROUTING_KEY = "inventory.reporting.job";

	@Bean
	public MessageConverter messageConverter() {
		return new JacksonJsonMessageConverter();
	}

	@Bean
	public DirectExchange inventoryReportingExchange() {
		return new DirectExchange(EXCHANGE);
	}

	@Bean
	public Queue inventoryReportingJobsQueue() {
		return new Queue(QUEUE);
	}

	@Bean
	public Binding inventoryReportingJobsBinding(
			Queue inventoryReportingJobsQueue,
			DirectExchange inventoryReportingExchange) {
		return BindingBuilder
				.bind(inventoryReportingJobsQueue)
				.to(inventoryReportingExchange)
				.with(ROUTING_KEY);
	}
}
