/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.value;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionContext;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.LogicalAssetRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("AlertEngineValueAsset")
@JsonSubTypes({ @JsonSubTypes.Type(value = AlertEngineValueControlPoint.class),
                @JsonSubTypes.Type(value = AlertEngineValueDevice.class),
                @JsonSubTypes.Type(value = AlertEngineValueLocation.class),
                @JsonSubTypes.Type(value = AlertEngineValueLogicalAsset.class) })
public class AlertEngineValueAsset<T extends AssetRecord> extends EngineValue
{
    public TypedRecordIdentity<T> record;

    //--//

    @SuppressWarnings("unchecked")
    public static <T extends AssetRecord, V extends AlertEngineValueAsset<T>> V create(AlertEngineExecutionContext ctx,
                                                                                       TypedRecordIdentity<? extends AssetRecord> ri)
    {
        ri = ctx.resolveAssetIdentity(ri);
        if (!TypedRecordIdentity.isValid(ri))
        {
            return null;
        }

        TypedRecordIdentity<DeviceElementRecord> ri_de = ri.as(DeviceElementRecord.class);
        if (ri_de != null)
        {
            return (V) AlertEngineValueControlPoint.createTyped(ri_de);
        }

        TypedRecordIdentity<DeviceRecord> ri_d = ri.as(DeviceRecord.class);
        if (ri_d != null)
        {
            return (V) AlertEngineValueDevice.createTyped(ri_d);
        }

        TypedRecordIdentity<LocationRecord> ri_l = ri.as(LocationRecord.class);
        if (ri_l != null)
        {
            return (V) AlertEngineValueLocation.createTyped(ri_l);
        }

        TypedRecordIdentity<LogicalAssetRecord> ri_group = ri.as(LogicalAssetRecord.class);
        if (ri_group != null)
        {
            return (V) AlertEngineValueLogicalAsset.createTyped(ri_group);
        }

        AlertEngineValueAsset<T> res = new AlertEngineValueAsset<>();
        res.record = (TypedRecordIdentity<T>) ri;
        return (V) res;
    }

    //--//

    @Override
    public int compareTo(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         EngineValue o)
    {
        AlertEngineValueAsset other = Reflection.as(o, AlertEngineValueAsset.class);
        if (other != null)
        {
            return StringUtils.compare(record.sysId, other.record.sysId);
        }

        throw stack.unexpected();
    }

    @Override
    public String format(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         Map<String, String> modifiers)
    {
        AlertEngineExecutionContext ctx2 = (AlertEngineExecutionContext) ctx;

        String name = ctx2.accessAsset(record, AssetRecord::getName);
        if (name != null)
        {
            return String.format("'%s (%s)'", name, record.sysId);
        }
        else
        {
            return String.format("'(%s)'", record.sysId);
        }
    }
}
