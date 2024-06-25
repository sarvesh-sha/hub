/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.normalizations;

import java.io.InputStream;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.Exceptions;
import com.optio3.util.Resources;
import org.apache.commons.lang3.StringUtils;

public class EquipmentClass
{
    public int                     id;
    public String                  equipClassName;
    public String                  description;
    public String                  azureDigitalTwin;
    public WellKnownEquipmentClass wellKnown;
    public List<String>            tags;

    private String m_idAsString;

    @JsonIgnore
    public String idAsString()
    {
        if (m_idAsString == null)
        {
            m_idAsString = Integer.toString(id);
        }

        return m_idAsString;
    }

    //--//

    public static int compare(EquipmentClass a,
                              EquipmentClass b,
                              boolean ascending)
    {
        int diff = 0;

        if (a != null && b != null)
        {
            diff = StringUtils.compareIgnoreCase(a.equipClassName, b.equipClassName);
        }
        else if (a != null)
        {
            diff = -1;
        }
        else if (b != null)
        {
            diff = 1;
        }

        return ascending ? diff : -diff;
    }

    public static List<EquipmentClass> load() throws
                                              Exception
    {
        try (InputStream stream = Resources.openResourceAsStream(EquipmentClass.class, "normalization/McKinstryEquipmentClass.json"))
        {
            return ObjectMappers.SkipNullsCaseInsensitive.readValue(stream, new TypeReference<List<EquipmentClass>>()
            {
            });
        }
    }

    public static String convertFromCsv() throws
                                          Exception
    {
        List<EquipmentClass> equipmentClasses = EquipmentClass.parse(Resources.loadResourceAsLines(EquipmentClass.class, "normalization/McKinstryEquipmentClass.csv", false));

        return ObjectMappers.prettyPrintAsJson(ObjectMappers.SkipNulls, equipmentClasses);
    }

    private static List<EquipmentClass> parse(List<String> lines)
    {
        List<EquipmentClass> equipmentClasses = Lists.newArrayList();

        String[] headers = StringUtils.split(lines.get(0), '\t');

        for (int i = 1; i < lines.size(); i++)
        {
            String[] values = StringUtils.split(lines.get(i), '\t');

            EquipmentClass ec = new EquipmentClass();
            equipmentClasses.add(ec);

            for (int headerIdx = 0; headerIdx < headers.length; headerIdx++)
            {
                final String header = headers[headerIdx];

                String valueRaw = values[headerIdx];

                if ("NULL".equals(valueRaw))
                {
                    valueRaw = null;
                }

                if (header.endsWith("ID"))
                {
                    int value = (valueRaw == null) ? -1 : Integer.parseInt(valueRaw);

                    switch (header)
                    {
                        case "ID":
                            ec.id = value;
                            break;

                        default:
                            throw Exceptions.newIllegalArgumentException("Unrecognized field '%s' at line %d", header, i);
                    }
                }
                else
                {
                    switch (header)
                    {
                        case "EquipClassName":
                            ec.equipClassName = valueRaw;
                            break;

                        case "Description":
                            ec.description = valueRaw;
                            break;

                        default:
                            throw Exceptions.newIllegalArgumentException("Unrecognized field '%s' at line %d", header, i);
                    }
                }
            }
        }

        return equipmentClasses;
    }
}
