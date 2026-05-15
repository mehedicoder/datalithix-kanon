package ai.datalithix.kanon.annotation.service.labelstudio;

public interface LabelStudioClient {
    LabelStudioTaskRef createTask(LabelStudioTaskRequest request);

    LabelStudioTaskResult fetchResult(String externalTaskId);
}
