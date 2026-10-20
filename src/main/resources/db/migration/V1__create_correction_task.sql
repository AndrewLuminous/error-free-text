CREATE TABLE correction_task (
    id UUID PRIMARY KEY,
    original_text TEXT NOT NULL,
    language VARCHAR(2) NOT NULL CHECK (language IN ('RU', 'EN')),
    status VARCHAR(16) NOT NULL CHECK (status IN ('NEW', 'PROCESSING', 'DONE', 'FAILED')),
    corrected_text TEXT,
    error_code INTEGER,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    CONSTRAINT task_result_matches_status CHECK (
        (status = 'NEW' AND started_at IS NULL AND finished_at IS NULL
            AND corrected_text IS NULL AND error_code IS NULL AND error_message IS NULL)
        OR (status = 'PROCESSING' AND started_at IS NOT NULL AND finished_at IS NULL
            AND corrected_text IS NULL AND error_code IS NULL AND error_message IS NULL)
        OR (status = 'DONE' AND started_at IS NOT NULL AND finished_at IS NOT NULL
            AND corrected_text IS NOT NULL AND error_code IS NULL AND error_message IS NULL)
        OR (status = 'FAILED' AND started_at IS NOT NULL AND finished_at IS NOT NULL
            AND corrected_text IS NULL AND error_code IS NOT NULL AND error_message IS NOT NULL)
    )
);

CREATE INDEX idx_task_queue ON correction_task (status, created_at, id);
