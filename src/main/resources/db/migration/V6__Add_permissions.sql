CREATE TABLE permission_groups (
    id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    choir_id UUID NOT NULL REFERENCES choirs(id),
    name     VARCHAR(255) NOT NULL
);

CREATE TABLE group_permissions (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id   UUID NOT NULL REFERENCES permission_groups(id) ON DELETE CASCADE,
    feature    VARCHAR(100) NOT NULL,
    can_view   BOOLEAN NOT NULL DEFAULT FALSE,
    can_edit   BOOLEAN NOT NULL DEFAULT FALSE,
    can_delete BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (group_id, feature)
);

CREATE TABLE user_groups (
    id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    group_id UUID NOT NULL REFERENCES permission_groups(id) ON DELETE CASCADE,
    UNIQUE (user_id, group_id)
);

ALTER TABLE songbooks ADD COLUMN is_hidden BOOLEAN NOT NULL DEFAULT FALSE;

-- Seed Admin group for every choir and grant it all permissions
WITH admin_groups AS (
    INSERT INTO permission_groups (choir_id, name)
    SELECT id, 'Admin' FROM choirs
    RETURNING id, choir_id
)
INSERT INTO group_permissions (group_id, feature, can_view, can_edit, can_delete)
SELECT ag.id, f.feature, TRUE, TRUE, TRUE
FROM admin_groups ag
CROSS JOIN (VALUES
    ('chorists'),
    ('chorists_notes'),
    ('voice_groups'),
    ('songs'),
    ('songbooks'),
    ('songbooks_hidden'),
    ('concerts'),
    ('formations'),
    ('base_formations'),
    ('permissions')
) AS f(feature);

-- Add all existing admin-role users to their choir's Admin group
INSERT INTO user_groups (user_id, group_id)
SELECT u.id, pg.id
FROM users u
JOIN permission_groups pg ON pg.choir_id = u.choir_id AND pg.name = 'Admin'
WHERE u.role = 'admin';