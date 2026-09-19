CREATE TABLE job_match (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    job_id UUID REFERENCES job(id),
    score DOUBLE PRECISION,
    matched_technologies VARCHAR(255)[],
    created_at TIMESTAMP
);

CREATE INDEX idx_job_match_user_id ON job_match(user_id);
CREATE INDEX idx_job_match_job_id ON job_match(job_id);
