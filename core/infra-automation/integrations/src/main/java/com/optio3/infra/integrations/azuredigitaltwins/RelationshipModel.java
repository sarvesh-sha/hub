/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.integrations.azuredigitaltwins;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import com.optio3.infra.integrations.azuredigitaltwins.schema.BaseSchemaModel;

public class RelationshipModel extends BaseModelWithName
{
    public String                     target;
    public Map<String, PropertyModel> properties;

    public static RelationshipModel tryParse(Map<String, BaseSchemaModel> lookupSchema,
                                             JsonNode node)
    {
        List<String> semanticTypes = matchType(node, "Relationship");
        if (semanticTypes != null)
        {
            RelationshipModel res = new RelationshipModel();
            res.semanticTypes = semanticTypes.isEmpty() ? null : semanticTypes;

            res.name   = getTextField(node, "name");
            res.target = getTextField(node, "target");

            JsonNode nodeProperties = selectPotentiallyQualifiedNode(node, "properties");
            if (nodeProperties != null)
            {
                if (nodeProperties instanceof ObjectNode)
                {
                    res.parseProperty(lookupSchema, nodeProperties);
                }
                else
                {
                    for (JsonNode nodeProperty : nodeProperties)
                    {
                        res.parseProperty(lookupSchema, nodeProperty);
                    }
                }
            }

            return res;
        }

        return null;
    }

    private void parseProperty(Map<String, BaseSchemaModel> lookupSchema,
                               JsonNode node)
    {
        PropertyModel prop = PropertyModel.tryParse(lookupSchema, node);
        if (prop != null)
        {
            if (properties == null)
            {
                properties = Maps.newHashMap();
            }

            properties.put(prop.name, prop);
        }
    }
}
