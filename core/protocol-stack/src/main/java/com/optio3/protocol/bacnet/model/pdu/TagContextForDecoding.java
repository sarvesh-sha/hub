/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu;

import java.lang.reflect.Array;
import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.lang.Unsigned32;
import com.optio3.logging.Logger;
import com.optio3.logging.LoggerFactory;
import com.optio3.logging.LoggerResource;
import com.optio3.protocol.bacnet.model.enums.ApplicationTag;
import com.optio3.protocol.model.bacnet.AnyValue;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.Choice;
import com.optio3.protocol.model.bacnet.Constructed;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.error.BACnetDecodingException;
import com.optio3.serialization.Null;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.SerializablePiece;
import com.optio3.serialization.SerializationHelper;
import com.optio3.serialization.SerializationSlotToFields;
import com.optio3.stream.InputBuffer;

public class TagContextForDecoding extends TagContextCommon
{
    public static final Logger LoggerInstance = TagContextCommon.LoggerInstance.createSubLogger(TagContextForDecoding.class);

    //--//

    public TagContextForDecoding(Object target)
    {
        this(null, target);
    }

    private TagContextForDecoding(TagContextForDecoding outer,
                                  Object target)
    {
        super(outer, target);
    }

    //--//

    private enum DecodeResult
    {
        Ok,
        CheckForOptional,
        NoMatch,
        UnexpectedNullValue,
    }

    public void decode(InputBuffer buffer)
    {
        List<TagHeaderForDecoding> tags = Lists.newArrayList();

        if (buffer != null)
        {
            while (!buffer.isEOF())
            {
                int startPos = buffer.getPosition();
                LoggerInstance.debug("##Starting decoding at %d", startPos);

                TagHeaderForDecoding tag = new TagHeaderForDecoding(buffer);
                if (tag.isClosingTag)
                {
                    throw BACnetDecodingException.newException("Closing tag without a corresponding opening tag at offset %d", startPos);
                }

                tags.add(tag);

                LoggerInstance.debug("##Ended decoding at %d", buffer.getPosition());
                LoggerInstance.debug("");
            }
        }

        decode(new TagCursor(tags));
    }

    private void decode(TagCursor cursor)
    {
        try (LoggerResource logCfg = LoggerFactory.indent(">>"))
        {
            CustomWrapper target = Reflection.as(m_target, CustomWrapper.class);
            if (target != null)
            {
                while (!cursor.isEOF())
                {
                    TagHeaderForDecoding tag = cursor.nextTag();

                    CustomField field = new CustomField();
                    target.fields.add(field);

                    field.contextTag     = tag.contextTag;
                    field.applicationTag = tag.applicationTag;

                    if (tag.isOpeningTag)
                    {
                        CustomWrapper         subTarget  = new CustomWrapper();
                        TagCursor             subCursor  = new TagCursor(tag.nestedTags);
                        TagContextForDecoding subContext = new TagContextForDecoding(this, subTarget);
                        subContext.decode(subCursor);

                        field.value = subTarget;
                    }
                    else
                    {
                        List<SerializationSlotToFields> slots = SerializationHelper.collectTags(CustomField.class);
                        field.value = tag.readValue(this, slots.get(0).normalPiece);
                    }
                }
            }
            else
            {
                List<SerializationSlotToFields> slots = SerializationHelper.collectTags(m_target.getClass());
                for (SerializationSlotToFields slot : slots)
                {
                    String choiceSet = selectChoiceSet(slot);

                    int startTagPos = cursor.getCursor();

                    SerializablePiece piece = slot.normalPiece;
                    if (piece == null)
                    {
                        throw BACnetDecodingException.newException("Encountered unexpected bitfield at tag number %d", startTagPos);
                    }

                    LoggerInstance.debug("#######");
                    LoggerInstance.debug("Trying to decode slot %s", slot);

                    DecodeResult res = decodeSlot(cursor, piece, slot.sequence, slot.isOptional(), choiceSet != null);
                    LoggerInstance.debug("Processed tag as %s", res);

                    switch (res)
                    {
                        case Ok:
                            registerChoiceSet(choiceSet);
                            break;

                        case UnexpectedNullValue:
                            throw BACnetDecodingException.newException("Encountered unexpected null at offset %d", startTagPos);

                        case CheckForOptional:
                        case NoMatch:
                            while (true)
                            {
                                if (slot.isOptional())
                                {
                                    break;
                                }

                                if (choiceSet != null)
                                {
                                    break;
                                }

                                LoggerInstance.debug("Stream ended at tag number %d, before required fields could be parsed", cursor.getCursor());
                                throw BACnetDecodingException.newException("Stream ended at tag number %d, before required fields could be parsed", cursor.getCursor());
                            }

                            if (res == DecodeResult.NoMatch)
                            {
                                // Backtrack and let the next slot deal with the tag.
                                cursor.setCursor(startTagPos);
                            }
                            break;
                    }
                }

                String missingChoiceSet = verifyChoiceSets();
                if (missingChoiceSet != null)
                {
                    if (m_target instanceof AnyValue)
                    {
                        TagHeaderForDecoding tag = cursor.nextTag();
                        if (tag.applicationTag != null)
                        {
                            Object value = tag.readPrimitive(tag.applicationTag, this, null, null);

                            AnyValue av = (AnyValue) m_target;
                            av.anyValue = value;
                            return;
                        }
                    }

                    throw BACnetDecodingException.newException("Missing value for choiceSet '%s'", missingChoiceSet);
                }
            }
        }
    }

