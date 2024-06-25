/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.metadata.normalization;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.transport.TransportAddress;
import com.optio3.serialization.Reflection;

@JsonTypeName("BACnetImportExportData")
@JsonSubTypes({ @JsonSubTypes.Type(value = BACnetBulkRenamingData.class) })
public class BACnetImportExportData extends ImportExportData
{
    public int                    networkId;
    public int                    instanceId;
    public BACnetObjectIdentifier objectId;
    public TransportAddress       transport;

    //--//

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        BACnetImportExportData that = Reflection.as(o, BACnetImportExportData.class);
        if (that == null)
        {
            return false;
        }

        return compareTo(that, false) == 0;
    }

    @Override
    public int hashCode()
    {
        int result = 1;

        result = 31 * result + Integer.hashCode(networkId);
        result = 31 * result + Integer.hashCode(instanceId);
        result = 31 * result + Objects.hashCode(objectId);

        return result;
    }

    @Override
    public int compareTo(ImportExportData o,
                         boolean fuzzy)
    {
        BACnetImportExportData that = Reflection.as(o, BACnetImportExportData.class);
        if (that != null)
        {
            int diff = fuzzy ? 0 : Integer.compare(this.networkId, that.networkId);
            if (diff == 0)
            {
                diff = Integer.compare(this.instanceId, that.instanceId);
                if (diff == 0)
                {
                    diff = BACnetObjectIdentifier.compare(this.objectId, that.objectId);
                }
            }

            return diff;
        }

        return 0;
    }
}
