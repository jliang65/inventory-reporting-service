package com.jeff.inventoryreporting.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.jeff.inventoryreporting.dto.JobResponseDto;
import com.jeff.inventoryreporting.dto.ReportRequestDto;
import com.jeff.inventoryreporting.entity.Job;
import com.jeff.inventoryreporting.entity.JobStatus;
import com.jeff.inventoryreporting.entity.JobType;
import com.jeff.inventoryreporting.messaging.JobPublisher;
import com.jeff.inventoryreporting.repository.JobRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class JobService {

	private final JobRepository jobRepository;
	private final JobPublisher jobPublisher;

	public JobService(JobRepository jobRepository, JobPublisher jobPublisher) {
		this.jobRepository = jobRepository;
		this.jobPublisher = jobPublisher;
	}

	public JobResponseDto findById(Long id) {
		Job job = jobRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Job not found: " + id));
		return toDto(job);
	}

	public Page<JobResponseDto> findAll(JobStatus status, Long locationId, Pageable pageable) {
		return jobRepository.findAll(status, locationId, pageable).map(this::toDto);
	}

	public JobResponseDto save(ReportRequestDto request) {
		if (request.getEndDate().isBefore(request.getStartDate())) {
			throw new IllegalArgumentException("endDate must not be before startDate");
		}

		Job job = new Job(
				JobType.INVENTORY_ACTIVITY,
				request.getStartDate(),
				request.getEndDate(),
				request.getLocationId());

		Job saved = jobRepository.save(job);
		jobPublisher.publish(saved.getId());
		return toDto(saved);
	}

	private JobResponseDto toDto(Job job) {
		JobResponseDto dto = new JobResponseDto();
		dto.setId(job.getId());
		dto.setType(job.getType());
		dto.setStatus(job.getStatus());
		dto.setStartDate(job.getStartDate());
		dto.setEndDate(job.getEndDate());
		dto.setLocationId(job.getLocationId());
		dto.setCreatedAt(job.getCreatedAt());
		dto.setStartedAt(job.getStartedAt());
		dto.setCompletedAt(job.getCompletedAt());
		dto.setResultPath(job.getResultPath());
		dto.setErrorMessage(job.getErrorMessage());
		return dto;
	}
}