    private DecodeResult decodeSlot(TagCursor cursor,
                                    SerializablePiece piece,
                                    int sequence,
                                    boolean isOptional,
                                    boolean isChoice)
    {
        boolean  isUntagged = isUntagged(piece);
        Class<?> type       = piece.actualType;

        if (cursor.isEOF())
        {
            return DecodeResult.CheckForOptional;
        }

        TagHeaderForDecoding tag = cursor.nextTag();

        if (tag.isOpeningTag)
        {
            Object value;

            if (isUntagged)
            {
                if (!Reflection.isSubclassOf(Choice.class, type))
                {
                    throw BACnetDecodingException.newException("Unexpected opening tag in an untagged context");
                }

                cursor.pushBack();
                value = decodeSubValue(type, piece.isList, piece.isArray, piece.fixedArraySize, cursor);
            }
            else
            {
                if (tag.contextTag != sequence)
                {
                    return DecodeResult.NoMatch;
                }

                value = decodeSubValue(type, piece.isList, piece.isArray, piece.fixedArraySize, tag.nestedTags);
            }

            assignValue(piece, value);

            return DecodeResult.Ok;
        }

        if (tag.isContextSpecific())
        {
            if (!isUntagged && tag.contextTag != sequence)
            {
                return DecodeResult.NoMatch;
            }

            if (isUntagged)
            {
                // Backtrack and let the elements of the list deal with the tag.
                cursor.pushBack();

                Object subValue = decodeSubValue(type, piece.isList, piece.isArray, piece.fixedArraySize, cursor);
                assignValue(piece, subValue);

                return DecodeResult.Ok;
            }
        }
        else
        {
            if (isOptional || isChoice)
            {
                // Application tags cannot be used for optional or choice fields.
                return DecodeResult.NoMatch;
            }
        }

        // Byte arrays are handled as primitives.
        if (piece.isArray && piece.actualType == byte.class)
        {
            Object array = tag.readPrimitive(ApplicationTag.OctetString, null, null, null);
            assignValue(piece, array);
            return DecodeResult.Ok;
        }

        if (piece.isList || piece.isArray)
        {
            // Backtrack and let the elements of the list deal with the tag.
            if (isUntagged)
            {
                cursor.pushBack();
            }

            Object value = decodeSubValue(type, piece.isList, piece.isArray, piece.fixedArraySize, cursor);
            assignValue(piece, value);

            return DecodeResult.Ok;
        }

        Object value = tag.readValue(this, piece);
        if (type == Object.class)
        {
            assignValue(piece, value);
            return DecodeResult.Ok;
        }

        if (value == null)
        {
            return DecodeResult.UnexpectedNullValue;
        }

        if (Reflection.canAssignTo(type, value.getClass()))
        {
            assignValue(piece, value);
            return DecodeResult.Ok;
        }

        if (Reflection.isSubclassOf(AnyValue.class, type))
        {
            Object fieldValue = piece.getField(m_target);
            if (!(fieldValue instanceof AnyValue))
            {
                fieldValue = Reflection.newInstance((Class<?>) type);
                assignValue(piece, fieldValue);
            }

            AnyValue av = (AnyValue) fieldValue;
            av.anyValue = value;
            return DecodeResult.Ok;
        }

        if (isUntagged)
        {
            // Backtrack and let the elements of the list deal with the tag.
            cursor.pushBack();

            Object subValue = decodeSubValue(type, piece.isList, piece.isArray, piece.fixedArraySize, cursor);
            assignValue(piece, subValue);

            return DecodeResult.Ok;
        }

        return DecodeResult.NoMatch;
    }

    private void assignValue(SerializablePiece piece,
                             Object value)
    {
        LoggerInstance.debug("assignValue: %s => %s : %s", piece, value != null ? value.getClass() : "<none>", value);

        recordValueForContext(value, isPropertyIndex(piece));

        if (value instanceof CustomWrapper && !(m_target instanceof CustomWrapper))
        {
            //
            // Attempt to unwrap simple primitive custom values.
            //
            CustomWrapper wrapper = (CustomWrapper) value;
            if (wrapper.fields.size() == 1)
            {
                CustomField field = wrapper.fields.get(0);
                if (field.applicationTag != null)
                {
                    value = field.value;
                }
            }
        }

        piece.setField(m_target, value);
    }

