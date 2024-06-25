/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.lang.Unsigned32;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.bacnet.constructed.BACnetError;
import com.optio3.protocol.model.bacnet.constructed.BACnetPropertyValue;
import com.optio3.protocol.model.bacnet.enums.BACnetEngineeringUnits;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.error.BACnetFailedException;
import com.optio3.protocol.model.bacnet.error.BACnetReadFailedException;
import com.optio3.protocol.model.bacnet.objects.access_credential;
import com.optio3.protocol.model.bacnet.objects.access_door;
import com.optio3.protocol.model.bacnet.objects.access_point;
import com.optio3.protocol.model.bacnet.objects.access_rights;
import com.optio3.protocol.model.bacnet.objects.access_user;
import com.optio3.protocol.model.bacnet.objects.access_zone;
import com.optio3.protocol.model.bacnet.objects.accumulator;
import com.optio3.protocol.model.bacnet.objects.alert_enrollment;
import com.optio3.protocol.model.bacnet.objects.analog_input;
import com.optio3.protocol.model.bacnet.objects.analog_output;
import com.optio3.protocol.model.bacnet.objects.analog_value;
import com.optio3.protocol.model.bacnet.objects.averaging;
import com.optio3.protocol.model.bacnet.objects.binary_input;
import com.optio3.protocol.model.bacnet.objects.binary_lighting_output;
import com.optio3.protocol.model.bacnet.objects.binary_output;
import com.optio3.protocol.model.bacnet.objects.binary_value;
import com.optio3.protocol.model.bacnet.objects.bitstring_value;
import com.optio3.protocol.model.bacnet.objects.calendar;
import com.optio3.protocol.model.bacnet.objects.channel;
import com.optio3.protocol.model.bacnet.objects.characterstring_value;
import com.optio3.protocol.model.bacnet.objects.command;
import com.optio3.protocol.model.bacnet.objects.credential_data_input;
import com.optio3.protocol.model.bacnet.objects.date_value;
import com.optio3.protocol.model.bacnet.objects.datepattern_value;
import com.optio3.protocol.model.bacnet.objects.datetime_value;
import com.optio3.protocol.model.bacnet.objects.datetimepattern_value;
import com.optio3.protocol.model.bacnet.objects.device;
import com.optio3.protocol.model.bacnet.objects.elevator_group;
import com.optio3.protocol.model.bacnet.objects.escalator;
import com.optio3.protocol.model.bacnet.objects.event_enrollment;
import com.optio3.protocol.model.bacnet.objects.event_log;
import com.optio3.protocol.model.bacnet.objects.file;
import com.optio3.protocol.model.bacnet.objects.global_group;
import com.optio3.protocol.model.bacnet.objects.group;
import com.optio3.protocol.model.bacnet.objects.integer_value;
import com.optio3.protocol.model.bacnet.objects.large_analog_value;
import com.optio3.protocol.model.bacnet.objects.life_safety_point;
import com.optio3.protocol.model.bacnet.objects.life_safety_zone;
import com.optio3.protocol.model.bacnet.objects.lift;
import com.optio3.protocol.model.bacnet.objects.lighting_output;
import com.optio3.protocol.model.bacnet.objects.load_control;
import com.optio3.protocol.model.bacnet.objects.loop;
import com.optio3.protocol.model.bacnet.objects.multi_state_input;
import com.optio3.protocol.model.bacnet.objects.multi_state_output;
import com.optio3.protocol.model.bacnet.objects.multi_state_value;
import com.optio3.protocol.model.bacnet.objects.network_port;
import com.optio3.protocol.model.bacnet.objects.network_security;
import com.optio3.protocol.model.bacnet.objects.notification_class;
import com.optio3.protocol.model.bacnet.objects.notification_forwarder;
import com.optio3.protocol.model.bacnet.objects.octetstring_value;
import com.optio3.protocol.model.bacnet.objects.positive_integer_value;
import com.optio3.protocol.model.bacnet.objects.program;
import com.optio3.protocol.model.bacnet.objects.pulse_converter;
import com.optio3.protocol.model.bacnet.objects.schedule;
import com.optio3.protocol.model.bacnet.objects.structured_view;
import com.optio3.protocol.model.bacnet.objects.time_value;
import com.optio3.protocol.model.bacnet.objects.timepattern_value;
import com.optio3.protocol.model.bacnet.objects.timer;
import com.optio3.protocol.model.bacnet.objects.trend_log;
import com.optio3.protocol.model.bacnet.objects.trend_log_multiple;
import com.optio3.serialization.Null;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import com.optio3.util.concurrency.DeadlineTimeoutException;
import org.apache.commons.lang3.StringUtils;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type_bacnet")
@JsonSubTypes({ @JsonSubTypes.Type(value = access_credential.class),
                @JsonSubTypes.Type(value = access_door.class),
                @JsonSubTypes.Type(value = access_point.class),
                @JsonSubTypes.Type(value = access_rights.class),
                @JsonSubTypes.Type(value = access_user.class),
                @JsonSubTypes.Type(value = access_zone.class),
                @JsonSubTypes.Type(value = accumulator.class),
                @JsonSubTypes.Type(value = alert_enrollment.class),
                @JsonSubTypes.Type(value = analog_input.class),
                @JsonSubTypes.Type(value = analog_output.class),
                @JsonSubTypes.Type(value = analog_value.class),
                @JsonSubTypes.Type(value = averaging.class),
                @JsonSubTypes.Type(value = binary_input.class),
                @JsonSubTypes.Type(value = binary_lighting_output.class),
                @JsonSubTypes.Type(value = binary_output.class),
                @JsonSubTypes.Type(value = binary_value.class),
                @JsonSubTypes.Type(value = bitstring_value.class),
                @JsonSubTypes.Type(value = calendar.class),
                @JsonSubTypes.Type(value = channel.class),
                @JsonSubTypes.Type(value = characterstring_value.class),
                @JsonSubTypes.Type(value = command.class),
                @JsonSubTypes.Type(value = credential_data_input.class),
                @JsonSubTypes.Type(value = date_value.class),
                @JsonSubTypes.Type(value = datepattern_value.class),
                @JsonSubTypes.Type(value = datetime_value.class),
                @JsonSubTypes.Type(value = datetimepattern_value.class),
                @JsonSubTypes.Type(value = device.class),
                @JsonSubTypes.Type(value = elevator_group.class),
                @JsonSubTypes.Type(value = escalator.class),
                @JsonSubTypes.Type(value = event_enrollment.class),
                @JsonSubTypes.Type(value = event_log.class),
                @JsonSubTypes.Type(value = file.class),
                @JsonSubTypes.Type(value = global_group.class),
                @JsonSubTypes.Type(value = group.class),
                @JsonSubTypes.Type(value = integer_value.class),
                @JsonSubTypes.Type(value = large_analog_value.class),
                @JsonSubTypes.Type(value = life_safety_point.class),
                @JsonSubTypes.Type(value = life_safety_zone.class),
                @JsonSubTypes.Type(value = lift.class),
                @JsonSubTypes.Type(value = lighting_output.class),
                @JsonSubTypes.Type(value = load_control.class),
                @JsonSubTypes.Type(value = loop.class),
                @JsonSubTypes.Type(value = multi_state_input.class),
                @JsonSubTypes.Type(value = multi_state_output.class),
                @JsonSubTypes.Type(value = multi_state_value.class),
                @JsonSubTypes.Type(value = network_port.class),
                @JsonSubTypes.Type(value = network_security.class),
                @JsonSubTypes.Type(value = notification_class.class),
                @JsonSubTypes.Type(value = notification_forwarder.class),
                @JsonSubTypes.Type(value = octetstring_value.class),
                @JsonSubTypes.Type(value = positive_integer_value.class),
                @JsonSubTypes.Type(value = program.class),
                @JsonSubTypes.Type(value = pulse_converter.class),
                @JsonSubTypes.Type(value = schedule.class),
                @JsonSubTypes.Type(value = structured_view.class),
                @JsonSubTypes.Type(value = time_value.class),
                @JsonSubTypes.Type(value = timepattern_value.class),
                @JsonSubTypes.Type(value = timer.class),
                @JsonSubTypes.Type(value = trend_log.class),
                @JsonSubTypes.Type(value = trend_log_multiple.class) })
