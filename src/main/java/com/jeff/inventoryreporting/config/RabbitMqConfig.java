package com.jeff.inventoryreporting.config;

import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.StatelessRetryOperationsInterceptor;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

	public static final String EXCHANGE = "inventory.reporting";
	public static final String QUEUE = "inventory.reporting.jobs";
	public static final String ROUTING_KEY = "inventory.reporting.job";

	public static final String DEAD_LETTER_EXCHANGE =
			"inventory.reporting.dead-letter";
	public static final String DEAD_LETTER_QUEUE =
			"inventory.reporting.jobs.dead-letter";
	public static final String DEAD_LETTER_ROUTING_KEY =
			"inventory.reporting.job.dead-letter";

	/** Total listener invocations before giving up (initial attempt + retries). */
	public static final int MAX_JOB_ATTEMPTS = 3;

	@Bean
	public MessageConverter messageConverter() {
		return new JacksonJsonMessageConverter();
	}

	@Bean
	public StatelessRetryOperationsInterceptor jobRetryInterceptor() {
		return RetryInterceptorBuilder.stateless()
				// Spring RetryPolicy: total attempts = 1 + maxRetries
				.maxRetries(MAX_JOB_ATTEMPTS - 1)
				.backOffOptions(
						2_000L,
						2.0,
						10_000L)
				.recoverer(new RejectAndDontRequeueRecoverer())
				.build();
	}

	@Bean
	public SimpleRabbitListenerContainerFactory jobListenerContainerFactory(
			SimpleRabbitListenerContainerFactoryConfigurer configurer,
			ConnectionFactory connectionFactory,
			StatelessRetryOperationsInterceptor jobRetryInterceptor) {

		SimpleRabbitListenerContainerFactory factory =
				new SimpleRabbitListenerContainerFactory();

		configurer.configure(factory, connectionFactory);
		factory.setAdviceChain(jobRetryInterceptor);
		factory.setDefaultRequeueRejected(false);

		return factory;
	}

	@Bean
	public DirectExchange inventoryReportingExchange() {
		return new DirectExchange(EXCHANGE);
	}

	@Bean
	public DirectExchange inventoryReportingDeadLetterExchange() {
		return new DirectExchange(DEAD_LETTER_EXCHANGE);
	}

	@Bean
	public Queue inventoryReportingJobsQueue() {
		return QueueBuilder.durable(QUEUE)
				.withArguments(Map.of(
						"x-dead-letter-exchange", DEAD_LETTER_EXCHANGE,
						"x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY))
				.build();
	}

	@Bean
	public Queue inventoryReportingJobsDeadLetterQueue() {
		return new Queue(DEAD_LETTER_QUEUE);
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

	@Bean
	public Binding inventoryReportingJobsDeadLetterBinding(
			Queue inventoryReportingJobsDeadLetterQueue,
			DirectExchange inventoryReportingDeadLetterExchange) {
		return BindingBuilder
				.bind(inventoryReportingJobsDeadLetterQueue)
				.to(inventoryReportingDeadLetterExchange)
				.with(DEAD_LETTER_ROUTING_KEY);
	}
}
