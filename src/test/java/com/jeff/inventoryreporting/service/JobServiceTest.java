package com.jeff.inventoryreporting.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jeff.inventoryreporting.dto.ReportRequestDto;
import com.jeff.inventoryreporting.entity.Job;
import com.jeff.inventoryreporting.messaging.JobPublisher;
import com.jeff.inventoryreporting.repository.JobRepository;

class JobServiceTest {

	private JobRepository jobRepository;
	private JobPublisher jobPublisher;
	private JobService jobService;

	@BeforeEach
	void setUp() {
		jobRepository = mock(JobRepository.class);
		jobPublisher = mock(JobPublisher.class);
		jobService = new JobService(jobRepository, jobPublisher);
	}

	@Test
	void save_whenEndDateBeforeStartDate_throwsIllegalArgumentException() {
		ReportRequestDto request = new ReportRequestDto();
		request.setStartDate(LocalDate.of(2024, 2, 1));
		request.setEndDate(LocalDate.of(2024, 1, 1));

		assertThrows(IllegalArgumentException.class, () -> jobService.save(request));
		verifyNoInteractions(jobRepository);
		verifyNoInteractions(jobPublisher);
	}

	@Test
	void save_whenDatesOmitted_doesNotThrow() {
		stubSaveAssignsId();

		assertDoesNotThrow(() -> jobService.save(new ReportRequestDto()));
	}

	@Test
	void save_whenOnlyStartDate_doesNotThrow() {
		stubSaveAssignsId();

		ReportRequestDto request = new ReportRequestDto();
		request.setStartDate(LocalDate.of(2024, 1, 1));

		assertDoesNotThrow(() -> jobService.save(request));
	}

	@Test
	void save_whenOnlyEndDate_doesNotThrow() {
		stubSaveAssignsId();

		ReportRequestDto request = new ReportRequestDto();
		request.setEndDate(LocalDate.of(2024, 1, 31));

		assertDoesNotThrow(() -> jobService.save(request));
	}

	//Creates a job with an id of 1 so no real database is needed 
	private void stubSaveAssignsId() {
		when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
			Job job = invocation.getArgument(0);
			job.setId(1L);
			return job;
		});
	}
}
