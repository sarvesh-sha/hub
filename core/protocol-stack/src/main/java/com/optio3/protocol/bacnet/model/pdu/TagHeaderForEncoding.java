/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu;

import java.util.BitSet;

import com.optio3.logging.Logger;
import com.optio3.protocol.bacnet.model.enums.ApplicationTag;
import com.optio3.protocol.bacnet.model.enums.CharsetEncoding;
import com.optio3.protocol.model.bacnet.BACnetDate;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.BACnetTime;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.error.BACnetDecodingException;
import com.optio3.serialization.Null;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.SerializationHelper;
import com.optio3.serialization.TypeDescriptor;
import com.optio3.serialization.TypedBitSet;
import com.optio3.stream.OutputBuffer;

public final class TagHeaderForEncoding extends TagHeaderCommon
{
    public static final Logger LoggerInstance = TagHeaderCommon.LoggerInstance.createSubLogger(TagHeaderForDecoding.class);

    //--//

    private TagHeaderForEncoding()
    {
    }

    private void emit(OutputBuffer buffer)
    {
        int header = 0;

        int     tag;
        boolean emitTagExtension    = false;
        boolean emitLengthExtension = false;

        if (applicationTag != null)
        {
            header |= insert(c_TagOffset, c_TagSize, applicationTag.encoding());
        }
        else
        {
            header |= insert(c_ClassOffset, true);

            if (isOpeningTag)
            {
                header |= insert(c_ValueOffset, c_ValueSize, c_markerOpeningTag);
                length = 0;
            }
            else if (isClosingTag)
            {
                header |= insert(c_ValueOffset, c_ValueSize, c_markerClosingTag);
                length = 0;
            }

            if (contextTag >= c_markerTagExtension)
            {
                emitTagExtension = true;
                tag              = c_markerTagExtension;
            }
            else
            {
                tag = contextTag;
            }

            header |= insert(c_TagOffset, c_TagSize, tag);
        }

        if (applicationTag == ApplicationTag.Boolean)
        {
            header |= insert(c_ValueOffset, c_ValueSize, m_boolValue ? 1 : 0);
        }
        else if (length >= c_markerLength1Byte)
        {
            header |= insert(c_ValueOffset, c_ValueSize, c_markerLength1Byte);
            emitLengthExtension = true;
        }
        else
        {
            header |= insert(c_ValueOffset, c_ValueSize, length);
        }

        buffer.emit1Byte(header);
        if (emitTagExtension)
        {
            buffer.emit1Byte(contextTag);
        }

        if (emitLengthExtension)
        {
            if (length <= 253)
            {
                buffer.emit1Byte(length);
            }
            else if (length <= 65535)
            {
                buffer.emit1Byte(c_markerLength2Bytes);
                buffer.emit2Bytes(length);
            }
            else
            {
                buffer.emit1Byte(c_markerLength4Bytes);
                buffer.emit4Bytes(length);
            }
        }
    }

    public static TagHeaderForEncoding createApplicationTag(ApplicationTag applicationTag)
    {
        TagHeaderForEncoding tag = new TagHeaderForEncoding();
        tag.applicationTag = applicationTag;
        return tag;
    }

    public static TagHeaderForEncoding createContextTag(int contextTag)
    {
        TagHeaderForEncoding tag = new TagHeaderForEncoding();
        tag.contextTag = contextTag;
        return tag;
    }

    public static void emitOpeningTag(OutputBuffer buffer,
                                      boolean isUntagged,
                                      int sequence)
    {
        if (isUntagged)
        {
            return;
        }

        TagHeaderForEncoding tag = new TagHeaderForEncoding();
        tag.isOpeningTag = true;
        tag.contextTag   = sequence;
        tag.emit(buffer);
    }

    public static void emitClosingTag(OutputBuffer buffer,
                                      boolean isUntagged,
                                      int sequence)
    {
        if (isUntagged)
        {
            return;
        }

        TagHeaderForEncoding tag = new TagHeaderForEncoding();
        tag.isClosingTag = true;
        tag.contextTag   = sequence;
        tag.emit(buffer);
    }

    public static ApplicationTag classifyValue(Object value)
    {
        if (value == null || value == Null.instance)
        {
            return ApplicationTag.Null;
        }

        Class<?>       clz = value.getClass();
        TypeDescriptor td  = Reflection.getDescriptor(clz);
        if (td != null)
        {
            if (value instanceof Boolean)
            {
                return ApplicationTag.Boolean;
            }

            if (value instanceof Enum)
            {
                return ApplicationTag.Enumerated;
            }

            if (value instanceof BACnetObjectTypeOrUnknown)
            {
                return ApplicationTag.Enumerated;
            }
            if (value instanceof BACnetPropertyIdentifierOrUnknown)
            {
                return ApplicationTag.Enumerated;
            }

            if (td.kind.isInteger)
            {
                return td.kind.isSigned ? ApplicationTag.SignedInteger : ApplicationTag.UnsignedInteger;
            }

            return value instanceof Double ? ApplicationTag.Double : ApplicationTag.Real;
        }

        if (clz == BACnetObjectIdentifier.class)
        {
            return ApplicationTag.BACnetObjectIdentifier;
        }

        if (value instanceof BitSet)
        {
            return ApplicationTag.BitString;
        }

        if (value instanceof TypedBitSet<?>)
        {
            return ApplicationTag.BitString;
        }

        if (clz == String.class)
        {
            return ApplicationTag.CharacterString;
        }

        if (value instanceof BACnetDate)
        {
            return ApplicationTag.Date;
        }

        if (value instanceof BACnetTime)
        {
            return ApplicationTag.Time;
        }

        if (value instanceof byte[])
        {
            return ApplicationTag.OctetString;
        }

        return null;
    }

