/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.montage;

import java.time.ZonedDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import com.optio3.util.BufferUtils;
import org.apache.commons.lang3.StringUtils;

@JsonSubTypes({ @JsonSubTypes.Type(value = BluetoothGateway_TemperatureHumiditySensor.class),
                @JsonSubTypes.Type(value = BluetoothGateway_SmartLock.class),
                @JsonSubTypes.Type(value = BluetoothGateway_PixelTag.class),
                @JsonSubTypes.Type(value = BluetoothGateway_PixelTagRaw.class) })
public abstract class BaseBluetoothGatewayObjectModel extends IpnObjectModel
{
    @JsonIgnore
    public String unitId;

    //--//

    @Override
    protected BaseObjectModel createEmptyCopy()
    {
        BaseBluetoothGatewayObjectModel copy = (BaseBluetoothGatewayObjectModel) super.createEmptyCopy();
        copy.unitId = unitId;
        return copy;
    }

    @Override
    public String extractUnitId()
    {
        if (unitId != null)
        {
            return unitId;
        }

        return super.extractUnitId();
    }

    @Override
    public boolean parseId(String id)
    {
        final String baseId = extractBaseId();
        if (StringUtils.startsWith(id, baseId))
        {
            String[] parts = StringUtils.split(id, '/');
            if (parts.length == 2 && StringUtils.equals(baseId, parts[0]))
            {
                unitId = parts[1];
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean shouldCommitReachabilityChange(boolean isReachable,
                                                  ZonedDateTime lastReachable)
    {
        return false;
    }

    //--//

    public static int extractHex16(Map<String, String> fields,
                                   String key,
                                   boolean littleEndian)
    {
        String val = fields.get(key);
        if (val == null)
        {
            return -1;
        }

        short num = BufferUtils.convertFromHex16(val, 0);
        if (littleEndian)
        {
            num = Short.reverseBytes(num);
        }

        return num & 0xFFFF;
    }

    public static float extractHex16AsFloat(Map<String, String> fields,
                                            String key,
                                            boolean littleEndian)
    {
        int val = extractHex16(fields, key, littleEndian);
        return val >= 0 ? val : Float.NaN;
    }
}
