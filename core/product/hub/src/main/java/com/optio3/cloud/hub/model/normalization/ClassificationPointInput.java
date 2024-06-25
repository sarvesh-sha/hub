/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.normalization;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

public class ClassificationPointInput
{
    public String sysId;
    public String parentSysId;
    public String networkSysId;

    public String pointClassOverride;

    public List<NormalizationEquipment> equipmentOverrides;

    public ClassificationPointInputDetails details = new ClassificationPointInputDetails();

    @JsonIgnore
    public EngineeringUnits objectUnits;

    @JsonIgnore
    public BACnetObjectType objectType;

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        ClassificationPointInput that = Reflection.as(o, ClassificationPointInput.class);
        if (that == null)
        {
            return false;
        }

        return StringUtils.equals(sysId, that.sysId);
    }

    @Override
    public int hashCode()
    {
        return sysId.hashCode();
    }

    public ClassificationPointOutput asResult()
    {
        ClassificationPointOutput res = new ClassificationPointOutput();

        res.sysId        = sysId;
        res.parentSysId  = parentSysId;
        res.networkSysId = networkSysId;

        res.objectUnits = objectUnits;
        res.objectType  = objectType;

        res.details.copy(details);

        return res;
    }
}