    //--//

    public void emitValue(OutputBuffer buffer,
                          TypeDescriptor td,
                          Object value)
    {
        if (isContextSpecific())
        {
            emitContextSpecificValue(buffer, td, value);
        }
        else
        {
            emitApplicationValue(buffer, value);
        }
    }

    public void emitApplicationValue(OutputBuffer buffer,
                                     Object value)
    {
        if (applicationTag == ApplicationTag.Null)
        {
            emit(buffer);
            return;
        }

        if (applicationTag == ApplicationTag.Boolean)
        {
            m_boolValue = (Boolean) value;

            emit(buffer);
            return;
        }

        try (OutputBuffer inner = new OutputBuffer())
        {
            emitPrimitive(inner, applicationTag, value);

            length = inner.size();

            emit(buffer);
            buffer.emitNestedBlock(inner);
        }
    }

    public void emitContextSpecificValue(OutputBuffer buffer,
                                         TypeDescriptor td,
                                         Object value)
    {
        try (OutputBuffer inner = new OutputBuffer())
        {
            if (td != null)
            {
                emitNumber(inner, value);
            }
            else
            {
                ApplicationTag appTag = classifyValue(value);
                if (appTag != null)
                {
                    emitPrimitive(inner, appTag, value);
                }
                else
                {
                    throw BACnetDecodingException.newException("INTERNAL ERROR: unexpected value: %s",
                                                               value.getClass()
                                                                    .getName());
                }
            }

            //--//

            length = inner.size();
            emit(buffer);
            buffer.emitNestedBlock(inner);
        }
    }

    private static void emitPrimitive(OutputBuffer buffer,
                                      ApplicationTag primitive,
                                      Object value)
    {
        switch (primitive)
        {
            case BACnetObjectIdentifier:
                SerializationHelper.write(buffer, value);
                return;

            case BitString:
                TypedBitSet<?> bt = Reflection.as(value, TypedBitSet.class);
                if (bt != null)
                {
                    emitAsBitString(buffer, bt.toBitSet(), bt.length());
                }
                else
                {
                    emitAsBitString(buffer, (BitSet) value);
                }
                return;

            case CharacterString:
                emitString(buffer, (String) value, null);
                return;

            case Date:
                SerializationHelper.write(buffer, value);
                return;

            case Double:
                buffer.emit((double) value);
                return;

            case Enumerated:
                emitNumber(buffer, value);
                return;

            case Null:
                return;

            case OctetString:
                buffer.emit((byte[]) value);
                return;

            case Real:
                buffer.emit((float) value);
                return;

            case UnsignedInteger:
                emitNumber(buffer, value);
                return;

            case SignedInteger:
                emitNumber(buffer, value);
                return;

            case Time:
                SerializationHelper.write(buffer, value);
                return;
        }

        throw BACnetDecodingException.newException("Unexpected application tag: %s", primitive);
    }

    private static void emitNumber(OutputBuffer buffer,
                                   Object value)
    {
        TypeDescriptor td  = Reflection.getDescriptor(value.getClass());
        long           num = td.asLongValue(value);

        int len = 1;
        while (len < 8)
        {
            long maskedNum = td.kind.signAwareMasking(num, len * 8);

            if (maskedNum == num)
            {
                break;
            }

            len++;
        }

        buffer.emitGenericInteger(num, len);
    }

    //--//

    public static void emitAsBitString(OutputBuffer ob,
                                       BitSet v)
    {
        emitAsBitString(ob, v, v.length());
    }

    public static void emitAsBitString(OutputBuffer ob,
                                       BitSet v,
                                       int numBits)
    {
        //
        // A bit string value shall contain an initial octet and zero or more
        // subsequent octets containing the bit string.
        // The initial octet shall encode, as an unsigned binary integer, the
        // number of unused bits in the final subsequent octet.
        // The number of unused bits shall be in the range zero to seven,
        // inclusive.
        // Bit strings defined in this standard, e.g., the Status_Flags
        // property, shall be encoded in the order of definition,
        // with the first defined Boolean value in the most significant bit,
        // i.e. bit 7, of the first subsequent octet.
        // The bits in the bitstring shall be placed in bits 7 to 0 of the first
        // subsequent octet, followed by bits 7 to 0 of the second subsequent
        // octet,
        // followed by bits 7 to 0 of each octet in turn, followed by as many
        // bits as are needed of the final subsequent octet,
        // commencing with bit 7. Undefined bits shall be zero.
        //

        int setBitsInLastByte   = (numBits % 8);
        int unsetBitsInLastByte = (8 - setBitsInLastByte) % 8;
        ob.emit1Byte(unsetBitsInLastByte);

        int bitIndex = 0;
        while (bitIndex < numBits)
        {
            int val  = 0;
            int mask = 0x80;
            while (mask != 0 && bitIndex < numBits)
            {
                if (v.get(bitIndex))
                {
                    val |= mask;
                }

                mask >>= 1;
                bitIndex++;
            }
            ob.emit1Byte(val);
        }
    }

    public static void emitString(OutputBuffer ob,
                                  String v,
                                  CharsetEncoding charset)
    {
        if (charset == null)
        {
            charset = CharsetEncoding.UTF_8;
        }

        ob.emit1Byte(charset.encoding());
        ob.emitStringAsBytes(v, charset.charset());
    }
}
