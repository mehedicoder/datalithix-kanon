package ai.datalithix.kanon.annotation.service.cvat;

public interface CvatClient {
    CvatTaskRef createTask(CvatTaskRequest request);

    CvatTaskResult fetchResult(String externalTaskId);
}
