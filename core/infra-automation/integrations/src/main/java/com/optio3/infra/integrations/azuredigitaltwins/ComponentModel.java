/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.integrations.azuredigitaltwins;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.optio3.infra.integrations.azuredigitaltwins.schema.BaseSchemaModel;

public class ComponentModel extends BaseModelWithName
{
    public BaseSchemaModel schema;
    public boolean         writable;

    public static ComponentModel tryParse(Map<String, BaseSchemaModel> lookupSchema,
                                          JsonNode node)
    {
        List<String> semanticTypes = matchType(node, "Component");
        if (semanticTypes != null)
        {
            ComponentModel res = new ComponentModel();
            res.semanticTypes = semanticTypes.isEmpty() ? null : semanticTypes;

            res.name     = getTextField(node, "name");
            res.writable = getBooleanField(node, "writable");
            res.schema   = BaseSchemaModel.tryParse(lookupSchema, selectPotentiallyQualifiedNode(node, "schema"));

            return res;
        }

        return null;
    }
}