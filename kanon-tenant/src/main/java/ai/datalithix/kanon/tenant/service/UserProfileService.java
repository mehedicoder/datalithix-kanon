package ai.datalithix.kanon.tenant.service;

import ai.datalithix.kanon.tenant.model.UserAccount;

public interface UserProfileService {
    UserAccount currentUser();
    String effectiveLocale();
    UserAccount updatePreferredLocale(String preferredLocale);
}
