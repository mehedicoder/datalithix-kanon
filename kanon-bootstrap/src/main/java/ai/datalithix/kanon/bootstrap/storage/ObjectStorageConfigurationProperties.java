package ai.datalithix.kanon.bootstrap.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kanon.object-storage")
public class ObjectStorageConfigurationProperties {
    private boolean enabled;
    private String provider = "s3-compatible";
    private String endpoint;
    private String region = "us-east-1";
    private String bucket = "kanon-assets";
    private String accessKey;
    private String secretKey;
    private boolean pathStyleAccess = true;
    private String tenantPrefixTemplate = "tenants/{tenantId}";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public boolean isPathStyleAccess() {
        return pathStyleAccess;
    }

    public void setPathStyleAccess(boolean pathStyleAccess) {
        this.pathStyleAccess = pathStyleAccess;
    }

    public String getTenantPrefixTemplate() {
        return tenantPrefixTemplate;
    }

    public void setTenantPrefixTemplate(String tenantPrefixTemplate) {
        this.tenantPrefixTemplate = tenantPrefixTemplate;
    }
}
