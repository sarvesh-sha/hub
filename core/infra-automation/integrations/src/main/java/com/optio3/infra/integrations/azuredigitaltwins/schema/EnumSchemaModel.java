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

public class EnumSchemaModel extends BaseSchemaModel
{
    public BaseSchemaModel     valueSchema;
    public Map<String, String> values = Maps.newHashMap();

    @Override
    protected void resolveImpl(Map<String, InterfaceModel> interfaces)
    {
        BaseSchemaModel.resolve(valueSchema, interfaces);
    }

    public static EnumSchemaModel tryParse(Map<String, BaseSchemaModel> lookupSchema,
                                           JsonNode node)
    {
        List<String> semanticTypes = matchType(node, "Enum");
        if (semanticTypes != null)
        {
            EnumSchemaModel res = new EnumSchemaModel();
            res.semanticTypes = semanticTypes.isEmpty() ? null : semanticTypes;

            res.id = getTextField(node, "@id");
            if (res.id != null)
            {
                lookupSchema.put(res.id, res);
            }

            res.valueSchema = PrimitiveSchemaModel.tryParse(lookupSchema, selectPotentiallyQualifiedNode(node, "valueSchema"));

            for (JsonNode enumValue : selectPotentiallyQualifiedNode(node, "enumValues"))
            {
                String name  = getFieldAsText(enumValue, "name");
                String value = getFieldAsText(enumValue, "enumValue");

                res.values.put(name, value);
            }

            return res;
        }

        return null;
    }
}
