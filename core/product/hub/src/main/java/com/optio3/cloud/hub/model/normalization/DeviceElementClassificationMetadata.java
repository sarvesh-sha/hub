/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.normalization;

import java.util.List;

import com.google.common.collect.Sets;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.protocol.model.EngineeringUnits;

public class DeviceElementClassificationMetadata
{
    public String           pointName;
    public Boolean          pointIgnore;
    public Double           positiveScore;
    public Double           negativeScore;
    public Double           pointClassThreshold;
    public String           pointClassId;
    public List<String>     pointClassTags;
    public EngineeringUnits assignedUnits;

    public List<NormalizationEquipmentLocation> locations;

    public static DeviceElementClassificationMetadata fromMetadata(MetadataMap metadata)
    {
        DeviceElementClassificationMetadata classification = new DeviceElementClassificationMetadata();
        classification.pointClassId        = AssetRecord.WellKnownMetadata.pointClassID.get(metadata);
        classification.pointIgnore         = AssetRecord.WellKnownMetadata.pointIgnore.get(metadata);
        classification.positiveScore       = AssetRecord.WellKnownMetadata.pointClassScore.get(metadata);
        classification.negativeScore       = AssetRecord.WellKnownMetadata.negativePointClassScore.get(metadata);
        classification.pointClassThreshold = AssetRecord.WellKnownMetadata.pointClassThreshold.get(metadata);
        classification.pointClassTags      = AssetRecord.WellKnownMetadata.pointClassTags.get(metadata);
        classification.assignedUnits       = AssetRecord.WellKnownMetadata.assignedUnits.get(metadata);

        NormalizationEquipmentLocations locationWrapper = AssetRecord.WellKnownMetadata.locationsWithType.get(metadata);
        classification.locations = NormalizationEquipmentLocations.unwrap(locationWrapper);

        return classification;
    }

    public void saveToMetadata(MetadataMap metadata)
    {
        AssetRecord.WellKnownMetadata.pointClassID.put(metadata, pointClassId);
        AssetRecord.WellKnownMetadata.locationsWithType.put(metadata, NormalizationEquipmentLocations.wrap(locations));
        AssetRecord.WellKnownMetadata.pointIgnore.put(metadata, pointIgnore);
        AssetRecord.WellKnownMetadata.pointClassScore.put(metadata, positiveScore);
        AssetRecord.WellKnownMetadata.negativePointClassScore.put(metadata, negativeScore);
        AssetRecord.WellKnownMetadata.pointClassThreshold.put(metadata, pointClassThreshold);
        AssetRecord.WellKnownMetadata.pointClassTags.put(metadata, pointClassTags);
        AssetRecord.WellKnownMetadata.assignedUnits.put(metadata, assignedUnits);

        updateTags(metadata);
    }

    public static void updateTags(MetadataMap metadata)
    {
        String  pointClassId = AssetRecord.WellKnownMetadata.pointClassID.get(metadata);
        boolean pointIgnored = AssetRecord.WellKnownMetadata.pointIgnore.getOrDefault(metadata, false);
        double  positive     = AssetRecord.WellKnownMetadata.pointClassScore.getOrDefault(metadata, 0.0);
        double  negative     = AssetRecord.WellKnownMetadata.negativePointClassScore.getOrDefault(metadata, 0.0);
        double  threshold    = AssetRecord.WellKnownMetadata.pointClassThreshold.getOrDefault(metadata, 2.0);
        boolean classified   = pointClassId != null && !pointIgnored && (positive + negative) >= threshold;

        metadata.modifyTags(AssetRecord.WellKnownMetadata.tags, (tags) ->
        {
            if (classified)
            {
                tags.setValuesForTag(AssetRecord.WellKnownTags.pointClassId, Sets.newHashSet(pointClassId));
            }
            else
            {
                tags.removeTag(AssetRecord.WellKnownTags.pointClassId);
            }
        });
    }
}
