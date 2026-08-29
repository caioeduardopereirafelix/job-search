CREATE TABLE jobs (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    company VARCHAR(255),
    description TEXT,
    source_url VARCHAR(1000) NOT NULL UNIQUE,
    source_name VARCHAR(100) NOT NULL,
    location VARCHAR(255),
    posted_at TIMESTAMP,
    fetched_at TIMESTAMP NOT NULL DEFAULT now()
);