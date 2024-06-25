/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.integrations.azuredigitaltwins;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;

public abstract class BaseModel
{
    public List<String> semanticTypes;

    public static List<String> fromFlatOrList(JsonNode node)
    {
        if (node instanceof TextNode)
        {
            return Lists.newArrayList(node.asText());
        }

        if (node instanceof ArrayNode)
        {
            List<String> res = Lists.newArrayList();

            for (JsonNode nodeChild : node)
            {
                res.add(nodeChild.asText());
            }

            return res;
        }

        return Collections.emptyList();
    }

    public static List<String> matchType(JsonNode node,
                                         String targetType)
    {
        if (node != null)
        {
            List<String> parts = fromFlatOrList(node.get("@type"));
            if (parts.contains(targetType))
            {
                parts.remove(targetType);
                return parts;
            }
        }

        return null;
    }

    public static JsonNode selectPotentiallyQualifiedNode(JsonNode node,
                                                          String name)
    {
        JsonNode childNode = node.get(name);
        if (childNode == null)
        {
            String qualifiedName = "dtmi:dtdl:property:" + name + ";";

            for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); )
            {
                Map.Entry<String, JsonNode> entry = it.next();

                if (entry.getKey()
                         .startsWith(qualifiedName))
                {
                    childNode = entry.getValue();
                    break;
                }
            }
        }

        return childNode;
    }

    public static String getFieldAsText(JsonNode node,
                                        String prop)
    {
        node = selectPotentiallyQualifiedNode(node, prop);
        return node != null ? node.asText() : null;
    }

    public static String getTextField(JsonNode node,
                                      String prop)
    {
        node = selectPotentiallyQualifiedNode(node, prop);
        return node instanceof TextNode ? node.asText() : null;
    }

    public static boolean getBooleanField(JsonNode node,
                                          String prop)
    {
        node = selectPotentiallyQualifiedNode(node, prop);
        return node instanceof BooleanNode && node.asBoolean();
    }
}
