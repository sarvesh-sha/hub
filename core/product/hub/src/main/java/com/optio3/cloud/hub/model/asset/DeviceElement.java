/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;

@JsonTypeName("DeviceElement")
@JsonSubTypes({ @JsonSubTypes.Type(value = MetricsDeviceElement.class) })
public class DeviceElement extends Asset
{
    @Optio3MapAsReadOnly
    public String identifier;

    @Optio3MapAsReadOnly
    public JsonNode contents;

    @Optio3MapAsReadOnly
    public JsonNode desiredContents;

    @Optio3MapAsReadOnly
    public boolean ableToUpdateState;

    //--//

    public List<DeviceElementSampling> samplingSettings;

    //--//

    @Override
    public AssetRecord newRecord()
    {
        return new DeviceElementRecord();
    }
}
