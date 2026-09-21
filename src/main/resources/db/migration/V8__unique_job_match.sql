DELETE FROM job_match a
USING job_match b
WHERE a.id > b.id
  AND a.user_id = b.user_id
  AND a.job_id = b.job_id;

ALTER TABLE job_match
    ADD CONSTRAINT uq_job_match_user_job UNIQUE (user_id, job_id);
