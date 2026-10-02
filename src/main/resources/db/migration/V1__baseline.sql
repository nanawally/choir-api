-- V1: baseline schema (all tables that existed before Flyway was introduced)

CREATE TABLE IF NOT EXISTS voice_groups (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(255) NOT NULL,
    is_standard  BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS voice_parts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    voice_group_id  UUID         NOT NULL REFERENCES voice_groups(id),
    name            VARCHAR(255) NOT NULL,
    color           VARCHAR(30)  NOT NULL,
    shape           VARCHAR(30)  NOT NULL,
    sort_order      INTEGER      NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS chorists (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name        VARCHAR(255) NOT NULL DEFAULT '',
    last_name         VARCHAR(255) NOT NULL DEFAULT '',
    is_section_leader BOOLEAN      NOT NULL DEFAULT FALSE,
    is_archived       BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS voice_assignments (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chorist_id    UUID NOT NULL REFERENCES chorists(id),
    voice_part_id UUID NOT NULL REFERENCES voice_parts(id)
);

CREATE TABLE IF NOT EXISTS concerts (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name      VARCHAR(255)  NOT NULL,
    date      VARCHAR(10),
    image_url VARCHAR(1024)
);

CREATE TABLE IF NOT EXISTS concert_chorists (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    concert_id  UUID NOT NULL REFERENCES concerts(id),
    chorist_id  UUID NOT NULL REFERENCES chorists(id)
);

CREATE TABLE IF NOT EXISTS songs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    composer        VARCHAR(255),
    arranger        VARCHAR(255),
    lyricist        VARCHAR(255),
    delning         VARCHAR(255),
    languages       VARCHAR(500),
    length          VARCHAR(10),
    accompanied     BOOLEAN,
    instrument      VARCHAR(255),
    year            INTEGER,
    collection_name VARCHAR(255),
    has_soloists    BOOLEAN,
    soloist_names   VARCHAR(500),
    has_sheet_music BOOLEAN      NOT NULL DEFAULT FALSE,
    sheet_music_key VARCHAR(512),
    lyrics          TEXT
);

CREATE TABLE IF NOT EXISTS song_audio_files (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    song_id       UUID         NOT NULL REFERENCES songs(id),
    voice_part_id UUID         REFERENCES voice_parts(id),
    storage_key   VARCHAR(512) NOT NULL,
    file_name     VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS song_links (
    id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    song_id UUID          NOT NULL REFERENCES songs(id),
    url     VARCHAR(2048) NOT NULL,
    label   VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS concert_songs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    concert_id  UUID    NOT NULL REFERENCES concerts(id),
    song_id     UUID    NOT NULL REFERENCES songs(id),
    sort_order  INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS formations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    concert_id  UUID REFERENCES concerts(id),
    name        VARCHAR(255) NOT NULL,
    row_sizes   VARCHAR(255) NOT NULL DEFAULT '[]'
);

CREATE TABLE IF NOT EXISTS placements (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    formation_id UUID    NOT NULL REFERENCES formations(id),
    chorist_id   UUID    NOT NULL REFERENCES chorists(id),
    grid_x       INTEGER NOT NULL,
    grid_y       INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS song_formations (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    concert_song_id  UUID    NOT NULL REFERENCES concert_songs(id),
    formation_id     UUID    NOT NULL REFERENCES formations(id),
    sort_order       INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS hidden_chorists (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    concert_song_id  UUID NOT NULL REFERENCES concert_songs(id),
    chorist_id       UUID NOT NULL REFERENCES chorists(id)
);

CREATE TABLE IF NOT EXISTS users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(50)  NOT NULL
);
