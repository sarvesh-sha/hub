/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.TypedBitSet;
import com.optio3.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public abstract class BaseObjectModel
{
    public static class ClassificationDetails
    {
        public boolean                         noEquipmentClassInDisplayName;
        public String                          equipmentName;
        public WellKnownEquipmentClassOrCustom equipmentClass;

        public WellKnownPointClassOrCustom pointClass;
        public String                      instanceSelector;
        public Map<String, Boolean>        extraTags = Collections.emptyMap(); // Set to true if the tag should be used in the physical name.

        public String getNormalizedName()
        {
            return equipmentName != null ? equipmentName : getPhysicalName();
        }

        public String getPhysicalName()
        {
            StringBuilder sb = new StringBuilder();

            if (!noEquipmentClassInDisplayName && equipmentClass != null)
            {
                append(sb, equipmentClass.getDisplayName());
            }

            append(sb, instanceSelector);

            for (String extraTag : extraTags.keySet())
            {
                if (extraTags.get(extraTag))
                {
                    append(sb, extraTag);
                }
            }

            return sb.toString();
        }

        public void addExtraTag(WellKnownTag tag,
                                boolean includeInName)
        {
            if (tag != null)
            {
                addExtraTag(tag.name(), includeInName);
            }
        }

        public void addExtraTag(String tag,
                                boolean includeInName)
        {
            if (tag != null)
            {
                if (extraTags.isEmpty())
                {
                    extraTags = Maps.newHashMap();
                }

                extraTags.put(tag, includeInName);
            }
        }

        private void append(StringBuilder sb,
                            String chunk)
        {
            if (StringUtils.isNotBlank(chunk))
            {
                if (sb.length() > 0)
                {
                    sb.append(" / ");
                }

                sb.append(chunk);
            }
        }
    }

    private static final ConcurrentMap<Class<?>, FieldModel[]> s_classToDescriptors = Maps.newConcurrentMap();

    //--//

    public abstract void fillClassificationDetails(ClassificationDetails detailsForGroup,
                                                   ClassificationDetails detailsForParent,
                                                   ClassificationDetails detailsForPoint);

    public boolean overrideDescriptorsPerObject()
    {
        return false;
    }

    public String overrideIdentifier(String identifier)
    {
        return identifier;
    }

    public String overrideDescription(FieldModel model,
                                      String description)
    {
        return description;
    }

    public EngineeringUnits overrideUnits(FieldModel model,
                                          EngineeringUnits units)
    {
        return units;
    }

    public List<String> overrideEnumeratedValues(FieldModel model,
                                                 List<String> values)
    {
        return values;
    }

    public WellKnownPointClassOrCustom overridePointClass(FieldModel model,
                                                          WellKnownPointClassOrCustom pointClass)
    {
        return pointClass;
    }

    public int overridePointClassPriority(FieldModel model,
                                          int priority)
    {
        return priority;
    }

    public List<String> overridePointTags(FieldModel model,
                                          List<String> tags)
    {
        return tags;
    }

    //--//

    public Object getField(String prop)
    {
        Field f = Reflection.findField(this.getClass(), prop);
        if (f == null)
        {
            return null;
        }

        Reflection.FieldAccessor accessor = new Reflection.FieldAccessor(f);
        return accessor.get(this);
    }

    public boolean setField(String prop,
                            Object value)
    {
        Field f = Reflection.findField(this.getClass(), prop);
        if (f == null)
        {
            return false;
        }

        Reflection.FieldAccessor accessor = new Reflection.FieldAccessor(f);
        accessor.set(this, value);

        return true;
    }

    protected static String arrayAsString(byte[] value)
    {
        return value != null ? new String(value) : null;
    }

    protected static byte[] stringAsArray(String value)
    {
        return value != null ? value.getBytes() : null;
    }

    //--//

    @SuppressWarnings("unchecked")
    public static <T extends BaseObjectModel> T copySingleProperty(T obj,
                                                                   String prop)
    {
        T objCopy = (T) obj.createEmptyCopy();

        if (prop != null)
        {
            try
            {
                Field  field = Reflection.findField(obj.getClass(), prop);
                Object val   = field.get(obj);
                field.set(objCopy, val);
            }
            catch (IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
        }

        return objCopy;
    }

    protected BaseObjectModel createEmptyCopy()
    {
        return Reflection.newInstance(this.getClass());
    }

    //--//

    private FieldModel[] m_fieldModels;

    @JsonIgnore
    public FieldModel[] getDescriptors()
    {
        if (m_fieldModels == null)
        {
            m_fieldModels = collectDescriptors(getClass());
        }

        return m_fieldModels;
    }

    public FieldModel getDescriptor(String field,
                                    boolean override)
    {
        if (override)
        {
            field = overrideIdentifier(field);
        }

        for (FieldModel fieldModel : getDescriptors())
        {
            if (fieldModel.name.equals(field))
            {
                return fieldModel;
            }
        }

        return null;
    }

    public String serializeToJson() throws
                                    JsonProcessingException
    {
        return getObjectMapperForInstance().writeValueAsString(this);
    }

    protected static <T> T deserializeInner(ObjectMapper om,
                                            Class<T> clz,
                                            String json) throws
                                                         IOException
    {
        return om.readValue(json, clz);
    }

    @JsonIgnore
    public abstract ObjectMapper getObjectMapperForInstance();

    @JsonIgnore
    public boolean isAbleToUpdateState(String field)
    {
        return false;
    }

    public boolean updateState(Map<String, JsonNode> state)
    {
        return false;
    }

    public boolean shouldIgnoreSample()
    {
        return false;
    }

    //--//

    @SuppressWarnings("unchecked")
    public static FieldModel[] collectDescriptors(Class<?> clz)
    {
        FieldModel[] res = s_classToDescriptors.get(clz);
        if (res == null)
        {
            List<FieldModel> lst = Lists.newArrayList();

            for (Field f : Reflection.collectFields(clz)
                                     .values())
            {
                FieldModelDescription t = f.getAnnotation(FieldModelDescription.class);
                if (t != null)
                {
                    Class<?>     ft = f.getType();
                    List<String> enumValues;

                    if (Reflection.canAssignTo(TypedBitSet.class, ft))
                    {
                        ft = Reflection.searchTypeArgument(TypedBitSet.class, ft, 0);
                    }

                    if (ft.isEnum())
                    {
                        Class<Enum> ft2  = (Class<Enum>) ft;
                        List<Enum>  coll = Reflection.collectEnumValues(ft2);

                        enumValues = CollectionUtils.transformToList(coll, Enum::name);
                    }
                    else
                    {
                        enumValues = Collections.emptyList();
                    }

                    lst.add(new FieldModel(f.getName(), f.getGenericType(), t, enumValues));
                }
            }

            lst.sort(Comparator.comparing((a) -> a.name));

            res = new FieldModel[lst.size()];
            lst.toArray(res);

            FieldModel[] oldRes = s_classToDescriptors.putIfAbsent(clz, res);
            if (oldRes != null)
            {
                res = oldRes;
            }
        }

        return res;
    }
}