public abstract class BACnetObjectModel extends BaseObjectModel
{
    public static class FailureDetails
    {
        public BACnetError              error;
        public boolean                  timeout;
        public DeadlineTimeoutException deadline;
        public String                   other;

        public Exception asException(BACnetObjectIdentifier objId,
                                     BACnetPropertyIdentifierOrUnknown propId)
        {
            if (error != null)
            {
                return new BACnetReadFailedException(objId, propId, error.error_class, error.error_code);
            }

            if (timeout)
            {
                return Exceptions.newGenericException(TimeoutException.class, "Timeout while reading property '%s' on object '%s'", propId, objId);
            }

            if (deadline != null)
            {
                return deadline;
            }

            if (other != null)
            {
                return Exceptions.newGenericException(BACnetFailedException.class, "Reading property '%s' on object '%s' failed due to %s", propId, objId, other);
            }

            return Exceptions.newGenericException(BACnetFailedException.class, "Reading property '%s' on object '%s' failed", propId, objId);
        }
    }

    private static final FailureDetails                                              s_failureDueToTimeout;
    private static final ConcurrentMap<Class<? extends BACnetObjectModel>, TypeInfo> s_lookup = Maps.newConcurrentMap();
    private static final Map<Class<? extends BACnetObjectModel>, BACnetObjectType>   s_subTypes;

