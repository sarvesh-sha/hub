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

public class EquipmentClassificationMetadata
{
    public String       equipmentKey;
    public String       equipmentClassId;
    public List<String> equipmentClassTags;

    public static EquipmentClassificationMetadata fromMetadata(MetadataMap metadata)
    {
        EquipmentClassificationMetadata classification = new EquipmentClassificationMetadata();
        classification.equipmentKey       = AssetRecord.WellKnownMetadata.equipmentKey.get(metadata);
        classification.equipmentClassId   = AssetRecord.WellKnownMetadata.equipmentClassID.get(metadata);
        classification.equipmentClassTags = AssetRecord.WellKnownMetadata.equipmentClassTags.get(metadata);
        return classification;
    }

    public void saveToMetadata(MetadataMap metadata)
    {
        AssetRecord.WellKnownMetadata.equipmentKey.put(metadata, equipmentKey);
        AssetRecord.WellKnownMetadata.equipmentClassID.put(metadata, equipmentClassId);
        AssetRecord.WellKnownMetadata.equipmentClassTags.put(metadata, equipmentClassTags);

        updateTags(metadata);
    }

    public static void updateTags(MetadataMap metadata)
    {
        String equipmentClassId = AssetRecord.WellKnownMetadata.equipmentClassID.get(metadata);

        metadata.modifyTags(AssetRecord.WellKnownMetadata.tags, (tags) ->
        {
            tags.addTag(AssetRecord.WellKnownTags.isEquipment, false);

            if (equipmentClassId != null)
            {
                tags.setValuesForTag(AssetRecord.WellKnownTags.equipmentClassId, Sets.newHashSet(equipmentClassId));
            }
            else
            {
                tags.removeTag(AssetRecord.WellKnownTags.equipmentClassId);
            }
        });
    }
}
