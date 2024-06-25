/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;
import com.optio3.util.Exceptions;
import org.apache.commons.lang3.StringUtils;

public final class BACnetDeviceAddress implements Comparable<BACnetDeviceAddress>
{
    public final int networkNumber;
    public final int instanceNumber;

    @JsonCreator
    public BACnetDeviceAddress(@JsonProperty("networkNumber") int networkNumber,
                               @JsonProperty("instanceNumber") int instanceNumber)
    {
        validateNetworkNumber(networkNumber);
        validateInstanceNumber(instanceNumber);

        this.networkNumber = networkNumber;
        this.instanceNumber = instanceNumber;
    }

    //--//

    @Override
    public boolean equals(Object o)
    {
        BACnetDeviceAddress that = Reflection.as(o, BACnetDeviceAddress.class);
        if (that == null)
        {
            return false;
        }

        return (networkNumber == that.networkNumber && instanceNumber == that.instanceNumber);
    }

    @Override
    public int hashCode()
    {
        int result = networkNumber;
        result = 31 * result + instanceNumber;
        return result;
    }

    @Override
    public int compareTo(BACnetDeviceAddress o)
    {
        return compare(this, o);
    }

    public static int compare(BACnetDeviceAddress o1,
                              BACnetDeviceAddress o2)
    {
        if (o1 == o2)
        {
            return 0;
        }

        if (o1 == null)
        {
            return 1;
        }

        if (o2 == null)
        {
            return -1;
        }

        int diff = Integer.compare(o1.networkNumber, o2.networkNumber);
        if (diff == 0)
        {
            diff = Integer.compare(o1.instanceNumber, o2.instanceNumber);
        }

        return diff;
    }

    @Override
    public String toString()
    {
        return networkNumber + "/" + instanceNumber;
    }

    //--//

    @JsonIgnore
    public boolean isWildcard()
    {
        return instanceNumber == BACnetObjectIdentifier.MAX_INSTANCE_NUMBER;
    }

    @JsonIgnore
    public boolean hasValidInstanceNumber()
    {
        return instanceNumber > 0;
    }

    public static BACnetDeviceAddress fromString(String value,
                                                 String separator)
    {
        try
        {
            String[] parts = StringUtils.split(value, BoxingUtils.get(separator, "/"));
            if (parts.length == 2)
            {
                return new BACnetDeviceAddress(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            }
        }
        catch (Throwable e)
        {
            // Any failure means bad format;
        }

        return null;
    }

    //--//

    private static void validateInstanceNumber(int instance_number)
    {
        if (instance_number >= 0 && instance_number < (1 << 22))
        {
            return;
        }

        throw Exceptions.newIllegalArgumentException("Invalid BACnet Device Instance Number: %d", instance_number);
    }

    private static void validateNetworkNumber(int network_number)
    {
        if (network_number >= 0 && network_number < (1 << 16))
        {
            return;
        }

        throw Exceptions.newIllegalArgumentException("Invalid BACnet Network Number: %d", network_number);
    }
}
