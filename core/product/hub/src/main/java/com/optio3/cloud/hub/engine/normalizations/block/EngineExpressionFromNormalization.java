/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.core.type.TypeReference;
import com.optio3.cloud.hub.engine.EngineExpression;
import com.optio3.cloud.hub.engine.EngineValue;

@JsonSubTypes({ @JsonSubTypes.Type(value = NormalizationEngineExpressionCreateChildEquipment.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionCreateEquipment.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetControllerBackupName.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetControllerDescription.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetControllerIdentifier.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetControllerInSubnet.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetControllerName.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetControllers.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetControllerLocation.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetControllerModel.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetControllerVendor.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetControlPointClass.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetControlPointDescription.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetControlPointIdentifier.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetControlPointName.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetControlPointNameRaw.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetControlPointOverrideName.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetControlPointLocation.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetControlPointUnits.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetControlPointUnitsString.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetDashboardName.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetDashboardEquipmentName.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetEquipment.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetEquipmentClass.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetEquipments.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetImportedStructure.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetInputValue.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetLocation.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetMetadataNumber.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetMetadataString.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionGetTags.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionPushEquipment.class),
                @JsonSubTypes.Type(value = NormalizationEngineExpressionPushEquipmentTable.class),
                @JsonSubTypes.Type(value = NormalizationEngineOperatorBinaryScoreTopDocument.class) })
public abstract class EngineExpressionFromNormalization<T extends EngineValue> extends EngineExpression<T>
{
    protected EngineExpressionFromNormalization(Class<T> resultType)
    {
        super(resultType);
    }

    protected EngineExpressionFromNormalization(TypeReference<T> resultType)
    {
        super(resultType);
    }
}
