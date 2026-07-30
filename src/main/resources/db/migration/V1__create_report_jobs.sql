CREATE TABLE report_jobs (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,

    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    location_id BIGINT,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,

    result_path VARCHAR(500),
    error_message TEXT
);
