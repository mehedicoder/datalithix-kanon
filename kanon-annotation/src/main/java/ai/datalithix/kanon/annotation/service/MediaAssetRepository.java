package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.MediaAsset;
import ai.datalithix.kanon.common.service.PagedQueryPort;
import java.util.List;
import java.util.Optional;

public interface MediaAssetRepository extends PagedQueryPort<MediaAsset> {
    MediaAsset save(MediaAsset mediaAsset);

    Optional<MediaAsset> findById(String tenantId, String mediaAssetId);

    List<MediaAsset> findByCaseId(String tenantId, String caseId);

    List<MediaAsset> findBySourceTraceId(String tenantId, String sourceTraceId);
}
