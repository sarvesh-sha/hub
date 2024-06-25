/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu;

import java.util.List;
import java.util.Optional;

import com.optio3.logging.Logger;
import com.optio3.logging.LoggerFactory;
import com.optio3.logging.LoggerResource;
import com.optio3.protocol.bacnet.model.enums.ApplicationTag;
import com.optio3.protocol.model.bacnet.AnyValue;
import com.optio3.protocol.model.bacnet.Constructed;
import com.optio3.protocol.model.bacnet.error.BACnetDecodingException;
import com.optio3.serialization.Null;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.SerializablePiece;
import com.optio3.serialization.SerializationHelper;
import com.optio3.serialization.SerializationSlotToFields;
import com.optio3.serialization.TypeDescriptor;
import com.optio3.stream.OutputBuffer;

public final class TagContextForEncoding extends TagContextCommon
{
    public static final Logger LoggerInstance = TagContextCommon.LoggerInstance.createSubLogger(TagContextForEncoding.class);

    //--//

    private final OutputBuffer m_buffer;

    public TagContextForEncoding(OutputBuffer buffer,
                                 Object target)
    {
        this(null, buffer, target);
    }

    private TagContextForEncoding(TagContextForEncoding outer,
                                  OutputBuffer buffer,
                                  Object target)
    {
        super(outer, target);

        m_buffer = buffer;
    }

    //--//

    private enum EncodeResult
    {
        Ok,
        Missing,
    }

    public void encode()
    {
        LoggerInstance.debug("Starting encoding at %d: %s",
                             m_buffer.getPosition(),
                             m_target.getClass()
                                     .getName());

        try (LoggerResource logCfg = LoggerFactory.indent(">>"))
        {
            List<SerializationSlotToFields> slots = SerializationHelper.collectTags(m_target.getClass());
            for (SerializationSlotToFields slot : slots)
            {
                String choiceSet = selectChoiceSet(slot);

                SerializablePiece piece = slot.normalPiece;
                if (piece == null)
                {
                    throw BACnetDecodingException.newException("Encountered unexpected bitfield at tag '%s' while trying to encode instance of class '%s'",
                                                               slot,
                                                               m_target.getClass()
                                                                       .getName());
                }

                EncodeResult res = encodeSlot(piece, slot.sequence, slot.isOptional(), choiceSet != null);
                switch (res)
                {
                    case Ok:
                        registerChoiceSet(choiceSet);
                        break;

                    case Missing:
                        if (choiceSet == null)
                        {
                            throw BACnetDecodingException.newException("Field cannot be missing: %s.%s",
                                                                       m_target.getClass()
                                                                               .getName(),
                                                                       slot.normalPiece.getFieldName());
                        }

                        break;
                }
            }

            String missingChoiceSet = verifyChoiceSets();
            if (missingChoiceSet != null)
            {
                AnyValue av = Reflection.as(m_target, AnyValue.class);
                if (av != null)
                {
                    Object value = av.anyValue;

                    ApplicationTag       appTag = TagHeaderForEncoding.classifyValue(value);
                    TagHeaderForEncoding tag    = TagHeaderForEncoding.createApplicationTag(appTag);
                    tag.emitApplicationValue(m_buffer, value);
                }
                else
                {
                    throw BACnetDecodingException.newException("Missing value for choiceSet '%s'", missingChoiceSet);
                }
            }
        }

        LoggerInstance.debug("Ended encoding at %d", m_buffer.getPosition());
    }

