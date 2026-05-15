CREATE TABLE tenant (
    tenant_id VARCHAR(120) PRIMARY KEY,
    tenant_key VARCHAR(120) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    status VARCHAR(40) NOT NULL,
    data_residency VARCHAR(80),
    default_locale VARCHAR(40),
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL
);

CREATE INDEX idx_tenant_status_name
    ON tenant (status, name);

CREATE TABLE organization (
    organization_id VARCHAR(120) NOT NULL,
    tenant_id VARCHAR(120) NOT NULL REFERENCES tenant (tenant_id),
    organization_key VARCHAR(120) NOT NULL,
    name VARCHAR(180) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, organization_id),
    CONSTRAINT uq_organization_tenant_key UNIQUE (tenant_id, organization_key)
);

CREATE INDEX idx_organization_tenant_status_name
    ON organization (tenant_id, status, name);

CREATE TABLE workspace (
    workspace_id VARCHAR(120) NOT NULL,
    tenant_id VARCHAR(120) NOT NULL,
    organization_id VARCHAR(120) NOT NULL,
    workspace_key VARCHAR(120) NOT NULL,
    name VARCHAR(180) NOT NULL,
    workspace_type VARCHAR(60) NOT NULL,
    domain_type VARCHAR(80),
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, organization_id, workspace_id),
    CONSTRAINT fk_workspace_organization
        FOREIGN KEY (tenant_id, organization_id)
        REFERENCES organization (tenant_id, organization_id),
    CONSTRAINT uq_workspace_org_key UNIQUE (tenant_id, organization_id, workspace_key)
);

CREATE INDEX idx_workspace_tenant_org_status_name
    ON workspace (tenant_id, organization_id, status, name);

CREATE INDEX idx_workspace_tenant_type
    ON workspace (tenant_id, workspace_type);

CREATE TABLE user_account (
    user_id VARCHAR(120) PRIMARY KEY,
    username VARCHAR(160) NOT NULL UNIQUE,
    email VARCHAR(240) NOT NULL UNIQUE,
    display_name VARCHAR(180) NOT NULL,
    status VARCHAR(40) NOT NULL,
    is_system_user BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL
);

CREATE INDEX idx_user_account_status_display_name
    ON user_account (status, display_name);

CREATE TABLE kanon_role (
    role_id VARCHAR(120) PRIMARY KEY,
    role_key VARCHAR(120) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    allowed_scope VARCHAR(40) NOT NULL,
    system_role BOOLEAN NOT NULL,
    permissions TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL
);

CREATE INDEX idx_kanon_role_scope
    ON kanon_role (allowed_scope, role_key);

CREATE TABLE membership (
    membership_id VARCHAR(120) PRIMARY KEY,
    user_id VARCHAR(120) NOT NULL REFERENCES user_account (user_id),
    scope VARCHAR(40) NOT NULL,
    tenant_id VARCHAR(120),
    organization_id VARCHAR(120),
    workspace_id VARCHAR(120),
    status VARCHAR(40) NOT NULL,
    starts_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL,
    CONSTRAINT ck_membership_scope_ids CHECK (
        (scope = 'PLATFORM' AND tenant_id IS NULL AND organization_id IS NULL AND workspace_id IS NULL)
        OR (scope = 'TENANT' AND tenant_id IS NOT NULL AND organization_id IS NULL AND workspace_id IS NULL)
        OR (scope = 'ORGANIZATION' AND tenant_id IS NOT NULL AND organization_id IS NOT NULL AND workspace_id IS NULL)
        OR (scope = 'WORKSPACE' AND tenant_id IS NOT NULL AND organization_id IS NOT NULL AND workspace_id IS NOT NULL)
    )
);

CREATE INDEX idx_membership_user_status
    ON membership (user_id, status);

CREATE INDEX idx_membership_tenant_scope
    ON membership (tenant_id, scope, status);

CREATE INDEX idx_membership_workspace_scope
    ON membership (tenant_id, organization_id, workspace_id, scope, status);

CREATE TABLE membership_role (
    membership_id VARCHAR(120) NOT NULL REFERENCES membership (membership_id) ON DELETE CASCADE,
    role_id VARCHAR(120) NOT NULL REFERENCES kanon_role (role_id),
    PRIMARY KEY (membership_id, role_id)
);

CREATE INDEX idx_membership_role_role
    ON membership_role (role_id);

ALTER TABLE workflow_definition
    ADD COLUMN organization_id VARCHAR(120),
    ADD COLUMN workspace_id VARCHAR(120);

UPDATE workflow_definition
SET organization_id = 'default-org',
    workspace_id = 'administration'
WHERE organization_id IS NULL OR workspace_id IS NULL;

ALTER TABLE workflow_definition
    ALTER COLUMN organization_id SET NOT NULL,
    ALTER COLUMN workspace_id SET NOT NULL;

CREATE INDEX idx_workflow_definition_workspace_type_enabled
    ON workflow_definition (tenant_id, organization_id, workspace_id, workflow_type, enabled);

ALTER TABLE workflow_instance
    ADD COLUMN organization_id VARCHAR(120),
    ADD COLUMN workspace_id VARCHAR(120),
    ADD COLUMN assigned_membership_id VARCHAR(120),
    ADD COLUMN reviewer_membership_id VARCHAR(120),
    ADD COLUMN approver_membership_id VARCHAR(120);

UPDATE workflow_instance
SET organization_id = 'default-org',
    workspace_id = 'administration'
WHERE organization_id IS NULL OR workspace_id IS NULL;

ALTER TABLE workflow_instance
    ALTER COLUMN organization_id SET NOT NULL,
    ALTER COLUMN workspace_id SET NOT NULL;

CREATE INDEX idx_workflow_instance_workspace_case
    ON workflow_instance (tenant_id, organization_id, workspace_id, case_id);

CREATE INDEX idx_workflow_instance_workspace_review_due
    ON workflow_instance (tenant_id, organization_id, workspace_id, review_required, assigned_membership_id, due_at);
