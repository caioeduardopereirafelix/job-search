CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255),
    password VARCHAR(255),
    roles VARCHAR(255),
    created_at TIMESTAMP
);
 
CREATE TABLE technology (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    category VARCHAR(255)
);
 
CREATE TABLE user_technology (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    technology_id UUID REFERENCES technology(id)
);
 
CREATE TABLE resume (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id),
    original_file_name VARCHAR(255),
    extract_text TEXT,
    file_path VARCHAR(255),
    upload_at TIMESTAMP
);
 
CREATE TABLE job (
    id UUID PRIMARY KEY,
    title_job VARCHAR(255),
    company_job VARCHAR(255),
    description_job TEXT,
    source_url_job VARCHAR(2048) UNIQUE,
    source_name_job VARCHAR(255),
    location VARCHAR(255),
    posted_at TIMESTAMP,
    fetched_at TIMESTAMP
);
 
CREATE INDEX idx_user_technology_user_id ON user_technology(user_id);
CREATE INDEX idx_job_source_url_job ON job(source_url_job);
 