    private void recordValueForContext(Object value,
                                       boolean isPropertyIndex)
    {
        BACnetObjectType value1 = Reflection.as(value, BACnetObjectType.class);
        if (value1 != null)
        {
            recordValueForContext(value1.forRequest(), false);
            return;
        }

        BACnetPropertyIdentifier value2 = Reflection.as(value, BACnetPropertyIdentifier.class);
        if (value2 != null)
        {

            recordValueForContext(value2.forRequest(), false);
            return;
        }

        BACnetObjectIdentifier value3 = Reflection.as(value, BACnetObjectIdentifier.class);
        if (value3 != null)
        {
            recordValueForContext(value3.object_type, false);
            return;
        }

        if (isPropertyIndex && value instanceof Unsigned32)
        {
            getNonChoiceContext().m_propertyIndexContext = (Unsigned32) value;
        }

        BACnetObjectTypeOrUnknown value4 = Reflection.as(value, BACnetObjectTypeOrUnknown.class);
        if (value4 != null)
        {
            getNonChoiceContext().m_objectContext = value4;

            LoggerInstance.debug("$$$$  recordValueForContext: BACnetObjectType %s", value);
            return;
        }

        BACnetPropertyIdentifierOrUnknown value5 = Reflection.as(value, BACnetPropertyIdentifierOrUnknown.class);
        if (value5 != null)
        {
            getNonChoiceContext().m_propertyContext = value5;

            LoggerInstance.debug("$$$$  recordValueForContext: BACnetPropertyIdentifier %s", value);
            return;
        }
    }

    //--//

    static class TagCursor
    {
        final List<TagHeaderForDecoding> tags;
        int cursor;

        TagCursor(List<TagHeaderForDecoding> tags)
        {
            this.tags = tags;
        }

        public void pushBack()
        {
            if (cursor == 0)
            {
                throw BACnetDecodingException.newException("Internal error: can't pushback on TagCursor already at the first tag");
            }

            cursor--;
        }

        boolean isEOF()
        {
            return cursor == tags.size();
        }

        TagHeaderForDecoding nextTag()
        {
            return tags.get(cursor++);
        }

        int getCursor()
        {
            return cursor;
        }

        void setCursor(int cursor)
        {
            this.cursor = cursor;
        }
    }

    private Object decodeSubValue(Class<?> actualType,
                                  boolean isList,
                                  boolean isArray,
                                  int fixedArraySize,
                                  List<TagHeaderForDecoding> nestedTags)
    {
        TagCursor tagCursor = new TagCursor(nestedTags);

        return decodeSubValue(actualType, isList, isArray, fixedArraySize, tagCursor);
    }

    private Object decodeSubValue(Class<?> actualType,
                                  boolean isList,
                                  boolean isArray,
                                  int fixedArraySize,
                                  TagCursor tagCursor)
    {
        Unsigned32 index = null;

        if (actualType == Object.class)
        {
            if (!isList && !isArray)
            {
                TagContextCommon.ContextType ct = inferTypeOfValueFromContext();
                if (ct != null)
                {
                    LoggerInstance.debug("### Inferred type from %s to %s (isList=%s isArray=%s)", actualType, ct.type, ct.isList, ct.isArray);
                    actualType = ct.type;
                    isList     = ct.isList;
                    isArray    = ct.isArray;

                    index = getPropertyIndexContext();
                }
            }
        }

        if (isArray && actualType == byte.class)
        {
            return decodePrimitive(tagCursor);
        }

        if (isList || isArray)
        {
            if (index != null)
            {
                long idx = index.unboxUnsigned();

                if (isArray && idx == 0)
                {
                    return decodePrimitive(tagCursor);
                }

                return decodeSubValue(tagCursor, actualType);
            }
            else
            {
                List<Object> list = Lists.newArrayList();

                while (!tagCursor.isEOF())
                {
                    Object value = decodeSubValue(tagCursor, actualType);
                    if (value == Null.instance)
                    {
                        value = null;
                    }

                    list.add(value);

                    if (fixedArraySize > 0)
                    {
                        fixedArraySize--;

                        if (fixedArraySize == 0)
                        {
                            break;
                        }
                    }
                }

                while (fixedArraySize-- > 0)
                {
                    list.add(null);
                }

                if (isArray)
                {
                    Object array = Array.newInstance(actualType, list.size());
                    for (int i = 0; i < list.size(); i++)
                         Array.set(array, i, list.get(i));

                    return array;
                }

                return list;
            }
        }

        return decodeSubValue(tagCursor, actualType);
    }

    private Object decodeSubValue(TagCursor tagCursor,
                                  Class<?> clz)
    {
        if (Reflection.isSubclassOf(Constructed.class, clz))
        {
            return decodeSubValueConstructed(tagCursor, clz);
        }

        // Special case for empty properties.
        if (clz == Object.class && tagCursor.isEOF())
        {
            return null;
        }

        return decodePrimitive(tagCursor);
    }

    private Object decodePrimitive(TagCursor tagCursor)
    {
        PrimitiveWrapper wrapper = decodeSubValueConstructed(tagCursor, PrimitiveWrapper.class);
        return wrapper.value;
    }

    private <T> T decodeSubValueConstructed(TagCursor tagCursor,
                                            Class<T> clz)
    {
        T                     value      = Reflection.newInstance(clz);
        TagContextForDecoding subContext = new TagContextForDecoding(this, value);
        subContext.decode(tagCursor);
        return value;
    }
}
