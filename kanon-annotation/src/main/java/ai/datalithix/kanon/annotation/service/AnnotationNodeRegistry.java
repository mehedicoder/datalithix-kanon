package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.AnnotationExecutionNodeType;
import ai.datalithix.kanon.annotation.model.AnnotationTask;
import java.util.List;
import java.util.Optional;

public interface AnnotationNodeRegistry {
    List<AnnotationNode> nodes();

    Optional<AnnotationNode> findById(String nodeId);

    Optional<AnnotationNode> findByType(AnnotationExecutionNodeType nodeType);

    Optional<AnnotationNode> selectNode(AnnotationTask task);
}
