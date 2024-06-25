/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionContext;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueControlPoint;
import com.optio3.cloud.hub.engine.core.value.EngineValueDateTimeRange;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveNumber;
import com.optio3.cloud.hub.model.ControlPointsSelection;
import com.optio3.cloud.hub.model.alert.AlertSampleAggregate;
import com.optio3.cloud.hub.model.dashboard.AggregationRequest;
import com.optio3.cloud.hub.model.dashboard.enums.AggregationTypeId;
import com.optio3.cloud.hub.model.schedule.FilterableTimeRange;
import com.optio3.cloud.hub.model.visualization.RangeSelection;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;

@JsonTypeName("AlertEngineExpressionBinaryControlPointSampleAggregate")
public class AlertEngineExpressionBinaryControlPointSampleAggregate extends EngineOperatorBinaryFromAlerts<EngineValuePrimitiveNumber, AlertEngineValueControlPoint, EngineValueDateTimeRange>
{
    public AlertSampleAggregate    aggregate;
    public EngineeringUnitsFactors unitsFactors;

    // TODO: UPGRADE PATCH: Legacy fixup for field type change.
    public void setUnit(EngineeringUnits unit)
    {
        HubApplication.reportPatchCall(unit);

        this.unitsFactors = EngineeringUnitsFactors.get(unit);
    }

    //--//

    public AlertEngineExpressionBinaryControlPointSampleAggregate()
    {
        super(EngineValuePrimitiveNumber.class);
    }

    @Override
    protected EngineValuePrimitiveNumber computeResult(EngineExecutionContext<?, ?> ctx,
                                                       EngineExecutionStack stack,
                                                       AlertEngineValueControlPoint controlPoint,
                                                       EngineValueDateTimeRange range)
    {
        stack.checkNonNullValue(controlPoint, "No Control Point");
        stack.checkNonNullValue(range, "No Time Range");

        AlertEngineExecutionContext                 ctx2  = (AlertEngineExecutionContext) ctx;
        AlertEngineExecutionContext.SamplesSnapshot cache = ctx2.getSamples(controlPoint.record);

        AggregationRequest req = new AggregationRequest();

        switch (aggregate)
        {
            case min:
                req.aggregationType = AggregationTypeId.MIN;
                break;

            case max:
                req.aggregationType = AggregationTypeId.MAX;
                break;

            case average:
                req.aggregationType = AggregationTypeId.MEAN;
                break;

            case delta:
                req.aggregationType = AggregationTypeId.DELTA;
                break;

            case increment:
                req.aggregationType = AggregationTypeId.INCREASE;
                break;

            case decrement:
                req.aggregationType = AggregationTypeId.DECREASE;
                break;

            default:
                return null;
        }

        FilterableTimeRange currentPeriod = new FilterableTimeRange();
        currentPeriod.range = RangeSelection.buildFixed(range.start, range.end);

        req.filterableRanges = Lists.newArrayList(currentPeriod);
        req.prop             = BACnetPropertyIdentifier.present_value.name();
        req.unitsFactors     = unitsFactors;

        double result = cache.computeAggregation(req);
        return EngineValuePrimitiveNumber.create(result);
    }
}
