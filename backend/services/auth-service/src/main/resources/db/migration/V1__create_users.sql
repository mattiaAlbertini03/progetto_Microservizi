CREATE TABLE users (
    id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(320) NOT NULL UNIQUE
);

INSERT INTO users (id, username, password, email)
VALUES ('user-1', 'demo', 'demo', 'demo@acme.local');
