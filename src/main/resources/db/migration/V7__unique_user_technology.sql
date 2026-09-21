DELETE FROM user_technology a
USING user_technology b
WHERE a.id > b.id
  AND a.user_id = b.user_id
  AND a.technology_id = b.technology_id;

ALTER TABLE user_technology
    ADD CONSTRAINT uq_user_technology_user_tech UNIQUE (user_id, technology_id);