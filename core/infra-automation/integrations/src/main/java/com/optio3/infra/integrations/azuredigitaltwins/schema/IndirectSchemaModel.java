/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.integrations.azuredigitaltwins.schema;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.optio3.infra.integrations.azuredigitaltwins.InterfaceModel;
import com.optio3.util.Exceptions;

public class IndirectSchemaModel extends BaseSchemaModel
{
    public String indirectSchema;

    @JsonIgnore
    public InterfaceModel resolvedSchema;

    @Override
    protected void resolveImpl(Map<String, InterfaceModel> interfaces)
    {
        resolvedSchema = interfaces.get(indirectSchema);
        if (resolvedSchema == null)
        {
            throw Exceptions.newIllegalArgumentException("Cannot resolve schema '%s'", indirectSchema);
        }
    }
}
