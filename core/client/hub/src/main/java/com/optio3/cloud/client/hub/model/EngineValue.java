/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Hub APIs
 * APIs and Definitions for the Optio3 Hub product.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.cloud.client.hub.model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes(
{
    @JsonSubTypes.Type(value = AlertEngineValueAlert.class),
    @JsonSubTypes.Type(value = AlertEngineValueAlertSeverity.class),
    @JsonSubTypes.Type(value = AlertEngineValueAlertStatus.class),
    @JsonSubTypes.Type(value = AlertEngineValueAsset.class),
    @JsonSubTypes.Type(value = AlertEngineValueAssetQueryCondition.class),
    @JsonSubTypes.Type(value = AlertEngineValueAssets.class),
    @JsonSubTypes.Type(value = AlertEngineValueControlPoint.class),
    @JsonSubTypes.Type(value = AlertEngineValueControlPointCoordinates.class),
    @JsonSubTypes.Type(value = AlertEngineValueControlPoints.class),
    @JsonSubTypes.Type(value = AlertEngineValueDeliveryOptions.class),
    @JsonSubTypes.Type(value = AlertEngineValueDevice.class),
    @JsonSubTypes.Type(value = AlertEngineValueEmail.class),
    @JsonSubTypes.Type(value = AlertEngineValueLocation.class),
    @JsonSubTypes.Type(value = AlertEngineValueLogicalAsset.class),
    @JsonSubTypes.Type(value = AlertEngineValueSample.class),
    @JsonSubTypes.Type(value = AlertEngineValueSamples.class),
    @JsonSubTypes.Type(value = AlertEngineValueSms.class),
    @JsonSubTypes.Type(value = AlertEngineValueTicket.class),
    @JsonSubTypes.Type(value = AlertEngineValueTravelEntry.class),
    @JsonSubTypes.Type(value = AlertEngineValueTravelLog.class),
    @JsonSubTypes.Type(value = EngineValueDateTime.class),
    @JsonSubTypes.Type(value = EngineValueDateTimeList.class),
    @JsonSubTypes.Type(value = EngineValueDateTimeRange.class),
    @JsonSubTypes.Type(value = EngineValueDuration.class),
    @JsonSubTypes.Type(value = EngineValueEngineeringUnits.class),
    @JsonSubTypes.Type(value = EngineValueList.class),
    @JsonSubTypes.Type(value = EngineValueListConcrete.class),
    @JsonSubTypes.Type(value = EngineValueLookupTable.class),
    @JsonSubTypes.Type(value = EngineValuePrimitiveBoolean.class),
    @JsonSubTypes.Type(value = EngineValuePrimitiveNumber.class),
    @JsonSubTypes.Type(value = EngineValuePrimitiveString.class),
    @JsonSubTypes.Type(value = EngineValueRegexMatch.class),
    @JsonSubTypes.Type(value = EngineValueRegexReplaceTable.class),
    @JsonSubTypes.Type(value = EngineValueTimeZone.class),
    @JsonSubTypes.Type(value = EngineValueWeeklySchedule.class),
    @JsonSubTypes.Type(value = MetricsEngineSelectValue.class),
    @JsonSubTypes.Type(value = MetricsEngineValueScalar.class),
    @JsonSubTypes.Type(value = MetricsEngineValueSeries.class),
    @JsonSubTypes.Type(value = MetricsEngineValueSetOfSeries.class),
    @JsonSubTypes.Type(value = NormalizationEngineValueController.class),
    @JsonSubTypes.Type(value = NormalizationEngineValueDocument.class),
    @JsonSubTypes.Type(value = NormalizationEngineValueEquipment.class),
    @JsonSubTypes.Type(value = NormalizationEngineValueLocation.class),
    @JsonSubTypes.Type(value = NormalizationEngineValuePoint.class)
})
public class EngineValue
{


  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class EngineValue {\n");

    sb.append("}");
    return sb.toString();
  }

}
