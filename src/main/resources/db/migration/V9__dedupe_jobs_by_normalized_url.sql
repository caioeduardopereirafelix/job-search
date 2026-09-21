-- O redirect_url do Adzuna traz um token "se" novo a cada busca, o que fazia a
-- mesma vaga ser inserida de novo a cada ciclo. Normaliza a URL (sem "se") e
-- funde as vagas repetidas numa so.
CREATE TEMP TABLE job_norm AS
SELECT id,
       regexp_replace(regexp_replace(source_url_job, '([?&])se=[^&]*&?', '\1'), '[?&]+$', '') AS norm_url
FROM job;

CREATE TEMP TABLE job_keeper AS
SELECT norm_url, (array_agg(id ORDER BY id::text))[1] AS keeper_id
FROM job_norm
GROUP BY norm_url;

CREATE TEMP TABLE job_map AS
SELECT n.id AS old_id, k.keeper_id
FROM job_norm n
JOIN job_keeper k ON k.norm_url = n.norm_url
WHERE n.id <> k.keeper_id;

-- Reaponta os matches das vagas repetidas para a vaga mantida, evitando conflito
-- com o unique (user_id, job_id); o que sobrar e apagado.
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

UPDATE job j
SET source_url_job = n.norm_url
FROM job_norm n
WHERE j.id = n.id AND j.source_url_job <> n.norm_url;