    static
    {
        s_failureDueToTimeout         = new FailureDetails();
        s_failureDueToTimeout.timeout = true;

        Map<Class<? extends BACnetObjectModel>, BACnetObjectType> subTypes = Maps.newHashMap();
        for (BACnetObjectType value : BACnetObjectType.values())
        {
            subTypes.put(value.getModel(), value);
        }

        s_subTypes = Collections.unmodifiableMap(subTypes);
    }

    public static Set<Class<? extends BACnetObjectModel>> getSubTypes()
    {
        return s_subTypes.keySet();
    }

    //--//

    static TypeInfo lookup(Class<? extends BACnetObjectModel> clz,
                           TypeFactory typeFactory)
    {
        TypeInfo res = s_lookup.get(clz);
        if (res == null)
        {
            res = new TypeInfo(clz, typeFactory);
            s_lookup.putIfAbsent(clz, res);
        }

        return res;
    }

    //--//

    static class PropertyInfo
    {
        final JavaType jsonType;
        JsonDeserializer<Object> deser;
        JsonSerializer<Object>   ser;

        PropertyInfo(JavaType jsonType)
        {
            this.jsonType = jsonType;
        }

        JsonDeserializer<Object> getDeserializer(DeserializationContext ctxt) throws
                                                                              JsonMappingException
        {
            if (deser == null)
            {
                deser = ctxt.findRootValueDeserializer(jsonType);
            }

            return deser;
        }

        JsonSerializer<Object> getSerializer(SerializerProvider provider) throws
                                                                          JsonMappingException
        {
            if (ser == null)
            {
                ser = provider.findValueSerializer(jsonType);
            }

            return ser;
        }
    }

    static class TypeInfo
    {
        private final Class<? extends BACnetObjectModel>                   modelClass;
        private final String                                               typeKey;
        private final String                                               typeValue;
        private final Map<BACnetPropertyIdentifierOrUnknown, PropertyInfo> properties;

