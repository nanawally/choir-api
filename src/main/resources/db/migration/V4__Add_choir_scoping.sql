CREATE TABLE choirs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL
);

INSERT INTO choirs (id, name) VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'La Cappella');

ALTER TABLE users      ADD COLUMN choir_id UUID NOT NULL REFERENCES choirs(id) DEFAULT 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';
ALTER TABLE chorists   ADD COLUMN choir_id UUID NOT NULL REFERENCES choirs(id) DEFAULT 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';
ALTER TABLE voice_groups ADD COLUMN choir_id UUID NOT NULL REFERENCES choirs(id) DEFAULT 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';
ALTER TABLE songs      ADD COLUMN choir_id UUID NOT NULL REFERENCES choirs(id) DEFAULT 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';
ALTER TABLE concerts   ADD COLUMN choir_id UUID NOT NULL REFERENCES choirs(id) DEFAULT 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';
ALTER TABLE songbooks  ADD COLUMN choir_id UUID NOT NULL REFERENCES choirs(id) DEFAULT 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';
-- formations.choir_id is nullable: only set for base formations (concert_id IS NULL)
ALTER TABLE formations ADD COLUMN choir_id UUID REFERENCES choirs(id) DEFAULT 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';

-- Clear choir_id on concert formations (scoped through their concert instead)
UPDATE formations SET choir_id = NULL WHERE concert_id IS NOT NULL;

-- Drop defaults; all existing rows are now assigned
ALTER TABLE users        ALTER COLUMN choir_id DROP DEFAULT;
ALTER TABLE chorists     ALTER COLUMN choir_id DROP DEFAULT;
ALTER TABLE voice_groups ALTER COLUMN choir_id DROP DEFAULT;
ALTER TABLE songs        ALTER COLUMN choir_id DROP DEFAULT;
ALTER TABLE concerts     ALTER COLUMN choir_id DROP DEFAULT;
ALTER TABLE songbooks    ALTER COLUMN choir_id DROP DEFAULT;
ALTER TABLE formations   ALTER COLUMN choir_id DROP DEFAULT;