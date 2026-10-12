CREATE TABLE users (
    user_id        UUID         NOT NULL DEFAULT gen_random_uuid(),
    first_name     VARCHAR(30)  NOT NULL,
    last_name      VARCHAR(50)  NOT NULL,
    username       VARCHAR(30)  NOT NULL,
    password       VARCHAR(200) NOT NULL,
    email          VARCHAR(254) NOT NULL,
    email_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_users PRIMARY KEY (user_id)
);

-- Case-insensitive uniqueness (also serves lookups by lower(email) / lower(username))
CREATE UNIQUE INDEX uk_users_username_lower ON users (lower(username));
CREATE UNIQUE INDEX uk_users_email_lower    ON users (lower(email));
