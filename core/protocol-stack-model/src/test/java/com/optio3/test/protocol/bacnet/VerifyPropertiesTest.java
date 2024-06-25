/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.protocol.bacnet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.optio3.lang.Unsigned16;
import com.optio3.lang.Unsigned32;
import com.optio3.lang.Unsigned8;
import com.optio3.protocol.model.bacnet.AnyValue;
import com.optio3.protocol.model.bacnet.BACnetDate;
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.protocol.model.bacnet.BACnetTime;
import com.optio3.protocol.model.bacnet.PropertyType;
import com.optio3.protocol.model.bacnet.enums.BACnetBinaryPV;
import com.optio3.protocol.model.bacnet.enums.BACnetConformance;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.TypeDescriptor;
import com.optio3.test.common.Optio3Test;
import org.junit.Ignore;
import org.junit.Test;

public class VerifyPropertiesTest extends Optio3Test
{
    private static final String   c_property_definitions = "BACnet/ObjectTypesAndProperties.txt";
    static final         String[] c_packages             = { "com.optio3.protocol.model.bacnet",
                                                             "com.optio3.protocol.model.bacnet.enums",
                                                             "com.optio3.protocol.model.bacnet.enums.bitstring",
                                                             "com.optio3.protocol.model.bacnet.constructed",
                                                             "com.optio3.protocol.model.bacnet.constructed.choice", };

    static class PropertyDefinition
    {
        BACnetObjectType         objectType;
        BACnetPropertyIdentifier property;
        BACnetConformance        conformance;

        boolean  isArray;
        boolean  isList;
        boolean  isOptional;
        int      arrayLength;
        Class<?> clz;

        String definition;
    }

    @Test
    public void verifyPropertyDefinitions() throws
                                            IOException
    {
        Map<BACnetObjectType, Map<BACnetPropertyIdentifier, PropertyDefinition>> lookup = Maps.newHashMap();

        try (BufferedReader reader = openResourceAsBufferedReader(c_property_definitions))
        {
            loadPropertiesDefinition(reader, lookup);
        }

        try
        {
            validateObjectTypeAnnotations(lookup);
        }
        catch (Error e)
        {
            List<String> lines = generateObjectTypeContents(lookup);
            for (String line : lines)
                System.out.println(line);

            throw e;
        }
    }

    @Ignore("NOTE!!! This unit test updates the source code, it should be manually enabled when 'verifyPropertyDefinitions' fails.")
    @Test
    public void regeneratePropertyDefinitions() throws
                                                IOException
    {
        Map<BACnetObjectType, Map<BACnetPropertyIdentifier, PropertyDefinition>> lookup = Maps.newHashMap();

        try (BufferedReader reader = openResourceAsBufferedReader(c_property_definitions))
        {
            loadPropertiesDefinition(reader, lookup);
        }

        URL    res  = openResource(c_property_definitions);
        String path = res.getPath();
        path = path.replace("/target/test-classes/" + c_property_definitions, "/src/main/java/com/optio3/protocol/model/bacnet/enums/BACnetObjectType.java");

        List<String> newLines = generateObjectTypeContents(lookup);
        List<String> output   = Lists.newArrayList();

        try (FileReader reader2 = new FileReader(path))
        {
            BufferedReader reader3 = new BufferedReader(reader2);
            boolean        skip    = false;

            while (true)
            {
                String line = reader3.readLine();
                if (line == null)
                {
                    break;
                }

                if (skip)
                {
                    if (line.startsWith("    // @formatter:on"))
                    {
                        skip = false;
                    }
                }

                if (!skip)
                {
                    output.add(line);

                    if (line.startsWith("    // @formatter:off"))
                    {
                        skip = true;
                        output.addAll(newLines);
                    }
                }
            }
        }

        try (FileWriter writer = new FileWriter(path))
        {
            for (String line : output)
            {
                writer.write(line);
                writer.write("\n");
            }
        }
    }

    @Ignore("NOTE!!! This unit test updates the source code, it should be manually enabled when 'verifyPropertyDefinitions' fails.")
    @Test
    public void regenerateObjectModels() throws
                                         IOException
    {
        Map<BACnetObjectType, Map<BACnetPropertyIdentifier, PropertyDefinition>> lookup = Maps.newHashMap();

        try (BufferedReader reader = openResourceAsBufferedReader(c_property_definitions))
        {
            loadPropertiesDefinition(reader, lookup);
        }

        URL    res  = openResource(c_property_definitions);
        String path = res.getPath();
        path = path.replace("/target/test-classes/" + c_property_definitions, "/src/main/java/com/optio3/protocol/model/bacnet/objects/");

        generateObjectModels(path, lookup);
    }

