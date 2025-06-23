-- V1__init_schema.sql
-- Flyway migration: initial schema based on original DDL

-- Workspace (tenant) table
CREATE TABLE IF NOT EXISTS public.workspace (
  id             SERIAL          PRIMARY KEY,
  team_id        VARCHAR(50)     UNIQUE NOT NULL,
  bot_token      VARCHAR(200)    NOT NULL,
  signing_secret VARCHAR(200)    NOT NULL,
  created_at     TIMESTAMP        NOT NULL DEFAULT now()
);

-- Message events table
CREATE TABLE IF NOT EXISTS public.message_event (
  id         BIGSERIAL         PRIMARY KEY,
  team_id    VARCHAR(50)       NOT NULL REFERENCES public.workspace(team_id),
  channel_id VARCHAR(50)       NOT NULL,
  user_id    VARCHAR(50)       NOT NULL,
  text       TEXT              NOT NULL,
  ts         VARCHAR(30)       NOT NULL,
  ts_epoch   DOUBLE PRECISION  NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_msg_team_ts
  ON public.message_event(team_id, ts_epoch);

-- Reaction events table
CREATE TABLE IF NOT EXISTS public.reaction_event (
  id         BIGSERIAL         PRIMARY KEY,
  team_id    VARCHAR(50)       NOT NULL REFERENCES public.workspace(team_id),
  channel_id VARCHAR(50)       NOT NULL,
  user_id    VARCHAR(50)       NOT NULL,
  reaction   VARCHAR(50)       NOT NULL,
  ts         VARCHAR(30)       NOT NULL,
  ts_epoch   DOUBLE PRECISION  NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_react_team_ts
  ON public.reaction_event(team_id, ts_epoch);

-- Default items table
CREATE TABLE IF NOT EXISTS public.default_item (
  id         SERIAL            PRIMARY KEY,
  team_id    VARCHAR(50)       NOT NULL REFERENCES public.workspace(team_id),
  item_name  VARCHAR(100)      NOT NULL,
  quantity   INTEGER           NOT NULL,
  UNIQUE(team_id, item_name)
);
CREATE INDEX IF NOT EXISTS idx_default_team
  ON public.default_item(team_id);

-- Schedule settings table
CREATE TABLE IF NOT EXISTS public.schedule_settings (
  team_id    VARCHAR(50)       PRIMARY KEY REFERENCES public.workspace(team_id),
  open_day   VARCHAR(3)        NOT NULL,
  open_time  TIME              NOT NULL,
  close_day  VARCHAR(3)        NOT NULL,
  close_time TIME              NOT NULL,
  updated_at TIMESTAMP         NOT NULL DEFAULT now()
);
