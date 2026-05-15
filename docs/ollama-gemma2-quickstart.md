# Quick Start: Configuring Gemma2:2b with Ollama

## Overview

This guide shows how to configure Google's Gemma 2 2B model running on Ollama for use in Kanon Platform.

## Prerequisites

- Kanon Platform running
- Admin access to `/admin/models`
- At least 4GB free RAM
- Internet connection (for initial model download)

## Step 1: Install Ollama

### Windows
```powershell
# Download from https://ollama.ai/download/windows
# Run the installer
# Ollama starts automatically
```

### macOS
```bash
# Download from https://ollama.ai/download/mac
# Or use Homebrew:
brew install ollama
```

### Linux
```bash
curl -fsSL https://ollama.ai/install.sh | sh
```

### Verify Installation
```bash
ollama --version
# Output: ollama version 0.x.x
```

## Step 2: Pull Gemma2:2b Model

```bash
# Pull the model (downloads ~1.6GB)
ollama pull gemma2:2b

# Verify it's available
ollama list
# Should show: gemma2:2b

# Test the model
ollama run gemma2:2b "Hello!"
# Should respond with a greeting
```

## Step 3: Configure in Kanon

1. **Navigate to Models Admin**
   - Go to `http://localhost:8080/admin/models`
   - Click "Create model"

2. **Fill in Configuration**

   | Field | Value |
   |-------|-------|
   | Profile Key | `gemma2-2b-local` |
   | Model Name | `Gemma 2 2B (Local)` |
   | Backend Type | `LOCAL_SERVER` |
   | Provider | `Ollama` |
   | Model ID | `gemma2:2b` |
   | Base URL | `http://localhost:11434` |
   | Secret Reference | *(leave empty)* |
   | Task Capabilities | `REASONING`, `CHAT`, `SUMMARIZATION` |
   | Cost Class | `FREE` |
   | Latency Class | `FAST` |
   | Priority | `100` |
   | Supports Tools | ✓ |
   | Supports Structured Output | ✓ |
   | Fallback Profile | *(optional)* |

3. **Click "Create"**

## Step 4: Test the Connection

1. Find your model in the grid
2. Click the **Test connection** icon (🔌)
3. Wait for the test to complete
4. Verify you see:
   - ✅ Status: COMPLETED
   - Response from the model
   - Token usage statistics
   - Latency information

**Expected Result**:
```
Status: COMPLETED
Latency: 2847ms
Response: Connection successful

Metadata:
  inputTokens: 15
  outputTokens: 8
  totalTokens: 23
  finishReason: STOP
```

## Step 5: Test Routing

1. Click the **Dry-run routing** icon (🔀)
2. Configure test:
   - Task Type: `REASONING`
   - Prefer local models: ✓
   - High-risk task: ☐
3. Click "Run dry-run"
4. Verify the model is selected as primary

**Expected Result**:
```
✓ This model IS the PRIMARY choice for this routing scenario
```

## Troubleshooting

### "Connection refused"
```bash
# Start Ollama server
ollama serve

# Verify it's running
curl http://localhost:11434/v1/models
```

### "Model not found"
```bash
# Check model is pulled
ollama list

# If not listed, pull it
ollama pull gemma2:2b
```

### "Timeout" on first request
- First request loads model into memory (10-30 seconds)
- Subsequent requests are much faster (1-5 seconds)
- This is normal behavior

### Wrong model ID
- Use exact name from `ollama list`
- Case-sensitive: `gemma2:2b` not `Gemma2:2b`

## Model Specifications

| Attribute | Value |
|-----------|-------|
| Parameters | 2 billion |
| Download Size | ~1.6GB |
| Memory Usage | ~2GB RAM |
| Context Length | 8,192 tokens |
| Speed | ⚡⚡⚡ Very Fast |
| Quality | ⭐⭐ Good for simple tasks |

## Use Cases

**Good For**:
- ✅ Simple Q&A
- ✅ Text summarization
- ✅ Basic classification
- ✅ Fast prototyping
- ✅ Low-resource environments

**Not Ideal For**:
- ❌ Complex reasoning
- ❌ Long-form content generation
- ❌ Highly technical tasks
- ❌ Multi-step problem solving

## Alternative Models

If gemma2:2b doesn't meet your needs:

```bash
# More capable models (require more RAM):
ollama pull llama3.2:3b      # 3B params, better quality
ollama pull phi3:mini        # 3.8B params, good reasoning
ollama pull mistral:7b       # 7B params, high quality

# Smaller models (less RAM):
ollama pull tinyllama        # 1.1B params, very fast
```

## Next Steps

1. **Configure Multiple Models**: Add llama3.2:3b as fallback
2. **Set Up Routing**: Configure task-specific routing rules
3. **Monitor Performance**: Check latency and token usage
4. **Production Deployment**: Consider dedicated Ollama server

## Resources

- Ollama Documentation: https://ollama.ai/docs
- Gemma Model Card: https://ai.google.dev/gemma
- Kanon Model Admin Guide: `docs/model-admin-test-manual.md`

## Quick Commands Reference

```bash
# Start Ollama
ollama serve

# List models
ollama list

# Pull a model
ollama pull gemma2:2b

# Test a model
ollama run gemma2:2b "Test prompt"

# Check running models
ollama ps

# Remove a model
ollama rm gemma2:2b

# View Ollama logs
ollama logs
```

---

**Last Updated**: 2026-05-03  
**Kanon Version**: 0.1.0-SNAPSHOT