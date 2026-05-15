UPDATE kanon_role
SET role_key = 'ORGANIZATION_ADMIN',
    name = 'Organization Admin',
    role_id = 'organization-admin',
    updated_at = CURRENT_TIMESTAMP,
    updated_by = 'system',
    audit_version = audit_version + 1
WHERE role_key = 'ORGANIZATION_MANAGER';