        private TypeInfo(Class<? extends BACnetObjectModel> clz,
                         TypeFactory typeFactory)
        {
            modelClass = clz;

            final JsonTypeInfo anno1 = getAnnotation(clz, JsonTypeInfo.class);
            final JsonTypeName anno2 = getAnnotation(clz, JsonTypeName.class);

            typeKey   = anno1.property();
            typeValue = anno2.value();

            final BACnetObjectModel                           instance   = Reflection.newInstance(clz);
            final BACnetObjectType                            objectType = instance.getObjectType();
            final Map<BACnetPropertyIdentifier, PropertyType> ptMap      = objectType.propertyTypes();

            properties = Maps.newHashMap();

            for (BACnetPropertyIdentifier propId : ptMap.keySet())
            {
                PropertyType pt = ptMap.get(propId);

                JavaType jsonType;

                if (pt.isArray())
                {
                    jsonType = typeFactory.constructArrayType(pt.type());
                }
                else if (pt.isList())
                {
                    jsonType = typeFactory.constructCollectionType(List.class, pt.type());
                }
                else
                {
                    jsonType = typeFactory.constructType(pt.type());
                }

                properties.put(propId.forRequest(), new PropertyInfo(jsonType));
            }
        }

        private <T extends Annotation> T getAnnotation(Class<?> clz,
                                                       Class<T> clzAnno)
        {
            while (clz != null)
            {
                T anno = clz.getAnnotation(clzAnno);
                if (anno != null)
                {
                    return anno;
                }

                clz = clz.getSuperclass();
            }

            return null;
        }
    }

    static class Serializer extends StdSerializer<BACnetObjectModel>
    {
        private static final long serialVersionUID = 1L;

        static final Serializer INSTANCE = new Serializer();

        private Serializer()
        {
            super(BACnetObjectModel.class);
        }

        @Override
        public void serializeWithType(BACnetObjectModel value,
                                      JsonGenerator gen,
                                      SerializerProvider serializers,
                                      TypeSerializer typeSer) throws
                                                              IOException
        {
            serialize(value, gen, serializers);
        }

        @Override
        public boolean isEmpty(SerializerProvider provider,
                               BACnetObjectModel value)
        {
            return value == null;
        }

        @Override
        public void serialize(BACnetObjectModel value,
                              JsonGenerator jgen,
                              SerializerProvider provider) throws
                                                           IOException
        {
            TypeInfo ti = lookup(value.getClass(), provider.getTypeFactory());

            jgen.writeStartObject();
            jgen.writeStringField(ti.typeKey, ti.typeValue);

            if (value.m_targetedProperties != null)
            {
                final AnnotationIntrospector inspector = provider.getAnnotationIntrospector();
                final SerializationConfig    config    = provider.getConfig();

                for (BACnetPropertyIdentifierOrUnknown propId : value.m_targetedProperties)
                {
                    PropertyInfo pi = ti.properties.get(propId);
                    if (pi != null) // Only serialize known properties.
                    {
                        if (!value.hasFailure(propId))
                        {
                            final Object propValue = value.getValue(propId, null);
                            if (propValue == null)
                            {
                                // Skip null values, since some serializers can't handle it.
                                continue;
                            }

                            jgen.writeFieldName(propId.value.name());
                            JsonSerializer<Object> ser = pi.getSerializer(provider);
                            ser.serialize(propValue, jgen, provider);
                        }
                    }
                }
            }

            jgen.writeEndObject();
        }
    }

    static class Deserializer<T extends BACnetObjectModel> extends StdDeserializer<T>
    {
        private static final long serialVersionUID = 1L;

        private final Class<T> m_targetClz;

        private Deserializer(Class<T> clz)
        {
            super(clz);

            m_targetClz = clz;
        }

        public static <T extends BACnetObjectModel> StdDeserializer<? extends T> getInstance(Class<T> clz)
        {
            return new Deserializer<>(clz);
        }

        @Override
        public T getNullValue(DeserializationContext ctxt)
        {
            return null;
        }

