CREATE TABLE IF NOT EXISTS songbooks (
    id         UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255)  NOT NULL,
    date       VARCHAR(10)   NULL,
    image_url  VARCHAR(1024) NULL,
    is_pinned  BOOLEAN       NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS songbook_songs (
    id           UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    songbook_id  UUID    NOT NULL REFERENCES songbooks(id),
    song_id      UUID    NOT NULL REFERENCES songs(id),
    sort_order   INTEGER NOT NULL DEFAULT 0,
    UNIQUE (songbook_id, song_id)
);