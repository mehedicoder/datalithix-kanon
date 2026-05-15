# LLM Service Configuration

This document defines how LLM models are configured, exposed as Spring services, and managed from the admin UI.

## Principle

- LLMs are configurable platform services, not hardcoded model clients.
- The application should expose model access through Spring-managed service beans.
- Admins must be able to configure local server models and API-backed models from the UI.
- Model routing should select from configured model profiles by tenant, domain, task type, policy, cost, latency, and compliance constraints.
- Credentials and secrets must not be stored as plain text in UI-visible fields.

## Spring Bean Design

Use an interface-first design in `kanon-ai-routing`.

Recommended contracts:

- `LlmService`
- `LlmServiceRegistry`
- `LlmServiceFactory`
- `ModelProfileRepository`
- `ModelInvocationService`
- `ModelRouter`

`ModelRouter` chooses the logical model profile.

`ModelInvocationService` invokes the selected model through an `LlmService`.

`LlmServiceRegistry` resolves active Spring beans by provider, profile id, tenant id, and capability.

`LlmServiceFactory` creates provider clients from admin-managed configuration.

## Injection Style

Use Spring dependency injection through constructors for required dependencies and setters/getters for runtime configurable properties where Spring binding or admin configuration requires mutable configuration objects.

Recommended pattern:

```java
public interface LlmService {
    ModelInvocationResult invoke(ModelInvocationRequest request);
    boolean supports(ModelCapability capability);
}
```

```java
public class ConfigurableLlmService implements LlmService {
    private LlmServiceProperties properties;

    public LlmServiceProperties getProperties() {
        return properties;
    }

    public void setProperties(LlmServiceProperties properties) {
        this.properties = properties;
    }

    @Override
    public ModelInvocationResult invoke(ModelInvocationRequest request) {
        // Provider-specific invocation.
    }

    @Override
    public boolean supports(ModelCapability capability) {
        return properties.capabilities().contains(capability);
    }
}
```

Use Spring `@ConfigurationProperties` for boot-time defaults and database-backed admin configuration for runtime model profiles.

## Supported Model Backend Types

The MVP should support two backend categories.

| Backend type | Purpose | Examples |
| --- | --- | --- |
| Local server model | Models running in the customer/server environment. | Ollama, llama.cpp server, vLLM, local OpenAI-compatible endpoint |
| API model | External or managed API model. | OpenAI-compatible API, Azure OpenAI, Anthropic-compatible adapter, Gemini-compatible adapter, Spring AI provider |

Prefer OpenAI-compatible HTTP shape for local/API endpoints when possible because many local runtimes support it. Provider-specific adapters can be added behind the same interface.

## Admin UI Fields

The admin UI must allow model configuration without code changes.

### Identity

- `model_profile_id`
- `tenant_id`
- `display_name`
- `description`
- `enabled`
- `default_profile`
- `created_at`
- `created_by`
- `updated_at`
- `updated_by`
- `version`

### Backend

- `backend_type`
- `provider`
- `base_url`
- `api_version`
- `model_name`
- `deployment_name`
- `endpoint_path`
- `local_runtime`
- `server_host`
- `server_port`

### Credentials

- `credential_ref`
- `api_key_secret_ref`
- `oauth_client_ref`
- `auth_type`

Secrets must be stored in a secret manager, encrypted database field, environment variable reference, or deployment secret. The UI should store references, not raw secrets.

### Capabilities

- `supported_task_types`
- `supported_modalities`
- `supported_domains`
- `supports_streaming`
- `supports_json_mode`
- `supports_tool_calling`
- `supports_vision`
- `supports_embeddings`
- `supports_reranking`
- `supports_transcription`
- `supports_image_input`
- `supports_video_metadata_input`

### Routing and Policy

- `priority`
- `fallback_profile_id`
- `allowed_tenant_ids`
- `allowed_domain_types`
- `allowed_data_residency`
- `allow_pii`
- `allow_sensitive_data`
- `cloud_allowed`
- `local_only`
- `compliance_tags`
- `policy_tags`

### Runtime Parameters

- `temperature`
- `top_p`
- `max_output_tokens`
- `context_window_tokens`
- `timeout_seconds`
- `retry_policy`
- `max_attempts`
- `rate_limit_per_minute`
- `concurrency_limit`

### Cost and Performance

- `cost_class`
- `latency_class`
- `input_token_cost`
- `output_token_cost`
- `expected_latency_ms`
- `health_status`
- `last_health_check_at`
- `last_failure_reason`

### Observability

- `trace_enabled`
- `log_prompts`
- `log_responses`
- `redact_sensitive_logs`
- `evidence_required`

Prompt and response logging must be disabled or redacted by default for sensitive tenants and regulated workflows.

## Local Model Configuration

Local models should be represented as normal model profiles with `backend_type = LOCAL_SERVER`.

Recommended local configuration fields:

- `base_url`
- `model_name`
- `local_runtime`
- `server_host`
- `server_port`
- `supports_vision`
- `context_window_tokens`
- `concurrency_limit`
- `local_only = true`
- `cloud_allowed = false`

## API Model Configuration

API models should be represented as normal model profiles with `backend_type = API`.

Recommended API configuration fields:

- `provider`
- `base_url`
- `api_version`
- `model_name`
- `deployment_name`
- `credential_ref`
- `timeout_seconds`
- `rate_limit_per_minute`
- `cloud_allowed`
- `allowed_data_residency`

## Persistence Rules

- Model profiles must be persisted in PostgreSQL.
- Flyway migrations must include audit columns.
- Persistence code must write audit columns.
- Model invocation events must be written to the evidence ledger.
- Secrets must be referenced, encrypted, or stored outside the database.
- Admin changes to model configuration must create evidence events.

## Evidence Events

Record evidence for:

- model profile created
- model profile updated
- model profile enabled
- model profile disabled
- model health check failed
- model health check recovered
- model selected by router
- model invocation started
- model invocation completed
- model invocation failed
- fallback model selected

## Admin UI Behavior

- Provide a model profile grid with meaningful icons for local models, API models, enabled/disabled status, health status, compliance, and fallback configuration.
- Provide create/edit forms for local server models and API models.
- Validate required fields based on backend type.
- Provide a test connection action.
- Provide a dry-run route action for tenant, domain, task type, modality, and data residency.
- Show health status and last failure reason.
- Show where the model is used by tenant, workflow, agent, and task type.

## MVP Scope

For MVP, implement:

- One common `LlmService` interface.
- One local OpenAI-compatible adapter.
- One API OpenAI-compatible adapter.
- Database-backed model profiles.
- Admin UI for creating, editing, enabling, disabling, testing, and assigning model profiles.
- Evidence for model profile changes and model invocation.

Defer provider-specific advanced adapters until the generic OpenAI-compatible path is stable.