    private void generateObjectModels(String path,
                                      Map<BACnetObjectType, Map<BACnetPropertyIdentifier, PropertyDefinition>> lookup) throws
                                                                                                                       IOException
    {
        for (BACnetObjectType objectType : BACnetObjectType.values())
        {
            Map<BACnetPropertyIdentifier, PropertyDefinition> map = lookup.get(objectType);
            if (map == null)
            {
                map = Maps.newHashMap();
            }

            List<String> list = generateObjectModel(objectType, map);

            try (FileWriter writer = new FileWriter(path + objectType.name() + ".java"))
            {
                for (String line : list)
                {
                    writer.write(line);
                    writer.write("\n");
                }
            }
        }
    }

    //--//

    private void validateObjectTypeAnnotations(Map<BACnetObjectType, Map<BACnetPropertyIdentifier, PropertyDefinition>> lookup)
    {
        for (BACnetObjectType objectType : BACnetObjectType.values())
        {
            Map<BACnetPropertyIdentifier, PropertyType> mapCurrent = objectType.propertyTypes();

            Map<BACnetPropertyIdentifier, PropertyDefinition> mapNew = lookup.get(objectType);
            if (mapNew == null)
            {
                mapNew = Maps.newHashMap();
            }

            Set<BACnetPropertyIdentifier> keyCurrent = mapCurrent.keySet();
            Set<BACnetPropertyIdentifier> keyNew     = mapNew.keySet();

            SetView<BACnetPropertyIdentifier> notInNew = Sets.difference(keyCurrent, keyNew);
            assertEquals(String.format("Extra properties present in %s, should remove: %s", objectType, notInNew), 0, notInNew.size());

            SetView<BACnetPropertyIdentifier> notInCurrent = Sets.difference(keyNew, keyCurrent);
            assertEquals(String.format("Properties not present in %s, should add: %s", objectType, notInCurrent), 0, notInCurrent.size());

            for (BACnetPropertyIdentifier prop : mapCurrent.keySet())
            {
                PropertyType       ptCurrent = mapCurrent.get(prop);
                PropertyDefinition pdNew     = mapNew.get(prop);

                assertEquals(pdNew.definition, pdNew.conformance, ptCurrent.conformance());
                assertEquals(pdNew.definition, pdNew.clz, ptCurrent.type());
                assertEquals(pdNew.definition, pdNew.isArray, ptCurrent.isArray());
                assertEquals(pdNew.definition, pdNew.isList, ptCurrent.isList());
                assertEquals(pdNew.definition, pdNew.isOptional, ptCurrent.isOptional());
                assertEquals(pdNew.definition, pdNew.arrayLength < 0 ? 0 : pdNew.arrayLength, ptCurrent.arrayLength());
            }
        }
    }

