package ai.datalithix.kanon.airouting.service;

import ai.datalithix.kanon.airouting.model.ModelInvocationRequest;
import ai.datalithix.kanon.airouting.model.ModelInvocationResult;

public interface ModelInvocationService {
    ModelInvocationResult invoke(ModelInvocationRequest request);
}
