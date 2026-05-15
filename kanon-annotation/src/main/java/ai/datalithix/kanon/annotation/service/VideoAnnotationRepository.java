package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.VideoAnnotation;
import ai.datalithix.kanon.common.service.PagedQueryPort;
import java.util.List;
import java.util.Optional;

public interface VideoAnnotationRepository extends PagedQueryPort<VideoAnnotation> {
    VideoAnnotation save(VideoAnnotation annotation);

    Optional<VideoAnnotation> findById(String tenantId, String annotationId);

    List<VideoAnnotation> findByMediaAssetId(String tenantId, String mediaAssetId);

    List<VideoAnnotation> findByCaseId(String tenantId, String caseId);
}