    private List<String> generateObjectTypeContents(Map<BACnetObjectType, Map<BACnetPropertyIdentifier, PropertyDefinition>> lookup)
    {
        List<String> lines = Lists.newArrayList();

        List<BACnetObjectType>         objectTypes = sortEnumValues(BACnetObjectType.values());
        List<BACnetPropertyIdentifier> propertyIds = sortEnumValues(BACnetPropertyIdentifier.values());

        int maxObject = -1;
        for (BACnetObjectType objectType : objectTypes)
            maxObject = Math.max(maxObject,
                                 objectType.name()
                                           .length());

        String fmtObj = "    %-" + maxObject + "s(%d, com.optio3.protocol.model.bacnet.objects.%s.class)";

        for (BACnetObjectType objectType : objectTypes)
        {
            Map<BACnetPropertyIdentifier, PropertyDefinition> map = lookup.get(objectType);
            if (map != null)
            {
                int                           maxProp = -1;
                int                           maxType = -1;
                Set<BACnetPropertyIdentifier> keys    = map.keySet();
                for (BACnetPropertyIdentifier key : keys)
                {
                    PropertyDefinition pd = map.get(key);
                    maxProp = Math.max(maxProp,
                                       key.name()
                                          .length());
                    maxType = Math.max(maxType, getTypeText(pd, false, false).length());
                }

                String fmtProp = "    @PropertyType(property = BACnetPropertyIdentifier.%-" + maxProp + "s, type = %-" + maxType + "s.class, conformance = BACnetConformance.%s";

                for (int pass = 0; pass < 3; pass++)
                {
                    for (BACnetPropertyIdentifier prop : propertyIds)
                    {
                        if (keys.contains(prop))
                        {
                            PropertyDefinition pd = map.get(prop);

                            switch (pd.conformance)
                            {
                                case Required:
                                    if (pass != 0)
                                    {
                                        continue;
                                    }
                                    break;

                                case RequiredAndWritable:
                                    if (pass != 1)
                                    {
                                        continue;
                                    }
                                    break;

                                case Optional:
                                    if (pass != 2)
                                    {
                                        continue;
                                    }
                                    break;
                            }

                            StringBuilder sb = new StringBuilder();

                            append(sb, fmtProp, pd.property.name(), getTypeText(pd, false, false), pd.conformance.name());

                            if (pd.isOptional)
                            {
                                append(sb, ", isOptional = true");
                            }

                            if (pd.isArray)
                            {
                                append(sb, ", isArray = true");

                                if (pd.arrayLength > 0)
                                {
                                    append(sb, ", arrayLength = %d", pd.arrayLength);
                                }
                            }
                            else if (pd.isList)
                            {
                                append(sb, ", isList = true");
                            }

                            append(sb, ")");

                            lines.add(sb.toString());
                        }
                    }
                }
            }

            {
                StringBuilder sb = new StringBuilder();
                append(sb, fmtObj, objectType.name(), objectType.getEncodingValue(), objectType.name());
                if (objectType == objectTypes.get(objectTypes.size() - 1))
                {
                    append(sb, ";");
                }
                else
                {
                    append(sb, ",");
                }

                lines.add(sb.toString());
            }
        }

        return lines;
    }

