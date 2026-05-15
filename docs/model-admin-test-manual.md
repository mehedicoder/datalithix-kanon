# Model Administration E2E Test Manual

## Scope

This manual verifies the Model Administration UI for creating, configuring, testing, and managing AI model profiles with real backend integration.

## Prerequisites

- Platform admin or user with model configuration permissions
- Access to the Administration > Models view
- For local models: Ollama installed and running (see Ollama Setup section)
- For API models: Valid API credentials (OpenAI, Anthropic, etc.)

## Test Data

Use unique names for easy cleanup:

- Profile key: `test-model-<date>`
- Model name: `Test Model <date>`
- Provider: OpenAI, Ollama, or Custom
- Backend type: LOCAL_SERVER or API

## Ollama Setup for Local Models

### Installing Ollama

1. **Download and Install**:
   - Visit https://ollama.ai
   - Download for your OS (Windows, macOS, Linux)
   - Run the installer

2. **Verify Installation**:
   ```bash
   ollama --version
   ```

3. **Start Ollama Server** (if not auto-started):
   ```bash
   ollama serve
   ```
   - Default URL: `http://localhost:11434`

### Pulling Models

Pull the gemma2:2b model (or any other model):

```bash
# Pull gemma2:2b (2 billion parameter model, ~1.6GB)
ollama pull gemma2:2b

# Other popular models:
ollama pull llama3.2:3b      # Llama 3.2 3B
ollama pull phi3:mini        # Microsoft Phi-3 Mini
ollama pull mistral:7b       # Mistral 7B
```

### Verify Model is Available

```bash
# List installed models
ollama list

# Test the model
ollama run gemma2:2b "Hello, how are you?"
```

### Ollama API Endpoints

Ollama provides OpenAI-compatible API:
- Base URL: `http://localhost:11434`
- Chat endpoint: `/v1/chat/completions`
- Models endpoint: `/v1/models`

## Configuring Ollama Models in Kanon

### Example: Gemma2:2b Configuration

**Profile Configuration**:
- **Profile Key**: `gemma2-2b-local`
- **Model Name**: `Gemma 2 2B (Local)`
- **Backend Type**: `LOCAL_SERVER`
- **Provider**: `Ollama`
- **Model ID**: `gemma2:2b` (exact name from `ollama list`)
- **Base URL**: `http://localhost:11434`
- **Secret Reference**: Leave empty (no auth required for local Ollama)
- **Task Capabilities**: `REASONING`, `CHAT`, `SUMMARIZATION`
- **Cost Class**: `FREE`
- **Latency Class**: `FAST`
- **Priority**: `100`
- **Supports Tools**: ✓ (if model supports function calling)
- **Supports Structured Output**: ✓ (if model supports JSON mode)
- **Fallback Profile**: (optional) Another model profile key

### Example: Llama 3.2 3B Configuration

**Profile Configuration**:
- **Profile Key**: `llama32-3b-local`
- **Model Name**: `Llama 3.2 3B (Local)`
- **Backend Type**: `LOCAL_SERVER`
- **Provider**: `Ollama`
- **Model ID**: `llama3.2:3b`
- **Base URL**: `http://localhost:11434`
- **Task Capabilities**: `REASONING`, `CHAT`, `EXTRACTION`, `CLASSIFICATION`
- **Cost Class**: `FREE`
- **Latency Class**: `FAST`
- **Priority**: `95`
- **Supports Tools**: ✓
- **Supports Structured Output**: ✓

### Model Selection Guidelines

| Model | Size | Use Case | Speed | Quality |
|-------|------|----------|-------|---------|
| gemma2:2b | 1.6GB | Fast responses, simple tasks | ⚡⚡⚡ | ⭐⭐ |
| llama3.2:3b | 2GB | Balanced performance | ⚡⚡ | ⭐⭐⭐ |
| phi3:mini | 2.3GB | Reasoning, coding | ⚡⚡ | ⭐⭐⭐ |
| mistral:7b | 4.1GB | High quality, complex tasks | ⚡ | ⭐⭐⭐⭐ |

## E2E Steps

### 1. Access Model Administration

Steps:

1. Log in as platform admin or user with `model.configure` permission.
2. Open `Administration > Models`.
3. Verify the model grid loads with existing models (if any).

Expected result:

