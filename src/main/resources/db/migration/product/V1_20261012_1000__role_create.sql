CREATE TABLE role (
    role_id     UUID          NOT NULL DEFAULT gen_random_uuid(),
    name        VARCHAR(50)   NOT NULL,
    description VARCHAR(1000),
    is_deleted  BOOLEAN       NOT NULL DEFAULT FALSE,
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_role      PRIMARY KEY (role_id),
    CONSTRAINT uk_role_name UNIQUE (name)
);
