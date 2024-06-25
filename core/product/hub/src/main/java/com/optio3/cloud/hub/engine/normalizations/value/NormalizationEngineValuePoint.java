/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.value;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.persistence.RecordLocator;

@JsonTypeName("NormalizationEngineValuePoint")
public class NormalizationEngineValuePoint extends EngineValue
{
    public enum PropertyType
    {
        ObjectId,
        Name,
        BackupName,
        Description,
        Location
    }

    public RecordLocator<DeviceElementRecord> locator;
    public String                             objectId;
    public String                             name;
    public String                             backupName;
    public String                             description;
    public String                             location;

    public static NormalizationEngineValuePoint create(RecordLocator<DeviceElementRecord> locator,
                                                       String objectId,
                                                       String name,
                                                       String backupName,
                                                       String description,
                                                       String location)
    {
        NormalizationEngineValuePoint res = new NormalizationEngineValuePoint();
        res.locator     = locator;
        res.objectId    = objectId;
        res.name        = name;
        res.backupName  = backupName;
        res.description = description;
        res.location    = location;
        return res;
    }

    public String getProperty(PropertyType property)
    {
        if (property != null)
        {
            switch (property)
            {
                case ObjectId:
                    return objectId;

                case Name:
                    return name;

                case BackupName:
                    return backupName;

                case Description:
                    return description;

                case Location:
                    return location;
            }
        }

        return "";
    }

    @Override
    public int compareTo(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         EngineValue o)
    {
        throw stack.unexpected();
    }

    @Override
    public String format(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         Map<String, String> modifiers)
    {
        return locator.getIdRaw();
    }
}
