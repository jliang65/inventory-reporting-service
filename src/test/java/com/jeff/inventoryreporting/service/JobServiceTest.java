package com.jeff.inventoryreporting.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jeff.inventoryreporting.dto.ReportRequestDto;
import com.jeff.inventoryreporting.repository.JobRepository;

class JobServiceTest {

	private JobRepository jobRepository;
	private JobService jobService;

	@BeforeEach
	void setUp() {
		jobRepository = mock(JobRepository.class);
		jobService = new JobService(jobRepository);
	}

	@Test
	void save_whenEndDateBeforeStartDate_throwsIllegalArgumentException() {
		ReportRequestDto request = new ReportRequestDto();
		request.setStartDate(LocalDate.of(2024, 2, 1));
		request.setEndDate(LocalDate.of(2024, 1, 1));

		assertThrows(IllegalArgumentException.class, () -> jobService.save(request));
		verifyNoInteractions(jobRepository);
	}
}
