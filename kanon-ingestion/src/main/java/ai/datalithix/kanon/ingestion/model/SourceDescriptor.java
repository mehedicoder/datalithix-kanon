package ai.datalithix.kanon.ingestion.model;

import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DataResidency;
import ai.datalithix.kanon.common.SourceCategory;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.common.compliance.ComplianceClassification;
import ai.datalithix.kanon.common.compliance.DataClassification;

public record SourceDescriptor(
        SourceCategory sourceCategory,
        SourceType sourceType,
        String sourceSystem,
        String sourceIdentifier,
        String sourceUri,
        AssetType assetType,
        DataClassification dataClassification,
        ComplianceClassification complianceClassification,
        DataResidency dataResidency,
        String retentionPolicy,
        String consentRef
) {}
