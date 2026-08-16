package com.jeff.inventoryreporting.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jeff.inventoryreporting.client.InventoryApiClient;
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
	private final InventoryApiClient inventoryApiClient;

	public JobService(
			JobRepository jobRepository,
			JobPublisher jobPublisher,
			InventoryApiClient inventoryApiClient) {
		this.jobRepository = jobRepository;
		this.jobPublisher = jobPublisher;
		this.inventoryApiClient = inventoryApiClient;
	}

	public JobResponseDto findById(Long id) {
		return toDto(findJob(id));
	}

	public Page<JobResponseDto> findAll(JobStatus status, Long locationId, Pageable pageable) {
		return jobRepository.findAll(status, locationId, pageable).map(this::toDto);
	}

	public JobResponseDto save(ReportRequestDto request) {
		if (request.getStartDate() != null
				&& request.getEndDate() != null
				&& request.getEndDate().isBefore(request.getStartDate())) {
			throw new IllegalArgumentException("endDate must not be before startDate");
		}

		if (request.getLocationId() != null) {
			inventoryApiClient.checkLocationId(request.getLocationId());
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

	@Transactional
	public Optional<UUID> tryClaim(Long jobId) {
		UUID token = UUID.randomUUID();
		int updatedRows = jobRepository.claimJob(jobId, token);

		if (updatedRows == 0) {
			return Optional.empty();
		}

		return Optional.of(token);
	}

	@Transactional
	public void markFailedAfterRetries(Long jobId) {
		Job job = findJob(jobId);

		if (job.getStatus() == JobStatus.COMPLETED) {
			return;
		}

		job.setStatus(JobStatus.FAILED);
		job.setCompletedAt(Instant.now());
		job.setProcessingToken(null);
		job.setLeaseExpiresAt(null);

		String finalError = job.getLastError();

		if (finalError == null || finalError.isBlank()) {
			finalError = "Report generation failed after all retry attempts";
		}

		job.setErrorMessage(finalError);
	}

	@Transactional
	public void markCompleted(
			Long jobId,
			UUID processingToken,
			String resultPath) {
		int updatedRows = jobRepository.completeClaimedJob(
				jobId,
				processingToken,
				resultPath);

		if (updatedRows == 0) {
			throw new IllegalStateException(
					"Could not complete job because the processing lease "
							+ "is no longer owned: " + jobId);
		}
	}

	@Transactional
	public void releaseAfterFailure(
			Long jobId,
			UUID processingToken,
			String errorMessage) {
		int updatedRows = jobRepository.releaseClaimAfterFailure(
				jobId,
				processingToken,
				errorMessage);

		if (updatedRows == 0) {
			throw new IllegalStateException(
					"Could not release processing lease for job: " + jobId);
		}
	}

	public Job findJob(Long jobId) {
		return jobRepository.findById(jobId)
				.orElseThrow(() -> new EntityNotFoundException("Job not found: " + jobId));
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
		dto.setAttemptCount(job.getAttemptCount());
		dto.setLastError(job.getLastError());
		return dto;
	}
}