        @Override
        public T deserialize(JsonParser jp,
                             DeserializationContext ctxt) throws
                                                          IOException
        {
            T res = Reflection.newInstance(m_targetClz);

            TypeInfo ti = lookup(m_targetClz, ctxt.getTypeFactory());

            jp.setCurrentValue(res);
            if (jp.hasTokenId(JsonTokenId.ID_FIELD_NAME))
            {
                String propName = jp.getCurrentName();
                do
                {
                    jp.nextToken();

                    BACnetPropertyIdentifier propId = BACnetPropertyIdentifier.parse(propName);
                    PropertyInfo             pi     = propId != null ? ti.properties.get(propId.forRequest()) : null;
                    if (pi == null)
                    {
                        throw Exceptions.newRuntimeException("Unknown property %s while deserializing %s", propName, m_targetClz);
                    }

                    JsonDeserializer<Object> deser = pi.getDeserializer(ctxt);
                    Object                   value = deser.deserialize(jp, ctxt);
                    res.setValue(propId, value);
                } while ((propName = jp.nextFieldName()) != null);
            }

            return res;
        }
    }

    //--//

    @JsonIgnore
    private final BACnetObjectType m_objectType;

    @JsonIgnore
    private BACnetObjectIdentifier m_objectIdentity;

    @JsonIgnore
    private Set<BACnetPropertyIdentifierOrUnknown> m_targetedProperties;

    @JsonIgnore
    private Map<BACnetPropertyIdentifierOrUnknown, FailureDetails> m_failures = Collections.emptyMap();

    @JsonIgnore
    private Map<BACnetPropertyIdentifierOrUnknown, Object> m_unknowns;

    //--//

    protected BACnetObjectModel(BACnetObjectType objectType)
    {
        m_objectType = objectType;
    }

    @Override
    protected BaseObjectModel createEmptyCopy()
    {
        BACnetObjectModel copy = (BACnetObjectModel) super.createEmptyCopy();
        copy.m_objectIdentity = m_objectIdentity;
        return copy;
    }

    //--//

    public PropertyType getPropertyType(BACnetPropertyIdentifierOrUnknown propIdOrUnknown)
    {
        return getPropertyType(propIdOrUnknown.value);
    }

    public PropertyType getPropertyType(BACnetPropertyIdentifier propId)
    {
        if (propId == null)
        {
            return null;
        }

        return m_objectType.propertyTypes()
                           .get(propId);
    }

    //--//

    private static final Pattern s_standardName = Pattern.compile("^(AI|AO|AV|BI|BO|BV) +([0-9]+)$");

    public String extractName(boolean allowStandardName)
    {
        String objectName  = (String) getValue(BACnetPropertyIdentifier.object_name, null);
        String description = (String) getValue(BACnetPropertyIdentifier.description, null);

        return extractName(objectName, description, allowStandardName);
    }

    public static String extractName(String objectName,
                                     String description,
                                     boolean allowStandardName)
    {
        String result = null;

        if (StringUtils.isNotBlank(objectName))
        {
            String trimmed = objectName.trim();

            Matcher matcher = s_standardName.matcher(trimmed);
            if (!matcher.matches())
            {
                // Not a standard name, use it.
                return trimmed;
            }

            // A standard name, fall through, in case a description is present.
            if (allowStandardName)
            {
                result = trimmed;
            }
        }

        if (StringUtils.isNotBlank(description))
        {
            String trimmed = description.trim();

            Matcher matcher = s_standardName.matcher(trimmed);
            if (!matcher.matches())
            {
                // Not a standard name, use it.
                return trimmed;
            }

            // A standard name, fall through, in case a description is present.
            if (allowStandardName)
            {
                result = trimmed;
            }
        }

        return result;
    }

    //--//

    @JsonIgnore
    public BACnetObjectType getObjectType()
    {
        return m_objectType;
    }

    @JsonIgnore
    public BACnetObjectIdentifier getObjectIdentity()
    {
        return m_objectIdentity;
    }

    public void setObjectIdentity(BACnetObjectIdentifier objectIdentity)
    {
        if (!objectIdentity.object_type.equals(m_objectType))
        {
            throw Exceptions.newIllegalArgumentException("Expecting object of type '%s', got '%s'", m_objectType, objectIdentity);
        }

        m_objectIdentity = objectIdentity;
    }

    //--//

    @JsonIgnore
    public BACnetEngineeringUnits getUnits()
    {
        BACnetEngineeringUnits units = null;

        if (hasProperty(BACnetPropertyIdentifier.units))
        {
            units = (BACnetEngineeringUnits) getValue(BACnetPropertyIdentifier.units, null);
        }

        return units != null ? units : BACnetEngineeringUnits.no_units;
    }

