package ai.datalithix.kanon.ingestion.service;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kanon.connectors")
public class V1ConnectorProperties {
    private boolean uploadEnabled = true;
    private boolean emailEnabled = true;
    private boolean restWebhookEnabled = true;
    private boolean databaseImportEnabled = true;
    private boolean objectStorageEnabled = true;
    private boolean manualEntryEnabled = true;
    private Map<String, String> upload = Map.of();
    private Map<String, String> email = Map.of();
    private Map<String, String> restWebhook = Map.of();
    private Map<String, String> databaseImport = Map.of();
    private Map<String, String> objectStorage = Map.of();
    private Map<String, String> manualEntry = Map.of();

    public boolean isUploadEnabled() { return uploadEnabled; }
    public void setUploadEnabled(boolean uploadEnabled) { this.uploadEnabled = uploadEnabled; }
    public boolean isEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(boolean emailEnabled) { this.emailEnabled = emailEnabled; }
    public boolean isRestWebhookEnabled() { return restWebhookEnabled; }
    public void setRestWebhookEnabled(boolean restWebhookEnabled) { this.restWebhookEnabled = restWebhookEnabled; }
    public boolean isDatabaseImportEnabled() { return databaseImportEnabled; }
    public void setDatabaseImportEnabled(boolean databaseImportEnabled) { this.databaseImportEnabled = databaseImportEnabled; }
    public boolean isObjectStorageEnabled() { return objectStorageEnabled; }
    public void setObjectStorageEnabled(boolean objectStorageEnabled) { this.objectStorageEnabled = objectStorageEnabled; }
    public boolean isManualEntryEnabled() { return manualEntryEnabled; }
    public void setManualEntryEnabled(boolean manualEntryEnabled) { this.manualEntryEnabled = manualEntryEnabled; }
    public Map<String, String> getUpload() { return upload; }
    public void setUpload(Map<String, String> upload) { this.upload = upload; }
    public Map<String, String> getEmail() { return email; }
    public void setEmail(Map<String, String> email) { this.email = email; }
    public Map<String, String> getRestWebhook() { return restWebhook; }
    public void setRestWebhook(Map<String, String> restWebhook) { this.restWebhook = restWebhook; }
    public Map<String, String> getDatabaseImport() { return databaseImport; }
    public void setDatabaseImport(Map<String, String> databaseImport) { this.databaseImport = databaseImport; }
    public Map<String, String> getObjectStorage() { return objectStorage; }
    public void setObjectStorage(Map<String, String> objectStorage) { this.objectStorage = objectStorage; }
    public Map<String, String> getManualEntry() { return manualEntry; }
    public void setManualEntry(Map<String, String> manualEntry) { this.manualEntry = manualEntry; }
}
