CREATE TABLE users (
    user_id        UUID         NOT NULL DEFAULT RANDOM_UUID(),
    first_name     VARCHAR(30)  NOT NULL,
    last_name      VARCHAR(50)  NOT NULL,
    username       VARCHAR(30)  NOT NULL,
    password       VARCHAR(200) NOT NULL,
    email          VARCHAR(254) NOT NULL,
    email_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_users          PRIMARY KEY (user_id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email    UNIQUE (email)
);
