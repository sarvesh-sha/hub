/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.montage;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.ipn.objects.IpnLocation;

@JsonTypeName("Ipn:BluetoothGateway:PixelTagRaw")
public class BluetoothGateway_PixelTagRaw extends BaseBluetoothGatewayObjectModel
{
    public static class RawPayload
    {
        public String      tag;
        public String      lastBridge;
        public byte        rssi;
        public IpnLocation location;
    }

    @FieldModelDescription(description = "Raw Message", units = EngineeringUnits.no_units)
    public String raw;

    //--//

    @Override
    public boolean shouldIncludeObject()
    {
        return super.shouldIncludeObject();
    }

    @Override
    public String extractBaseId()
    {
        return "pixelTagRaw";
    }

    @Override
    public void fillClassificationDetails(ClassificationDetails detailsForGroup,
                                          ClassificationDetails detailsForParent,
                                          ClassificationDetails detailsForPoint)
    {
        // Classification is done through the decoded object.
    }

    public void setRaw(RawPayload raw) throws
                                       JsonProcessingException
    {
        this.raw = getObjectMapperForInstance().writeValueAsString(raw);
    }

    public RawPayload getRaw() throws
                               JsonProcessingException
    {
        return getObjectMapperForInstance().readValue(raw, RawPayload.class);
    }
}