    public boolean hasProperty(BACnetPropertyIdentifierOrUnknown propId)
    {
        return getPropertyType(propId) != null;
    }

    public boolean hasProperty(BACnetPropertyIdentifier propId)
    {
        return getPropertyType(propId) != null;
    }

    public Type getType(BACnetPropertyIdentifier propId)
    {
        return getType(propId.forRequest());
    }

    public Type getType(BACnetPropertyIdentifierOrUnknown propId)
    {
        PropertyType pd = getPropertyType(propId);
        if (pd == null)
        {
            throw Exceptions.newIllegalArgumentException("Property '%s' does not exist in object '%s'", propId, m_objectIdentity);
        }

        try
        {
            Field field = Reflection.findField(this.getClass(), propId.value.name());
            return field.getGenericType();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @JsonIgnore
    public Set<BACnetPropertyIdentifierOrUnknown> getAccessedProperties()
    {
        return CollectionUtils.asEmptyCollectionIfNull(m_targetedProperties);
    }

    public boolean hasFailure(BACnetPropertyIdentifierOrUnknown propId)
    {
        return m_failures.containsKey(propId);
    }

    public FailureDetails getFailureDetails(BACnetPropertyIdentifierOrUnknown propId)
    {
        return m_failures.get(propId);
    }

    public <T> T getNumber(BACnetPropertyIdentifier propId,
                           Unsigned32 index,
                           Class<T> clz)
    {
        return getNumber(propId.forRequest(), index, clz);
    }

    public <T> T getNumber(BACnetPropertyIdentifierOrUnknown propId,
                           Unsigned32 index,
                           Class<T> clz)
    {
        Object value = getValue(propId, index);

        return Reflection.coerceNumber(value, clz);
    }

    public Object getValue(BACnetPropertyIdentifier propId,
                           Unsigned32 index)
    {
        return getValue(propId.forRequest(), index);
    }

    public Object getValue(BACnetPropertyIdentifierOrUnknown propId,
                           Unsigned32 index)
    {
        PropertyType pd = getPropertyType(propId);
        if (pd == null)
        {
            return m_unknowns != null ? m_unknowns.get(propId) : null;
        }

        try
        {
            Field field = Reflection.findField(this.getClass(), propId.value.name());

            Object value = field.get(this);

            if (value != null)
            {
                final Class<?> clzSource = field.getType();
                final Class<?> clzTarget = pd.type();

                if (pd.isArray())
                {
                    final Class<?> componentType = clzSource.getComponentType();
                    final int      length        = Array.getLength(value);

                    if (index != null)
                    {
                        int indexValPlusOne = index.unbox();

                        if (indexValPlusOne == 0)
                        {
                            value = length;
                        }
                        else
                        {
                            value = Array.get(value, indexValPlusOne - 1);

                            value = Reflection.coerceNumber(value, clzTarget);
                        }
                    }
                    else if (!clzTarget.isAssignableFrom(componentType))
                    {
                        Object value2 = Array.newInstance(clzTarget, length);

                        for (int i = 0; i < length; i++)
                        {
                            Object subValue = Array.get(value, i);
                            subValue = Reflection.coerceNumber(subValue, clzTarget);

                            Array.set(value2, i, subValue);
                        }

                        value = value2;
                    }
                }
                else if (pd.isList())
                {
                    // Nothing to do here.
                }
                else
                {
                    if (pd.isOptional())
                    {
                        if (value instanceof Optional)
                        {
                            Optional<?> opt = (Optional<?>) value;

                            value = opt.isPresent() ? opt.get() : null;
                        }
                    }

                    value = Reflection.coerceNumber(value, clzTarget);
                }
            }

            return value;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public int getArrayLength(BACnetPropertyIdentifier propId)
    {
        return getArrayLength(propId.forRequest());
    }

    public int getArrayLength(BACnetPropertyIdentifierOrUnknown propId)
    {
        PropertyType pd = getPropertyType(propId);
        if (pd == null)
        {
            throw Exceptions.newIllegalArgumentException("Property '%s' does not exist in object '%s'", propId, m_objectIdentity);
        }

        if (!pd.isArray())
        {
            throw Exceptions.newIllegalArgumentException("Property '%s' in object '%s' is not an array", propId, m_objectIdentity);
        }

        try
        {
            Object value = getField(propId.value.name());

            if (value == null)
            {
                return -1;
            }

            return Array.getLength(value);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    //--//

    public void validate(BACnetObjectTypeOrUnknown objectType)
    {
        if (objectType == null || !objectType.equals(m_objectType))
        {
            throw Exceptions.newIllegalArgumentException("Expecting object of type '%s', got '%s'", m_objectType, objectType);
        }
    }

    public void setValue(BACnetPropertyValue prop)
    {
        setValueWithOptionalIndex(prop.property_identifier, prop.property_array_index, prop.property_value);
    }

    public void setValue(BACnetPropertyIdentifier propId,
                         Object value)
    {
        setValue(propId.forRequest(), null, value);
    }

    public void setValue(BACnetPropertyIdentifier propId,
                         Unsigned32 index,
                         Object value)
    {
        if (propId != null)
        {
            setValue(propId.forRequest(), index, value);
        }
    }

    public void setValueWithOptionalIndex(BACnetPropertyIdentifierOrUnknown propIdOrUnknown,
                                          Optional<Unsigned32> optIndex,
                                          Object value)
    {
        setValue(propIdOrUnknown, optIndex != null && optIndex.isPresent() ? optIndex.get() : null, value);
    }

    public void setValue(BACnetPropertyIdentifierOrUnknown propIdOrUnknown,
                         Unsigned32 index,
                         Object value)
    {
        if (propIdOrUnknown == null)
        {
            return;
        }

        registerPropertyAccess(propIdOrUnknown);

        try
        {
            PropertyType pd = getPropertyType(propIdOrUnknown);
            if (pd == null)
            {
                if (m_unknowns == null)
                {
                    m_unknowns = Maps.newHashMap();
                }

                m_unknowns.put(propIdOrUnknown, value);
            }
            else
            {
                final BACnetPropertyIdentifier propId = propIdOrUnknown.value;

                Field field = Reflection.findField(this.getClass(), propId.name());

                if (pd.isOptional())
                {
                    if (value == Null.instance)
                    {
                        value = Optional.empty();
                    }
                    else
                    {
                        value = Optional.of(value);
                    }
                }
                else if (value == Null.instance)
                {
                    value = null;
                }
                else if (value != null)
                {
                    Class<?> clzTarget = field.getType();

                    if (value instanceof JsonNode)
                    {
                        value = getObjectMapper().convertValue(value, clzTarget);
                    }

                    Class<?> clzSource = value.getClass();

                    if (clzTarget.isArray())
                    {
                        clzTarget = clzTarget.getComponentType();

                        if (index != null)
                        {
                            int indexValPlusOne = Unsigned32.unboxOrDefault(index, -1);

                            if (indexValPlusOne < 0)
                            {
                                throw Exceptions.newIllegalArgumentException("Unexpected index for property '%s': %d", propId, index);
                            }

                            // Index == 0 means 'array length'
                            if (indexValPlusOne == 0)
                            {
                                int len = Reflection.coerceNumber(value, int.class);

                                value = Array.newInstance(clzTarget, len);
                            }
                            else
                            {
                                int    indexVal   = indexValPlusOne - 1;
                                Object valueField = field.get(this);
                                if (valueField != null)
                                {
                                    int lenTarget = Array.getLength(valueField);

                                    if (lenTarget <= indexVal)
                                    {
                                        //
                                        // Resize array.
                                        //
                                        Object valueFieldNew = Array.newInstance(clzTarget, indexVal + 1);

                                        for (int i = 0; i < lenTarget; i++)
                                        {
                                            Object item = Array.get(valueField, i);
                                            Array.set(valueFieldNew, i, item);
                                        }

                                        valueField = valueFieldNew;
                                    }
                                }
                                else
                                {
                                    valueField = Array.newInstance(clzTarget, indexVal + 1);
                                }

                                value = Reflection.coerceNumber(value, clzTarget);
                                Array.set(valueField, indexVal, value);

                                value = valueField;
                            }
                        }
                        else if (clzSource.isArray())
                        {
                            clzSource = clzSource.getComponentType();

                            int lenSource = Array.getLength(value);

                            Object valueField = field.get(this);
                            if (valueField != null)
                            {
                                //
                                // If the array is already allocated, limit the copy to the size of the target array.
                                //
                                lenSource = Math.min(lenSource, Array.getLength(valueField));
                            }
                            else
                            {
                                valueField = Array.newInstance(clzTarget, lenSource);
                            }

                            for (int i = 0; i < lenSource; i++)
                            {
                                Object item = Array.get(value, i);
                                item = Reflection.coerceNumber(item, clzTarget);
                                Array.set(valueField, i, item);
                            }

                            value = valueField;
                        }
                    }
                    else
                    {
                        value = Reflection.coerceNumber(value, clzTarget);
                    }
                }

                field.set(this, value);
            }
        }
        catch (Exception e)
        {
            setFailureInner(propIdOrUnknown, e);
        }
    }

    public void setFailure(BACnetPropertyIdentifierOrUnknown propId,
                           Object failure)
    {
        registerPropertyAccess(propId);

        setFailureInner(propId, failure);
    }

    private void setFailureInner(BACnetPropertyIdentifierOrUnknown propId,
                                 Object failure)
    {
        if (m_failures.isEmpty())
        {
            m_failures = Maps.newHashMap();
        }

        FailureDetails fd;

        if (failure instanceof BACnetError)
        {
            fd       = new FailureDetails();
            fd.error = (BACnetError) failure;
        }
        else if (failure instanceof DeadlineTimeoutException)
        {
            fd          = new FailureDetails();
            fd.deadline = (DeadlineTimeoutException) failure;
        }
        else if (failure instanceof TimeoutException)
        {
            fd = s_failureDueToTimeout; // Use a singleton object, since timeouts don't have per-instance state.
        }
        else
        {
            fd       = new FailureDetails();
            fd.other = failure.toString();
        }

        m_failures.put(propId, fd);
    }

    private void registerPropertyAccess(BACnetPropertyIdentifierOrUnknown propId)
    {
        if (m_targetedProperties == null)
        {
            m_targetedProperties = Sets.newHashSet();
        }

        m_targetedProperties.add(propId);
    }

    //--//

    public static <T extends BACnetObjectModel> T deserializeFromJson(Class<T> clz,
                                                                      String json) throws
                                                                                   IOException
    {
        return deserializeInner(getObjectMapper(), clz, json);
    }

    @Override
    public ObjectMapper getObjectMapperForInstance()
    {
        return getObjectMapper();
    }

    public static ObjectMapper getObjectMapper()
    {
        return ObjectMappers.SkipNulls;
    }

    @Override
    public void fillClassificationDetails(ClassificationDetails detailsForGroup,
                                          ClassificationDetails detailsForParent,
                                          ClassificationDetails detailsForPoint)
    {
        // Classification is done through a different process on BACnet.
    }

    @Override
    public boolean isAbleToUpdateState(String identifier)
    {

        switch (m_objectType)
        {
            case analog_output:
            case analog_value:
            case large_analog_value:
            case binary_output:
            case binary_lighting_output:
            case binary_value:
            case multi_state_output:
            case multi_state_value:
            case command:
                return true;

            default:
                return false;
        }
    }

    @Override
    public boolean updateState(Map<String, JsonNode> state)
    {
        boolean modified = super.updateState(state);

        for (String field : state.keySet())
        {
            BACnetPropertyIdentifier prop = BACnetPropertyIdentifier.parse(field);
            if (hasProperty(prop))
            {
                setValue(prop, state.get(field));
                modified = true;
            }
        }

        return modified;
    }
}
