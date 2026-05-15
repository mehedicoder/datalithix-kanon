package ai.datalithix.kanon.airouting.service;

import ai.datalithix.kanon.airouting.model.ChatModelRoute;
import ai.datalithix.kanon.common.TenantContext;
import ai.datalithix.kanon.domain.model.TaskDescriptor;

public interface ModelRouter {
    ChatModelRoute resolve(TenantContext tenantContext, TaskDescriptor taskDescriptor);
}
