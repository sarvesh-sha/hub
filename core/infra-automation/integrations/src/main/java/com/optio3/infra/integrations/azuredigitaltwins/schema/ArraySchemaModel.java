/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.integrations.azuredigitaltwins.schema;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.optio3.infra.integrations.azuredigitaltwins.InterfaceModel;

public class ArraySchemaModel extends BaseSchemaModel
{
    public BaseSchemaModel elementSchema;

    @Override
    protected void resolveImpl(Map<String, InterfaceModel> interfaces)
    {
        BaseSchemaModel.resolve(elementSchema, interfaces);
    }

    public static ArraySchemaModel tryParse(Map<String, BaseSchemaModel> lookupSchema,
                                            JsonNode node)
    {
        List<String> semanticTypes = matchType(node, "Array");
        if (semanticTypes != null)
        {
            ArraySchemaModel res = new ArraySchemaModel();
            res.semanticTypes = semanticTypes.isEmpty() ? null : semanticTypes;

            res.id            = getTextField(node, "@id");
            res.elementSchema = BaseSchemaModel.tryParse(lookupSchema, node.get("elementSchema"));
            return res;
        }

        return null;
    }
}
