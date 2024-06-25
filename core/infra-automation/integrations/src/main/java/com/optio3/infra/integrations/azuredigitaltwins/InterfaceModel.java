/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.integrations.azuredigitaltwins;

import java.util.List;
import java.util.Map;

import com.azure.digitaltwins.core.BasicDigitalTwin;
import com.azure.digitaltwins.core.BasicDigitalTwinMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Maps;
import com.optio3.infra.integrations.azuredigitaltwins.schema.BaseSchemaModel;

public class InterfaceModel extends BaseModelWithId
{
    public List<String>                   semanticTypes;
    public String                         context;
    public Map<String, ExtendModel>       superClasses;
    public Map<String, PropertyModel>     properties;
    public Map<String, ComponentModel>    components;
    public Map<String, RelationshipModel> relationships;

    public void ensureComponentsOnTwin(BasicDigitalTwin twin)
    {
        if (components != null)
        {
            Map<String, Object> map = twin.getContents();

            //
            // Create all missing components.
            //
            for (ComponentModel comp : components.values())
            {
                if (!map.containsKey(comp.name))
                {
                    map.put(comp.name, new BasicDigitalTwin(null).setMetadata(new BasicDigitalTwinMetadata()));
                }
            }
        }

        if (superClasses != null)
        {
            for (ExtendModel value : superClasses.values())
            {
                value.target.ensureComponentsOnTwin(twin);
            }
        }
    }

    public static InterfaceModel tryParse(JsonNode node)
    {
        List<String> semanticTypes = matchType(node, "Interface");
        if (semanticTypes != null)
        {
            InterfaceModel res = new InterfaceModel();
            res.semanticTypes = semanticTypes.isEmpty() ? null : semanticTypes;

            res.id      = getTextField(node, "@id");
            res.context = getTextField(node, "@context");

            Map<String, BaseSchemaModel> lookupSchema = Maps.newHashMap();

            JsonNode nodeSchemas = selectPotentiallyQualifiedNode(node, "schemas");
            if (nodeSchemas instanceof ArrayNode)
            {
                for (JsonNode nodeSchema : nodeSchemas)
                {
                    BaseSchemaModel.tryParse(lookupSchema, nodeSchema);
                }
            }

            JsonNode nodeContents = selectPotentiallyQualifiedNode(node, "contents");
            if (nodeContents != null)
            {
                for (JsonNode nodeContent : nodeContents)
                {
                    PropertyModel prop = PropertyModel.tryParse(lookupSchema, nodeContent);
                    if (prop != null)
                    {
                        if (res.properties == null)
                        {
                            res.properties = Maps.newHashMap();
                        }

                        res.properties.put(prop.name, prop);
                    }

                    ComponentModel comp = ComponentModel.tryParse(lookupSchema, nodeContent);
                    if (comp != null)
                    {
                        if (res.components == null)
                        {
                            res.components = Maps.newHashMap();
                        }

                        res.components.put(comp.name, comp);
                    }

                    RelationshipModel rel = RelationshipModel.tryParse(lookupSchema, nodeContent);
                    if (rel != null)
                    {
                        if (res.relationships == null)
                        {
                            res.relationships = Maps.newHashMap();
                        }

                        res.relationships.put(rel.name, rel);
                    }
                }
            }

            JsonNode nodeExtends = selectPotentiallyQualifiedNode(node, "extends");
            if (nodeExtends != null)
            {
                for (String superClass : fromFlatOrList(nodeExtends))
                {
                    ExtendModel ext = new ExtendModel();
                    ext.id = superClass;

                    if (res.superClasses == null)
                    {
                        res.superClasses = Maps.newHashMap();
                    }

                    res.superClasses.put(ext.id, ext);
                }
            }

            return res;
        }

        return null;
    }
}