    private List<String> generateObjectModel(BACnetObjectType objectType,
                                             Map<BACnetPropertyIdentifier, PropertyDefinition> map)
    {
        List<String> lines = Lists.newArrayList();

        append(lines, "/*");
        append(lines, " * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.");
        append(lines, " *");
        append(lines, " * Proprietary & Confidential Information.");
        append(lines, " */");
        append(lines, "package com.optio3.protocol.model.bacnet.objects;");
        append(lines);

        boolean hasList     = false;
        boolean hasOptional = false;

        for (PropertyDefinition pd : map.values())
        {
            if (pd.isList)
            {
                hasList = true;
            }

            if (pd.isOptional)
            {
                hasOptional = true;
            }
        }

        if (hasList)
        {
            append(lines, "import java.util.List;");
        }

        if (hasOptional)
        {
            append(lines, "import java.util.Optional;");
        }

        if (hasList || hasOptional)
        {
            append(lines);
        }

        append(lines, "import com.fasterxml.jackson.annotation.JsonIgnore;");
        append(lines, "import com.fasterxml.jackson.annotation.JsonTypeName;");

        if (hasList)
        {
            append(lines, "import com.google.common.collect.Lists;");
        }

        Set<String> types = Sets.newHashSet();
        types.add(BACnetObjectModel.class.getName());
        types.add(BACnetObjectType.class.getName());
        for (PropertyDefinition pd : map.values())
        {
            Class<?> clz = pd.clz;

            if (clz == String.class)
            {
                continue;
            }

            if (clz.isArray())
            {
                clz = clz.getComponentType();
            }

            if (!pd.isOptional && !pd.isList)
            {
                clz = adjustTypeForUnsigned(clz);
            }

            if (clz.isPrimitive())
            {
                continue;
            }

            String typeText = clz.getName();
            types.add(typeText);
        }

        List<String> types2 = Lists.newArrayList(types);
        types2.sort((s1, s2) -> s1.compareTo(s2));

        for (String type : types2)
        {
            append(lines, "import %s;", type);
        }

        append(lines);
        append(lines, "// NOTE: Generated automatically by regenerateObjectModels unit test!!");
        append(lines, "@JsonTypeName(\"BACnet:%s\")", objectType.name());
        append(lines, "public final class %s extends BACnetObjectModel", objectType.name());
        append(lines, "{");

        Set<BACnetPropertyIdentifier> props      = map.keySet();
        BACnetPropertyIdentifier[]    propsArray = new BACnetPropertyIdentifier[props.size()];
        props.toArray(propsArray);
        List<BACnetPropertyIdentifier> propsSorted = sortEnumValues(propsArray);

        int maxField = -1;
        for (BACnetPropertyIdentifier prop : propsSorted)
        {
            PropertyDefinition pd        = map.get(prop);
            String             fieldType = getFieldType(pd);
            maxField = Math.max(maxField, fieldType.length());
        }

        String fmt = String.format("    public %%-%ds %%s;", maxField);

        append(lines, "    // @formatter:off");

        boolean gotHeader = false;

        for (int pass = -1; pass < 3; pass++)
        {
            boolean got = false;

            for (BACnetPropertyIdentifier prop : propsSorted)
            {
                PropertyDefinition pd = map.get(prop);

                switch (pd.property)
                {
                    case object_identifier:
                        if (pass != -1)
                        {
                            continue;
                        }

                        append(lines, "    @JsonIgnore // Avoid serializing the identity, it would be a duplicate in most cases.");
                        break;

                    case object_type:
                        if (pass != -1)
                        {
                            continue;
                        }

                        append(lines, "    @JsonIgnore // Avoid serializing the type, we already know it.");
                        break;

                    default:
                        if (pass == -1)
                        {
                            continue;
                        }

                        switch (pd.conformance)
                        {
                            case Required:
                                if (pass != 0)
                                {
                                    continue;
                                }

                                if (!got)
                                {
                                    got = true;

                                    if (gotHeader)
                                    {
                                        append(lines, "");
                                    }

                                    append(lines, "    ///////////////////");
                                    append(lines, "    //");
                                    append(lines, "    // Required fields:");
                                    append(lines, "    //");
                                    gotHeader = true;
                                }
                                break;

                            case RequiredAndWritable:
                                if (pass != 1)
                                {
                                    continue;
                                }

                                if (!got)
                                {
                                    got = true;

                                    if (gotHeader)
                                    {
                                        append(lines, "");
                                    }

                                    append(lines, "    //////////////////////////////");
                                    append(lines, "    //");
                                    append(lines, "    // RequiredAndWritable fields:");
                                    append(lines, "    //");
                                    gotHeader = true;
                                }
                                break;

                            case Optional:
                                if (pass != 2)
                                {
                                    continue;
                                }

                                if (!got)
                                {
                                    got = true;

                                    if (gotHeader)
                                    {
                                        append(lines, "");
                                    }

                                    append(lines, "    ///////////////////");
                                    append(lines, "    //");
                                    append(lines, "    // Optional fields:");
                                    append(lines, "    //");
                                    gotHeader = true;
                                }
                                break;
                        }
                        break;
                }

                String fieldType = getFieldType(pd);
                append(lines, fmt, fieldType, prop.name());

                if (pass == -1)
                {
                    append(lines, "");
                }
            }
        }
        append(lines, "    // @formatter:on");

        append(lines);

        append(lines, "    public %s()", objectType.name());
        append(lines, "    {");
        append(lines, "        super(BACnetObjectType.%s);", objectType.name());

        for (BACnetPropertyIdentifier prop : sortEnumValues(propsArray))
        {
            PropertyDefinition pd = map.get(prop);

            if (pd.isOptional)
            {
                continue;
            }

            if (pd.isArray && pd.arrayLength > 0)
            {
                String typeText = getTypeText(pd, false, true);
                append(lines, "        %s = new %s[%d];", prop.name(), typeText, pd.arrayLength);

                Class<?> clz = pd.clz;
                clz = adjustTypeForUnsigned(clz);

                if (clz.isPrimitive())
                {
                    continue;
                }

                if (clz == String.class)
                {
                    continue;
                }

                for (int i = 0; i < pd.arrayLength; i++)
                {
                    append(lines, "        %s[%d] = new %s();", prop.name(), i, typeText);
                }
            }
            else if (pd.isList)
            {
                append(lines, "        %s = Lists.newArrayList();", prop.name());
            }
        }
        append(lines, "    }");

        append(lines, "}");
        return lines;
    }

    private void append(List<String> lines)
    {
        lines.add("");
    }

    private void append(List<String> lines,
                        String txt)
    {
        lines.add(txt);
    }

    private void append(List<String> lines,
                        String fmt,
                        Object... args)
    {
        lines.add(String.format(fmt, args));
    }

    private void append(StringBuilder sb,
                        String fmt,
                        Object... args)
    {
        sb.append(String.format(fmt, args));
    }

    //--//

    private static <T extends Enum<T>> List<T> sortEnumValues(T[] values)
    {
        List<T> list = Lists.newArrayList();
        for (T v : values)
            list.add(v);

        list.sort((o1, o2) ->
                  {
                      return o1.name()
                               .compareTo(o2.name());
                  });

        return list;
    }

