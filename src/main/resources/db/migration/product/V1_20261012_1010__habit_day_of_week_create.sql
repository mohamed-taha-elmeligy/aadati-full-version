CREATE TABLE habit_day_of_week (
    habit_id    UUID   NOT NULL,
    day_week_id BIGINT NOT NULL,
    CONSTRAINT pk_habit_day_of_week     PRIMARY KEY (habit_id, day_week_id),
    CONSTRAINT fk_habit_day_of_week_h   FOREIGN KEY (habit_id)    REFERENCES habit (habit_id) ON DELETE CASCADE,
    CONSTRAINT fk_habit_day_of_week_d   FOREIGN KEY (day_week_id) REFERENCES habit_day_week (day_week_id) ON DELETE RESTRICT
);

CREATE INDEX inx_habit_day_of_week_day ON habit_day_of_week (day_week_id);
