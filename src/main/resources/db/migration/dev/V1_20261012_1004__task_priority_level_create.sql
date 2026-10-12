CREATE TABLE task_priority_level (
    task_priority_level_id UUID        NOT NULL DEFAULT RANDOM_UUID(),
    priority_level         INTEGER     NOT NULL,
    name                   VARCHAR(50) NOT NULL,
    color                  VARCHAR(7)  NOT NULL,
    is_deleted             BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_task_priority_level       PRIMARY KEY (task_priority_level_id),
    CONSTRAINT uk_task_priority_level_level UNIQUE (priority_level),
    CONSTRAINT uk_task_priority_level_name  UNIQUE (name),
    CONSTRAINT ck_task_priority_level_level CHECK (priority_level > 0),
    CONSTRAINT ck_task_priority_level_color CHECK (REGEXP_LIKE(color, '^#[0-9A-Fa-f]{6}$'))
);
