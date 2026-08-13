package com.jeff.inventoryreporting.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI inventoryReportingOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Inventory Reporting API")
						.version("1.0.0")
						.description(
								"REST API for submitting and tracking asynchronous inventory activity report jobs."));
	}
}
