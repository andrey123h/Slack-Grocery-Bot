-- V1__init_schema.sql
-- Flyway migration: initialize schema

-- Workspace table
CREATE TABLE IF NOT EXISTS public.workspace (
    id integer NOT NULL DEFAULT nextval('workspace_id_seq'::regclass),
    team_id character varying(50) NOT NULL,
    bot_token character varying(200) NOT NULL,
    signing_secret character varying(200) NOT NULL,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    CONSTRAINT workspace_pkey PRIMARY KEY (id),
    CONSTRAINT workspace_team_id_key UNIQUE (team_id)
);

-- Schedule settings per workspace
CREATE TABLE IF NOT EXISTS public.schedule_settings (
    team_id character varying(50) NOT NULL,
    open_day character varying(3) NOT NULL,
    open_time time without time zone NOT NULL,
    close_day character varying(3) NOT NULL,
    close_time time without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL DEFAULT now(),
    CONSTRAINT schedule_settings_pkey PRIMARY KEY (team_id),
    CONSTRAINT schedule_settings_team_id_fkey FOREIGN KEY (team_id)
        REFERENCES public.workspace (team_id)
);

-- Default item table
CREATE TABLE IF NOT EXISTS public.default_item (
    id integer NOT NULL DEFAULT nextval('default_item_id_seq'::regclass),
    team_id character varying(50) NOT NULL,
    item_name character varying(100) NOT NULL,
    quantity integer NOT NULL,
    CONSTRAINT default_item_pkey PRIMARY KEY (id),
    CONSTRAINT default_item_team_id_item_name_key UNIQUE (team_id, item_name),
    CONSTRAINT default_item_team_id_fkey FOREIGN KEY (team_id)
        REFERENCES public.workspace (team_id)
);
CREATE INDEX IF NOT EXISTS idx_default_team
    ON public.default_item(team_id);

-- Message events table
CREATE TABLE IF NOT EXISTS public.message_event (
    id bigint NOT NULL DEFAULT nextval('message_event_id_seq'::regclass),
    team_id character varying(50) NOT NULL,
    channel_id character varying(50) NOT NULL,
    user_id character varying(50) NOT NULL,
    text text NOT NULL,
    ts character varying(30) NOT NULL,
    ts_epoch double precision NOT NULL,
    CONSTRAINT message_event_pkey PRIMARY KEY (id),
    CONSTRAINT message_event_team_id_fkey FOREIGN KEY (team_id)
        REFERENCES public.workspace (team_id)
);
CREATE INDEX IF NOT EXISTS idx_msg_team_ts
    ON public.message_event(team_id, ts_epoch);

-- Reaction events table
CREATE TABLE IF NOT EXISTS public.reaction_event (
    id bigint NOT NULL DEFAULT nextval('reaction_event_id_seq'::regclass),
    team_id character varying(50) NOT NULL,
    channel_id character varying(50) NOT NULL,
    user_id character varying(50) NOT NULL,
    reaction character varying(50) NOT NULL,
    ts character varying(30) NOT NULL,
    ts_epoch double precision NOT NULL,
    CONSTRAINT reaction_event_pkey PRIMARY KEY (id),
    CONSTRAINT reaction_event_team_id_fkey FOREIGN KEY (team_id)
        REFERENCES public.workspace (team_id)
);
CREATE INDEX IF NOT EXISTS idx_react_team_ts
    ON public.reaction_event(team_id, ts_epoch);
