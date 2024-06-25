/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.core.block.EngineOperatorUnary;

@JsonSubTypes({ @JsonSubTypes.Type(value = AlertEngineOperatorUnaryAsControlPoint.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryAsDevice.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryAsGroup.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryAssetGetLocation.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryAssetGetName.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryAssetQueryExec.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryAssetQueryNot.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryAssetQueryRelation.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryAssetQueryRelationSingle.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryControlPointCoordinates.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryControlPointLastSample.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryControlPointMetadataNumber.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryControlPointMetadataString.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryControlPointMetadataTimestamp.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryControlPointNewSamples.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryCoordinatesNewSamples.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryCreateAlert.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryGetAlert.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryGetAlertSeverity.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryGetAlertStatus.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryGetEmailDeliveryOptionsFromLocation.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryGetProperty.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryGetSmsDeliveryOptionsFromLocation.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryGetTimeZoneFromLocation.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryHasAlertChanged.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryLocationGetName.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnarySampleGetProperty.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnarySampleGetTime.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryTravelEntryGetTime.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryTravelEntryInsideFence.class) })
public abstract class EngineOperatorUnaryFromAlerts<To extends EngineValue, Ti extends EngineValue> extends EngineOperatorUnary<To, Ti>
{
    protected EngineOperatorUnaryFromAlerts(Class<To> resultType)
    {
        super(resultType);
    }
}