    private EncodeResult encodeSlot(SerializablePiece piece,
                                    int sequence,
                                    boolean isOptional,
                                    boolean isChoice)
    {
        Optional<?> valueOpt = piece.getFieldRaw(m_target);
        Object      value;
        if (!valueOpt.isPresent())
        {
            if (isOptional)
            {
                return EncodeResult.Ok;
            }

            if (Reflection.isSubclassOf(AnyValue.class, piece.actualType))
            {
                value = null;
            }
            else
            {
                return EncodeResult.Missing;
            }
        }
        else
        {
            value = valueOpt.get();
            if (value == Null.instance)
            {
                value = null;
            }
        }

        LoggerInstance.debug("encodeSlot: %s => %s", piece, value != null ? value.getClass() : "<none>");
        Class<?> actualType = piece.actualType;
        boolean  isList     = piece.isList;
        boolean  isArray    = piece.isArray;

        if (actualType == Object.class && value != null && (!isList && !isArray))
        {
            Class<?> type = value.getClass();

            if (Reflection.isSubclassOf(List.class, type))
            {
                isList  = true;
                isArray = false;
            }
            else if (type == byte[].class)
            {
                // Handled as a primitive type.
            }
            else if (type.isArray())
            {
                actualType = type.getComponentType();
                isList     = false;
                isArray    = true;
            }
        }

        boolean isUntagged = isUntagged(piece);

        if (isList || isArray)
        {
            int fixedArraySize = piece.fixedArraySize;

            TagHeaderForEncoding.emitOpeningTag(m_buffer, isUntagged, sequence);

            int length = SerializationHelper.getCollectionLength(value, isList, isArray);
            if (fixedArraySize > 0)
            {
                length = fixedArraySize;
            }

            for (int i = 0; i < length; i++)
            {
                Object value2 = SerializationHelper.getCollectionItem(value, isList, isArray, fixedArraySize, i);
                encodeValue(true, -1, value2, actualType);
            }

            TagHeaderForEncoding.emitClosingTag(m_buffer, isUntagged, sequence);

            return EncodeResult.Ok;
        }

        encodeValue(isUntagged, sequence, value, actualType);
        return EncodeResult.Ok;
    }

    private void encodeValue(boolean isUntagged,
                             int sequence,
                             Object value,
                             Class<?> actualType)
    {
        boolean useApplicationTag = isUntagged;

        if (Reflection.isSubclassOf(AnyValue.class, actualType))
        {
            AnyValue av = (AnyValue) value;

            if (av.anyValue != null)
            {
                value             = av.anyValue;
                useApplicationTag = true;
            }
        }

        if (actualType.isEnum())
        {
            useApplicationTag = true;
        }

        ApplicationTag appTag = TagHeaderForEncoding.classifyValue(value);
        if (appTag == null)
        {
            TagHeaderForEncoding.emitOpeningTag(m_buffer, isUntagged, sequence);

            encodeSubValue(actualType, value);

            TagHeaderForEncoding.emitClosingTag(m_buffer, isUntagged, sequence);
            return;
        }

        TagHeaderForEncoding tag;
        TypeDescriptor       td = Reflection.getDescriptor(actualType);

        if (useApplicationTag)
        {
            tag = TagHeaderForEncoding.createApplicationTag(appTag);
            tag.emitValue(m_buffer, td, value);
            return;
        }

        if (actualType == Object.class)
        {
            TagHeaderForEncoding.emitOpeningTag(m_buffer, isUntagged, sequence);

            tag = TagHeaderForEncoding.createApplicationTag(appTag);
            tag.emitValue(m_buffer, td, value);

            TagHeaderForEncoding.emitClosingTag(m_buffer, isUntagged, sequence);
            return;
        }

        if (isUntagged)
        {
            throw BACnetDecodingException.newException("INTERNAL ERROR: unexpected value requiring a context tag", actualType.getName(), value);
        }

        tag = TagHeaderForEncoding.createContextTag(sequence);
        tag.emitValue(m_buffer, td, value);
    }

    private void encodeSubValue(Class<?> actualType,
                                Object value)
    {
        if (!Constructed.class.isInstance(value))
        {
            PrimitiveWrapper wrapper = new PrimitiveWrapper();
            wrapper.value = value;

            value = wrapper;
        }

        TagContextForEncoding subContext = new TagContextForEncoding(this, m_buffer, value);
        subContext.encode();
    }
}