- Model grid displays with columns: status icon, type icon, model name, provider, backend type, cost class, latency class, updated timestamp, and actions.
- Status icons show health (green=healthy, yellow=degraded, red=unhealthy, gray=disabled).
- Type icons show local server (server icon) or API (cloud icon).
- Search, backend type filter, provider filter, and "Show Disabled" checkbox are visible.
- "Create model" button is visible if user has configure permission.

### 2. Create Local Ollama Model (Gemma2:2b)

**Prerequisites**: Ollama installed and `gemma2:2b` model pulled (see Ollama Setup section)

Steps:

1. Ensure Ollama is running:
   ```bash
   ollama serve
   ```

2. Verify model is available:
   ```bash
   ollama list
   # Should show gemma2:2b in the list
   ```

3. In Kanon UI, click "Create model".

4. Fill in the form:
   - **Profile key**: `gemma2-2b-local`
   - **Model name**: `Gemma 2 2B (Local)`
   - **Backend type**: `LOCAL_SERVER`
   - **Provider**: `Ollama`
   - **Model ID**: `gemma2:2b` (must match exactly from `ollama list`)
   - **Base URL**: `http://localhost:11434`
   - **Secret reference**: Leave empty
   - **Task capabilities**: Select `REASONING`, `CHAT`, `SUMMARIZATION`
   - **Cost class**: `FREE`
   - **Latency class**: `FAST`
   - **Priority**: `100`
   - **Supports tools**: checked (if model supports it)
   - **Supports structured output**: checked (if model supports JSON mode)
   - **Fallback profile**: Leave empty or specify another model

5. Click "Create".

Expected result:

- Model profile is created successfully
- Success notification: "Model profile created"
- Model appears in grid with:
  - Green/yellow/red status icon (based on health)
  - Server icon (local model indicator)
  - Enabled status
  - Health status shows "UNKNOWN" initially

Rollback:

- Delete the test model from the grid

**Troubleshooting**:
- If creation fails, verify Ollama is running: `curl http://localhost:11434/v1/models`
- Check model name matches exactly: `ollama list`
- Ensure no firewall blocking localhost:11434

### 3. Create API Model with Secret Reference

Steps:

1. Click "Create model".
2. Fill in the form:
   - Profile key: `test-api-openai-<date>`
   - Model name: `Test OpenAI GPT-4`
   - Backend type: `API`
   - Provider: `OpenAI`
   - Model ID: `gpt-4`
   - Base URL: `https://api.openai.com/v1`
   - Secret reference: `env:OPENAI_API_KEY` or `secret:openai-key`
   - Task capabilities: Select `REASONING`, `CHAT`, `STRUCTURED_EXTRACTION`
   - Cost class: `HIGH`
   - Latency class: `NORMAL`
   - Priority: `90`
   - Fallback profile: Leave empty or reference another model
   - Supports tools: checked
   - Supports structured output: checked
3. Click "Create".

Expected result:

- Model profile is created with secret reference stored (not raw API key).
- Success notification appears.
- Model appears in the grid with API type icon (cloud).

Rollback:

- Delete the test model.

### 4. Edit Model Profile

Steps:

1. Find the test model in the grid.
2. Click the "Edit" action button.
3. Update fields:
   - Model name: `Updated Test Model`
   - Priority: `110`
   - Cost class: `MEDIUM`
4. If user has `view_secrets` permission, secret reference field is editable; otherwise it shows redacted.
5. Click "Update".

Expected result:

- Model profile is updated.
- Success notification appears: "Model profile updated".
- Grid refreshes with updated values.
- Secret reference remains protected unless user has view_secrets permission.

### 5. Enable/Disable Model

Steps:

1. Find an enabled test model.
2. Click the "Disable" action button (power icon).
3. Verify status changes to disabled (gray icon).
4. Click the "Enable" action button.
5. Verify status changes to enabled.

Expected result:

- Enable/disable toggles work correctly.
- Status icon updates immediately.
- Notifications confirm each action.

### 6. Test Connection (Real Implementation)

Steps:

1. Find the gemma2:2b model in the grid.
2. Click the "Test connection" action button (connect icon).
3. A dialog opens showing "Testing connection to Gemma 2 2B (Local)..."
4. Wait for the test to complete (usually 2-10 seconds).

Expected result:

**On Success**:
- Status changes to green with "Connection to Gemma 2 2B (Local) successful"
- Result panel shows:
  ```
  Status: COMPLETED
  Latency: 2847ms
  Response: Connection successful (or actual model response)
  
  Metadata:
    inputTokens: 15
    outputTokens: 8
    totalTokens: 23
    finishReason: STOP
  ```