    private Class<?> adjustTypeForUnsigned(Class<?> clz)
    {
        if (clz == Unsigned8.class || clz == Unsigned16.class || clz == Unsigned32.class)
        {
            clz = long.class;
        }
        return clz;
    }

    private String getTypeText(PropertyDefinition pd,
                               boolean useBoxedVersion,
                               boolean replaceLongForUnsigned)
    {
        if (pd.clz == byte[].class)
        {
            return "byte[]";
        }

        Class<?> clz = pd.clz;

        if (useBoxedVersion)
        {
            TypeDescriptor td = Reflection.getDescriptor(pd.clz);
            if (td != null)
            {
                Class<?> clz2 = td.getBoxedType();
                if (clz2 != null)
                {
                    clz = clz2;
                }
            }
        }
        else
        {
            if (replaceLongForUnsigned)
            {
                clz = adjustTypeForUnsigned(clz);
            }
        }

        String   typeName         = clz.getName();
        String[] typeNameParts    = typeName.split("\\.");
        String   lastTypeNamePart = typeNameParts[typeNameParts.length - 1];
        return lastTypeNamePart;
    }

    private String getFieldType(PropertyDefinition pd)
    {
        boolean useBoxedVersion = pd.isOptional;
        String  prefix          = pd.isOptional ? "Optional<" : "";
        String  suffix          = pd.isOptional ? ">" : "";
        String  fmt;

        if (pd.isArray)
        {
            fmt = "%s%s[]%s";
        }
        else if (pd.isList)
        {
            fmt             = "%sList<%s>%s";
            useBoxedVersion = true;
        }
        else
        {
            fmt = "%s%s%s";
        }

        String typeText = getTypeText(pd, useBoxedVersion, true);
        return String.format(fmt, prefix, typeText, suffix);
    }

