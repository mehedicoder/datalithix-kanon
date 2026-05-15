package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.AnnotationExecutionNodeType;
import ai.datalithix.kanon.annotation.model.AnnotationTask;
import java.util.List;
import java.util.Optional;

public class DefaultAnnotationNodeRegistry implements AnnotationNodeRegistry {
    private final List<AnnotationNode> nodes;

    public DefaultAnnotationNodeRegistry(List<AnnotationNode> nodes) {
        this.nodes = nodes == null ? List.of() : List.copyOf(nodes);
    }

    @Override
    public List<AnnotationNode> nodes() {
        return nodes;
    }

    @Override
    public Optional<AnnotationNode> findById(String nodeId) {
        return nodes.stream()
                .filter(node -> node.descriptor().nodeId().equals(nodeId))
                .findFirst();
    }

    @Override
    public Optional<AnnotationNode> findByType(AnnotationExecutionNodeType nodeType) {
        return nodes.stream()
                .filter(node -> node.descriptor().nodeType() == nodeType)
                .findFirst();
    }

    @Override
    public Optional<AnnotationNode> selectNode(AnnotationTask task) {
        if (task == null) {
            return Optional.empty();
        }
        if (task.preferredNodeType() != null) {
            return nodes.stream()
                    .filter(node -> node.descriptor().nodeType() == task.preferredNodeType())
                    .filter(node -> node.supports(task))
                    .findFirst();
        }
        return nodes.stream()
                .filter(node -> node.supports(task))
                .findFirst();
    }
}
