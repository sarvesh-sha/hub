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

public class MapSchemaModel extends BaseSchemaModel
{
    public static class Pair
    {
        public String          name;
        public BaseSchemaModel schema;

        Pair(Map<String, BaseSchemaModel> lookupSchema,
             JsonNode node)
        {
            name   = getTextField(node, "name");
            schema = BaseSchemaModel.tryParse(lookupSchema, node.get("schema"));
        }

        public static void resolve(Pair pair,
                                   Map<String, InterfaceModel> interfaces)
        {
            if (pair != null)
            {
                BaseSchemaModel.resolve(pair.schema, interfaces);
            }
        }
    }

    public Pair key;
    public Pair value;

    @Override
    protected void resolveImpl(Map<String, InterfaceModel> interfaces)
    {
        Pair.resolve(key, interfaces);
        Pair.resolve(value, interfaces);
    }

    public static MapSchemaModel tryParse(Map<String, BaseSchemaModel> lookupSchema,
                                          JsonNode node)
    {
        List<String> semanticTypes = matchType(node, "Map");
        if (semanticTypes != null)
        {
            MapSchemaModel res = new MapSchemaModel();
            res.semanticTypes = semanticTypes.isEmpty() ? null : semanticTypes;

            res.key   = new Pair(lookupSchema, selectPotentiallyQualifiedNode(node, "mapKey"));
            res.value = new Pair(lookupSchema, selectPotentiallyQualifiedNode(node, "mapValue"));
            return res;
        }

        return null;
    }
}