    private void loadPropertiesDefinition(BufferedReader reader,
                                          Map<BACnetObjectType, Map<BACnetPropertyIdentifier, PropertyDefinition>> lookup) throws
                                                                                                                           IOException
    {
        Map<String, BACnetObjectType>         objectTypes         = Maps.newHashMap();
        Map<String, BACnetPropertyIdentifier> propertyIdentifiers = Maps.newHashMap();

        for (BACnetObjectType e : BACnetObjectType.values())
            objectTypes.put(e.name(), e);

        for (BACnetPropertyIdentifier e : BACnetPropertyIdentifier.values())
            propertyIdentifiers.put(e.name(), e);

        Map<String, Class<?>> seen = Maps.newHashMap();

        for (String line : loadLines(reader, true))
        {
            String[] parts = line.split("\t");

            PropertyDefinition pd = new PropertyDefinition();
            pd.definition = line;

            String objectTypeTxt = safeExtract(parts, 0).toLowerCase()
                                                        .replace('-', '_')
                                                        .replace(' ', '_');
            pd.objectType = objectTypes.get(objectTypeTxt);
            assertNotNull(String.format("Can't find BACnetObjectType %s", objectTypeTxt), pd.objectType);

            String propTxt = safeExtract(parts, 1).toLowerCase();
            pd.property = propertyIdentifiers.get(propTxt);
            assertNotNull(String.format("Can't find BACnetPropertyIdentifier %s", propTxt), pd.property);

            Map<BACnetPropertyIdentifier, PropertyDefinition> o1 = lookup.computeIfAbsent(pd.objectType, k -> Maps.newHashMap());

            PropertyDefinition pd2 = o1.get(pd.property);
            if (pd2 != null)
            {
                fail(String.format("Unexpected duplicate property %s for object %s: [%s] [%s]", pd.property, pd.objectType, pd.definition, pd2.definition));
            }

            o1.put(pd.property, pd);

            String prefix          = null;
            String cardinality     = null;
            String type            = safeExtract(parts, 2);
            char   conformanceChar = safeExtract(parts, 3, 0);

            switch (conformanceChar)
            {
                case 'R':
                    pd.conformance = BACnetConformance.Required;
                    break;

                case 'W':
                    pd.conformance = BACnetConformance.RequiredAndWritable;
                    break;

                case 'O':
                    pd.conformance = BACnetConformance.Optional;
                    break;

                default:
                    fail(String.format("Unexpected conformance code %s: %s", safeExtract(parts, 3), line));
            }

            int i = type.indexOf(" (");
            if (i >= 0)
            {
                String suffix = type.substring(i + 2)
                                    .trim();
                assertTrue(suffix.endsWith(")"));
                type = type.substring(0, i)
                           .trim();
            }

            i = type.indexOf(" of ");
            if (i >= 0)
            {
                prefix = type.substring(0, i)
                             .trim();
                type   = type.substring(i + 4)
                             .trim();

                i = prefix.indexOf("[");
                if (i >= 0)
                {
                    cardinality = prefix.substring(i + 1)
                                        .trim();
                    assertTrue(cardinality.endsWith("]"));
                    cardinality = cardinality.substring(0, cardinality.length() - 1);
                    prefix      = prefix.substring(0, i)
                                        .trim();
                }
            }

            if (prefix != null)
            {

                switch (prefix)
                {
                    case "BACnetARRAY":
                        pd.isArray = true;
                        break;

                    case "BACnetLIST":
                        pd.isList = true;
                        break;

                    default:
                        fail(prefix);
                        break;
                }
            }

            if (cardinality != null)
            {

                switch (cardinality)
                {
                    case "N":
                        pd.arrayLength = -1;
                        break;

                    case "2":
                        pd.arrayLength = 2;
                        break;

                    case "3":
                        pd.arrayLength = 3;
                        break;

                    case "7":
                        pd.arrayLength = 7;
                        break;

                    case "16":
                        pd.arrayLength = 16;
                        break;

                    default:
                        fail(cardinality);
                        break;
                }
            }

            switch (type)
            {
                case "BOOLEAN":
                    pd.clz = boolean.class;
                    break;

                case "REAL":
                    pd.clz = float.class;
                    break;

                case "Double":
                    pd.clz = double.class;
                    break;

                case "CharacterString":
                    pd.clz = String.class;
                    break;

                case "OCTET STRING":
                    pd.clz = byte[].class;
                    break;

                case "BIT STRING":
                    pd.clz = BitSet.class;
                    break;

                case "INTEGER":
                    pd.clz = int.class;
                    break;

                case "Unsigned":
                case "Unsigned32":
                    pd.clz = Unsigned32.class;
                    break;

                case "Unsigned8":
                    pd.clz = Unsigned8.class;
                    break;

                case "Unsigned16":
                    pd.clz = Unsigned16.class;
                    break;

                case "Any":
                    pd.clz = AnyValue.class;
                    break;

                case "Date":
                    pd.clz = BACnetDate.class;
                    break;

                case "Time":
                    pd.clz = BACnetTime.class;
                    break;

                case "BACnetOptionalREAL":
                    pd.isOptional = true;
                    pd.clz = float.class;
                    break;

                case "BACnetOptionalUnsigned":
                    pd.isOptional = true;
                    pd.clz = Unsigned32.class;
                    break;

                case "BACnetOptionalBinaryPV":
                    pd.isOptional = true;
                    pd.clz = BACnetBinaryPV.class;
                    break;

                case "BACnetOptionalCharacterString":
                    pd.isOptional = true;
                    pd.clz = String.class;
                    break;

                case "BACnetAccessThreatLevel":
                    pd.clz = byte.class;
                    break;

                case "BACnetObjectType":
                    pd.clz = BACnetObjectTypeOrUnknown.class;
                    break;

                case "BACnetPropertyIdentifier":
                    pd.clz = BACnetPropertyIdentifierOrUnknown.class;
                    break;

                default:
                    pd.clz = seen.get(type);
                    if (pd.clz == null)
                    {
                        pd.clz = lookupType(type);
                        if (pd.clz == null)
                        {
                            String msg = String.format("Can't find type '%s' used by property '%s' in object '%s'", type, pd.property, pd.objectType);
                            System.out.println(msg);
                            fail(msg);
                        }

                        seen.put(type, pd.clz);
                    }
                    break;
            }
        }
    }

    private static char safeExtract(String[] parts,
                                    int partIndex,
                                    int charIndex)
    {
        String val = safeExtract(parts, partIndex);

        return charIndex < val.length() ? val.charAt(charIndex) : 0;
    }

    private static String safeExtract(String[] parts,
                                      int index)
    {
        return index < parts.length ? parts[index] : "";
    }

    private Class<?> lookupType(String type)
    {
        for (String pkg : c_packages)
        {
            try
            {
                return Class.forName(pkg + "." + type);
            }
            catch (ClassNotFoundException e1)
            {
            }
        }

        return null;
    }
}
