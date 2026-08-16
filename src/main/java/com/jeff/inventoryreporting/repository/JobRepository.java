package com.jeff.inventoryreporting.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jeff.inventoryreporting.entity.Job;
import com.jeff.inventoryreporting.entity.JobStatus;

public interface JobRepository extends JpaRepository<Job, Long> {

	@Query("""
			SELECT j FROM Job j
			WHERE (:status IS NULL OR j.status = :status)
			AND (:locationId IS NULL OR j.locationId = :locationId)
			""")
	Page<Job> findAll(
			@Param("status") JobStatus status,
			@Param("locationId") Long locationId,
			Pageable pageable);

	@Modifying
	@Query(value = """
			UPDATE report_jobs
			SET status = 'PROCESSING',
			    processing_token = :token,
			    lease_expires_at = CURRENT_TIMESTAMP + INTERVAL '5 minutes',
			    started_at = COALESCE(started_at, CURRENT_TIMESTAMP),
			    attempt_count = attempt_count + 1
			WHERE id = :jobId
			  AND status NOT IN ('COMPLETED', 'FAILED')
			  AND (
			      processing_token IS NULL
			      OR lease_expires_at IS NULL
			      OR lease_expires_at < CURRENT_TIMESTAMP
			  )
			""", nativeQuery = true)
	int claimJob(
			@Param("jobId") Long jobId,
			@Param("token") UUID token);

	@Modifying
	@Query(value = """
			UPDATE report_jobs
			SET status = 'COMPLETED',
			    completed_at = CURRENT_TIMESTAMP,
			    result_path = :resultPath,
			    error_message = NULL,
			    processing_token = NULL,
			    lease_expires_at = NULL
			WHERE id = :jobId
			  AND processing_token = :token
			  AND status = 'PROCESSING'
			""", nativeQuery = true)
	int completeClaimedJob(
			@Param("jobId") Long jobId,
			@Param("token") UUID token,
			@Param("resultPath") String resultPath);

	@Modifying
	@Query(value = """
			UPDATE report_jobs
			SET last_error = :errorMessage,
			    processing_token = NULL,
			    lease_expires_at = NULL
			WHERE id = :jobId
			  AND processing_token = :token
			  AND status = 'PROCESSING'
			""", nativeQuery = true)
	int releaseClaimAfterFailure(
			@Param("jobId") Long jobId,
			@Param("token") UUID token,
			@Param("errorMessage") String errorMessage);
}
