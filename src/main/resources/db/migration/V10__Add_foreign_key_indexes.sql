-- Add indexes on foreign key columns used in WHERE clauses.
-- PostgreSQL does NOT auto-index foreign keys; only PKs and UNIQUE constraints get implicit indexes.

-- choir_id scoping (hit on nearly every API request)
CREATE INDEX idx_chorists_choir_id         ON chorists (choir_id);
CREATE INDEX idx_concerts_choir_id         ON concerts (choir_id);
CREATE INDEX idx_songs_choir_id            ON songs (choir_id);
CREATE INDEX idx_voice_groups_choir_id     ON voice_groups (choir_id);
CREATE INDEX idx_songbooks_choir_id        ON songbooks (choir_id);
CREATE INDEX idx_users_choir_id            ON users (choir_id);
CREATE INDEX idx_permission_groups_choir_id ON permission_groups (choir_id);
CREATE INDEX idx_formations_choir_id       ON formations (choir_id);

-- Junction / child table foreign keys
CREATE INDEX idx_concert_songs_concert_id      ON concert_songs (concert_id);
CREATE INDEX idx_concert_songs_song_id         ON concert_songs (song_id);
CREATE INDEX idx_concert_chorists_concert_id   ON concert_chorists (concert_id);
CREATE INDEX idx_formations_concert_id         ON formations (concert_id);
CREATE INDEX idx_voice_parts_voice_group_id    ON voice_parts (voice_group_id);
CREATE INDEX idx_placements_formation_id       ON placements (formation_id);
CREATE INDEX idx_placements_chorist_id         ON placements (chorist_id);
CREATE INDEX idx_song_formations_concert_song_id ON song_formations (concert_song_id);
CREATE INDEX idx_hidden_chorists_concert_song_id ON hidden_chorists (concert_song_id);
CREATE INDEX idx_voice_assignments_voice_part_id ON voice_assignments (voice_part_id);
CREATE INDEX idx_voice_assignments_chorist_id    ON voice_assignments (chorist_id);
CREATE INDEX idx_song_audio_files_song_id      ON song_audio_files (song_id);
CREATE INDEX idx_song_links_song_id            ON song_links (song_id);
CREATE INDEX idx_users_chorist_id              ON users (chorist_id);