- Close button becomes enabled

**On Failure**:
- Status shows red with "Connection to Gemma 2 2B (Local) failed"
- Result panel shows:
  ```
  Status: FAILED
  Reason: Connection refused / Model not found / Timeout
  Latency: 5000ms
  ```

**Common Failure Reasons**:
- Ollama not running → Start with `ollama serve`
- Model not pulled → Run `ollama pull gemma2:2b`
- Wrong model ID → Check with `ollama list`
- Firewall blocking → Check localhost:11434 accessibility
- Timeout → Model loading can take time on first request

**Testing Tips**:
- First request may be slower (model loading)
- Subsequent requests are faster (model cached in memory)
- Check Ollama logs: `ollama logs` or console output

### 7. Dry-Run Routing (Real Implementation)

Steps:

1. Find the gemma2:2b model in the grid.
2. Click the "Dry-run routing" action button (split/route icon).
3. In the dialog, configure the test scenario:
   - **Task type**: Select `REASONING` (or any other type)
   - **Tenant**: Shows current tenant (read-only)
   - **Prefer local models**: ✓ Check this
   - **High-risk task**: Leave unchecked (or check to test different routing)
4. Click "Run dry-run".

Expected result:

**Routing Decision Display**:
```
=== Routing Decision ===

Task Type: REASONING
Tenant: tenant-06a99f43-9bb7-44da-9e19-e7644e6c8dad
Prefer Local: true
High Risk: false

=== Selected Route ===

Primary Profile: gemma2-2b-local
  Model: Gemma 2 2B (Local)
  Provider: Ollama
  Backend: LOCAL_SERVER
  Cost Class: FREE
  Latency Class: FAST
  Local: true
  Enabled: true
  Health: HEALTHY

Fallback Profile: (if configured)
  Model: (fallback model details)
  Provider: (fallback provider)
  Backend: (fallback backend)

Reason: Local-first classification policy

✓ This model IS the PRIMARY choice for this routing scenario
```

**Routing Indicators**:
- ✓ Green checkmark: Model IS selected as primary
- ⚠ Yellow warning: Model is fallback choice
- ✗ Red X: Model is NOT selected for this scenario

**Testing Different Scenarios**:

| Scenario | Task Type | Prefer Local | Expected Primary |
|----------|-----------|--------------|------------------|
| Local reasoning | REASONING | ✓ | gemma2-2b-local |
| Cloud reasoning | REASONING | ✗ | gpt-4 (if configured) |
| Classification | CLASSIFICATION | ✓ | gemma2-2b-local |
| High-risk extraction | EXTRACTION | ✗ (high-risk) | gpt-4 (cloud model) |

**Routing Logic**:
- Prefers local models when "Prefer local" is checked
- Routes to cloud models for high-risk tasks
- Considers task capabilities, cost class, latency class
- Uses fallback if primary is unavailable

### 8. Filter and Search Models

Steps:

1. Enter text in the search field (e.g., "Test").
2. Verify grid filters to matching models.
3. Select a backend type filter (e.g., "LOCAL_SERVER").
4. Verify grid shows only local server models.
5. Select a provider filter (e.g., "OpenAI").
6. Verify grid shows only OpenAI models.
7. Clear filters.

Expected result:

- Search filters by model name or profile key.
- Backend type filter works correctly.
- Provider filter works correctly.
- Filters combine (AND logic).
- Grid updates immediately on filter changes.

### 9. Show Disabled Models

Steps:

1. Disable a test model.
2. Verify it disappears from the default grid view.
3. Check "Show Disabled".
4. Verify disabled model appears with disabled status icon.
5. Uncheck "Show Disabled".
6. Verify disabled model disappears again.

Expected result:

- Disabled models are hidden by default.
- "Show Disabled" checkbox toggles visibility.
- Disabled models show gray status icon.

### 10. Delete Model

Steps:

1. Find a test model.
2. Click the "Delete" action button (trash icon).
3. In the confirmation dialog, type the exact model name.
4. Click "Delete".

Expected result:

- Confirmation dialog requires typing exact model name.
- Model is deleted (or marked as deleted/disabled depending on implementation).
- Success notification appears: "Model profile deleted".
- Model disappears from grid.

### 11. Permission-Based Visibility

Steps:

1. Log in as a user without `model.configure` permission.
2. Try to access `Administration > Models`.

Expected result:

- If user has `model.read` permission: Grid is visible but create/edit/delete actions are hidden.
- If user has no model permissions: Models menu item is hidden in navigation.
- Backend API enforces permissions even if UI is bypassed.

### 12. Secret Redaction

Steps:

1. Log in as a user without `view_secrets` permission.
2. Open edit dialog for a model with a secret reference.
3. Verify secret reference field shows redacted component.
4. Log in as a user with `view_secrets` permission.
5. Open edit dialog for the same model.
6. Verify secret reference field is editable.

Expected result:

- Users without `view_secrets` see redacted secret references.
- Users with `view_secrets` can view and edit secret references.
- Redacted component shows "Hidden because this field may contain sensitive data".

## Localization Verification

Steps:

1. Switch UI language to German.
2. Verify all model admin labels, buttons, notifications, and help text appear in German.
3. Switch back to English.
4. Verify all text appears in English.

Expected result:

- All UI strings use translation keys.
- English and German translations are complete.
- No hardcoded English strings appear in German mode.

## Cleanup

After testing:

1. Delete all test models created during testing.
2. Verify no test data remains in the model grid.

## Advanced Configuration

### Multiple Ollama Models

You can configure multiple Ollama models with different priorities:

```
Priority 100: gemma2:2b (fast, simple tasks)
Priority 95:  llama3.2:3b (balanced)
Priority 90:  mistral:7b (complex tasks)
```

The router will select based on task requirements and model capabilities.

### Fallback Configuration

Configure fallback chains for reliability:

```
Primary: gemma2-2b-local
Fallback: llama32-3b-local
Fallback of fallback: gpt-4-api (cloud)
```

### Remote Ollama Server

For remote Ollama instances:

```
Base URL: http://192.168.1.100:11434
or
Base URL: https://ollama.example.com
```

Ensure network connectivity and firewall rules allow access.

## Troubleshooting Guide

### Connection Test Fails

**Problem**: "Connection refused"
- **Solution**: Start Ollama with `ollama serve`
- **Verify**: `curl http://localhost:11434/v1/models`

**Problem**: "Model not found"
- **Solution**: Pull model with `ollama pull gemma2:2b`
- **Verify**: `ollama list` shows the model

**Problem**: "Timeout"
- **Solution**: First request loads model into memory (can take 10-30s)
- **Verify**: Try again, subsequent requests are faster

**Problem**: "Invalid model ID"
- **Solution**: Use exact name from `ollama list` (case-sensitive)
- **Example**: `gemma2:2b` not `gemma2` or `Gemma2:2b`

### Routing Issues

**Problem**: Wrong model selected
- **Check**: Task capabilities match task type
- **Check**: Priority values are correct
- **Check**: "Prefer local" setting matches expectation

**Problem**: No model selected
- **Check**: At least one model has required task capability
- **Check**: Models are enabled
- **Check**: Models have healthy status

## Performance Optimization

### Model Loading

- **First request**: 10-30 seconds (model loads into memory)
- **Subsequent requests**: 1-5 seconds (model cached)
- **Keep Ollama running**: Prevents reload delays

### Memory Management

- **gemma2:2b**: ~2GB RAM
- **llama3.2:3b**: ~3GB RAM
- **mistral:7b**: ~5GB RAM

Run `ollama ps` to see loaded models and memory usage.

### Concurrent Requests

Ollama handles concurrent requests, but performance degrades with many simultaneous requests. Consider:
- Load balancing across multiple Ollama instances
- Request queuing in application layer
- Model-specific rate limiting

## Implementation Status

- [x] Model grid with status, type, and action columns
- [x] Create model dialog with local server and API backend types
- [x] Edit model dialog with secret redaction
- [x] Enable/disable toggle
- [x] **Test connection action with real model invocation**
- [x] **Dry-run routing with actual routing logic**
- [x] Delete model with confirmation
- [x] Search and filter by backend type and provider
- [x] Show/hide disabled models
- [x] Permission-based visibility
- [x] Secret reference handling with redaction
- [x] English and German localization
- [x] Navigation menu integration
- [x] **Backend service integration for test connection**
- [x] **Backend service integration for dry-run routing**
- [x] **Async execution for connection tests**
- [x] **Comprehensive error handling and user feedback**
- [ ] Backend service integration for health checks (periodic)
- [ ] Backend service integration for production model invocation