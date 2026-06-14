CREATE TYPE source_type AS ENUM ('s3_only', 'sqs', 'sns', 'lambda');

CREATE TABLE processed_payload (
    id SERIAL PRIMARY KEY,
    payload JSONB NOT NULL,
    source_type source_type NOT NULL,
    processing_datetime TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
