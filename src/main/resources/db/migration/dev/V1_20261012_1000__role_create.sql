CREATE TABLE role (
    role_id     UUID          NOT NULL DEFAULT RANDOM_UUID(),
    name        VARCHAR(50)   NOT NULL,
    description VARCHAR(1000),
    is_deleted  BOOLEAN       NOT NULL DEFAULT FALSE,
    updated_at  TIMESTAMP WITH TIME ZONE   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at  TIMESTAMP WITH TIME ZONE   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_role      PRIMARY KEY (role_id),
    CONSTRAINT uk_role_name UNIQUE (name)
);
