package com.jeff.inventoryreporting.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
