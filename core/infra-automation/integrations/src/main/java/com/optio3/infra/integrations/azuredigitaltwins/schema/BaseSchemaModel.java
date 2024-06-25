/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.integrations.azuredigitaltwins.schema;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.optio3.infra.integrations.azuredigitaltwins.BaseModel;
import com.optio3.infra.integrations.azuredigitaltwins.InterfaceModel;
import com.optio3.util.Exceptions;

public abstract class BaseSchemaModel extends BaseModel
{
    public  String  id;
    private boolean m_resolved;

    public static BaseSchemaModel tryParse(Map<String, BaseSchemaModel> lookupSchema,
                                           JsonNode node)
    {
        BaseSchemaModel model;

        model = PrimitiveSchemaModel.tryParse(lookupSchema, node);
        if (model != null)
        {
            return model;
        }

        model = EnumSchemaModel.tryParse(lookupSchema, node);
        if (model != null)
        {
            return model;
        }

        model = ObjectSchemaModel.tryParse(lookupSchema, node);
        if (model != null)
        {
            return model;
        }

        model = ArraySchemaModel.tryParse(lookupSchema, node);
        if (model != null)
        {
            return model;
        }

        model = MapSchemaModel.tryParse(lookupSchema, node);
        if (model != null)
        {
            return model;
        }

        throw Exceptions.newIllegalArgumentException("Unable to decode schema: %s", node);
    }

    //--//

    public static void resolve(BaseSchemaModel schema,
                               Map<String, InterfaceModel> interfaces)
    {
        if (schema != null)
        {
            schema.resolve(interfaces);
        }
    }

    public final void resolve(Map<String, InterfaceModel> interfaces)
    {
        if (!m_resolved)
        {
            m_resolved = true;
            resolveImpl(interfaces);
        }
    }

    protected abstract void resolveImpl(Map<String, InterfaceModel> interfaces);
}
