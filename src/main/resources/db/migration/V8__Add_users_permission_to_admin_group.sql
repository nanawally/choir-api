-- The V6 migration seeded Admin groups with all features except 'users'.
-- Add the missing 'users' permission to every Admin group.
INSERT INTO group_permissions (group_id, feature, can_view, can_edit, can_delete)
SELECT pg.id, 'users', TRUE, TRUE, TRUE
FROM permission_groups pg
WHERE pg.name = 'Admin'
ON CONFLICT (group_id, feature) DO NOTHING;
