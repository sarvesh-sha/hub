/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.integrations.azuredigitaltwins.schema;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import com.optio3.infra.integrations.azuredigitaltwins.InterfaceModel;

public class ObjectSchemaModel extends BaseSchemaModel
{
    public Map<String, BaseSchemaModel> fields = Maps.newHashMap();

    @Override
    protected void resolveImpl(Map<String, InterfaceModel> interfaces)
    {
        for (BaseSchemaModel field : fields.values())
        {
            field.resolve(interfaces);
        }
    }

    public static ObjectSchemaModel tryParse(Map<String, BaseSchemaModel> lookupSchema,
                                             JsonNode node)
    {
        List<String> semanticTypes = matchType(node, "Object");
        if (semanticTypes != null)
        {
            ObjectSchemaModel res = new ObjectSchemaModel();
            res.semanticTypes = semanticTypes.isEmpty() ? null : semanticTypes;

            res.id = getTextField(node, "@id");
            if (res.id != null)
            {
                lookupSchema.put(res.id, res);
            }

            for (JsonNode fieldValue : node.get("fields"))
            {
                String          name  = getFieldAsText(fieldValue, "name");
                BaseSchemaModel value = BaseSchemaModel.tryParse(lookupSchema, fieldValue.get("schema"));

                res.fields.put(name, value);
            }

            return res;
        }

        return null;
    }
}
