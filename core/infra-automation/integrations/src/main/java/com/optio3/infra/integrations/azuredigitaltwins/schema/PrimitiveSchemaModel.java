/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.integrations.azuredigitaltwins.schema;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.optio3.infra.integrations.azuredigitaltwins.InterfaceModel;

public class PrimitiveSchemaModel extends BaseSchemaModel
{
    public enum Kind
    {
        BOOLEAN("boolean"),
        DATE("date"),
        DATETIME("dateTime"),
        DOUBLE("double"),
        DURATION("duration"),
        FLOAT("float"),
        INTEGER("integer"),
        LONG("long"),
        STRING("string"),
        TIME("time");

        public final String id;

        Kind(String id)
        {
            this.id = id;
        }

        public static Kind parse(String val)
        {
            for (Kind kind : values())
            {
                if (kind.id.equals(val))
                {
                    return kind;
                }
            }

            return null;
        }
    }

    public Kind kind;

    @Override
    protected void resolveImpl(Map<String, InterfaceModel> interfaces)
    {
        // Nothing to do.
    }

    public static BaseSchemaModel tryParse(Map<String, BaseSchemaModel> lookupSchema,
                                           JsonNode node)
    {
        if (node instanceof TextNode)
        {
            String val  = node.asText();
            Kind   kind = Kind.parse(val);
            if (kind != null)
            {
                PrimitiveSchemaModel m = new PrimitiveSchemaModel();
                m.kind = kind;
                return m;
            }

            BaseSchemaModel model = lookupSchema.get(val);
            if (model == null)
            {
                var m = new IndirectSchemaModel();
                m.indirectSchema = val;
                return m;
            }
        }

        return null;
    }
}
