package com.jeff.inventoryreporting.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jeff.inventoryreporting.dto.JobResponseDto;
import com.jeff.inventoryreporting.dto.ReportRequestDto;
import com.jeff.inventoryreporting.entity.JobStatus;
import com.jeff.inventoryreporting.service.JobService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

	private final JobService jobService;

	public JobController(JobService jobService) {
		this.jobService = jobService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public JobResponseDto create(@Valid @RequestBody ReportRequestDto request) {
		return jobService.save(request);
	}

	@GetMapping
	public Page<JobResponseDto> findJobs(
			@RequestParam(required = false) JobStatus status,
			@RequestParam(required = false) Long locationId,
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return jobService.findAll(status, locationId, pageable);
	}

	@GetMapping("/{id}")
	public JobResponseDto getById(@PathVariable Long id) {
		return jobService.findById(id);
	}
}
