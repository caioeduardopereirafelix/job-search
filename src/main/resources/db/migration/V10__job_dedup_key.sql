-- Regra de negocio: mesma vaga = mesmo titulo + empresa + local (ignorando
-- caixa e espacos). O Adzuna publica o mesmo anuncio com ids/URLs diferentes.
ALTER TABLE job ADD COLUMN dedup_key VARCHAR(800);

UPDATE job
SET dedup_key = lower(regexp_replace(trim(coalesce(title_job, '')), '\s+', ' ', 'g')) || '|'
             || lower(regexp_replace(trim(coalesce(company_job, '')), '\s+', ' ', 'g')) || '|'
             || lower(regexp_replace(trim(coalesce(location, '')), '\s+', ' ', 'g'));

CREATE TEMP TABLE job_map AS
SELECT id AS old_id, keeper_id
FROM (
    SELECT id, first_value(id) OVER (PARTITION BY dedup_key ORDER BY posted_at DESC NULLS LAST, id::text) AS keeper_id
    FROM job
) t
WHERE id <> keeper_id;

-- Reaponta um match por usuario para a vaga mantida (sem violar o unique) e apaga o resto.
UPDATE job_match jm
SET job_id = m.keeper_id
FROM job_map m
WHERE jm.job_id = m.old_id
  AND NOT EXISTS (
      SELECT 1 FROM job_match x
      WHERE x.user_id = jm.user_id AND x.job_id = m.keeper_id
  )
  AND jm.id = (
      SELECT min(y.id::text)::uuid FROM job_match y
      JOIN job_map m2 ON m2.old_id = y.job_id
      WHERE y.user_id = jm.user_id AND m2.keeper_id = m.keeper_id
  );

DELETE FROM job_match WHERE job_id IN (SELECT old_id FROM job_map);
DELETE FROM job WHERE id IN (SELECT old_id FROM job_map);

ALTER TABLE job ADD CONSTRAINT uq_job_dedup_key UNIQUE (dedup_key);
