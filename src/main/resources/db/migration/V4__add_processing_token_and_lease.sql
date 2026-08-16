ALTER TABLE report_jobs
    ADD COLUMN processing_token UUID,
    ADD COLUMN lease_expires_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_report_jobs_lease_expires_at
    ON report_jobs (lease_expires_at);
