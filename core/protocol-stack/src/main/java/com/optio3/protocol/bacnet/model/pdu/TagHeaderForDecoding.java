/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu;

import java.util.BitSet;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.lang.Unsigned32;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.protocol.bacnet.model.enums.ApplicationTag;
import com.optio3.protocol.bacnet.model.enums.CharsetEncoding;
import com.optio3.protocol.model.bacnet.BACnetDate;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.BACnetTime;
import com.optio3.protocol.model.bacnet.error.BACnetDecodingException;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.SerializablePiece;
import com.optio3.serialization.SerializationHelper;
import com.optio3.serialization.TypeDescriptor;
import com.optio3.serialization.TypeDescriptorKind;
import com.optio3.serialization.TypedBitSet;
import com.optio3.stream.InputBuffer;

public final class TagHeaderForDecoding extends TagHeaderCommon
{
    public static final Logger LoggerInstance = TagHeaderCommon.LoggerInstance.createSubLogger(TagHeaderForDecoding.class);

    //--//

    private final InputBuffer m_payload;

    public final List<TagHeaderForDecoding> nestedTags;

    public TagHeaderForDecoding(InputBuffer buffer)
    {
        int b = buffer.read1ByteUnsigned();

        int  tag   = extract(b, c_TagOffset, c_TagSize);
        long value = extract(b, c_ValueOffset, c_ValueSize);

        if (tag == c_markerTagExtension)
        {
            tag = buffer.read1ByteUnsigned();
        }

        boolean isContextSpecific = extract(b, c_ClassOffset);
        if (isContextSpecific)
        {
            contextTag = tag;

            switch ((int) value)
            {
                case c_markerOpeningTag:
                    isOpeningTag = true;
                    value = 0; // So that length will be zero.
                    break;

                case c_markerClosingTag:
                    isClosingTag = true;
                    value = 0; // So that length will be zero.
                    break;
            }
        }
        else
        {
            applicationTag = ApplicationTag.parse((byte) tag);
            switch (applicationTag)
            {
                case Boolean:
                    m_boolValue = value == 1;
                    value = 0; // So that length will be zero.
                    break;

                case Null:
                    value = 0; // So that length will be zero.
                    break;

                default:
                    break;
            }
        }

        if (value > c_markerLength1Byte)
        {
            throw BACnetDecodingException.newException("Invalid value for BACnet tag length: %s", value);
        }

        if (value == c_markerLength1Byte)
        {
            value = buffer.read1ByteUnsigned();
            if (value == c_markerLength2Bytes)
            {
                value = buffer.read2BytesUnsigned();
            }
            else if (value == c_markerLength4Bytes)
            {
                value = buffer.read4BytesUnsigned();
            }
        }

        length = (int) value;

        dump();

        m_payload = buffer.readNestedBlock(length);

        if (isOpeningTag)
        {
            List<TagHeaderForDecoding> list = Lists.newArrayList();

            int pos = buffer.getPosition();

            while (true)
            {
                if (buffer.isEOF())
                {
                    throw BACnetDecodingException.newException("Stream ended while parsing context %s, range [%d..%d]", contextTag, pos, buffer.getPosition());
                }

                TagHeaderForDecoding nextTag = new TagHeaderForDecoding(buffer);
                if (nextTag.isClosingTag)
                {
                    if (nextTag.contextTag == contextTag)
                    {
                        break;
                    }

                    throw BACnetDecodingException.newException("Expecting closing tag for context %s, got one for context %s, range [%d..%d]",
                                                               contextTag,
                                                               nextTag.contextTag,
                                                               pos,
                                                               buffer.getPosition());
                }

                list.add(nextTag);
            }

            nestedTags = Collections.unmodifiableList(list);
        }
        else
        {
            nestedTags = Collections.emptyList();
        }
    }

    private void dump()
    {
        if (!LoggerInstance.isEnabled(Severity.Debug))
        {
            return;
        }

        if (isClosingTag)
        {
            LoggerInstance.debug(">>Closing TAG: %s", contextTag);
        }
        else if (isOpeningTag)
        {
            LoggerInstance.debug(">>Opening TAG: %s", contextTag);
        }
        else if (applicationTag != null)
        {
            LoggerInstance.debug(">>applicationTag: %s, length: %d", applicationTag, length);
        }
        else
        {
            LoggerInstance.debug(">>contextTag: %s, length: %d", contextTag, length);
        }
    }

    //--//

    public Object readValue(TagContextForDecoding context,
                            SerializablePiece piece)
    {
        if (applicationTag == ApplicationTag.Boolean)
        {
            return m_boolValue;
        }

        if (applicationTag == ApplicationTag.Null)
        {
            return null;
        }

        m_payload.setPosition(0);

        TypeDescriptor td   = piece.typeDescriptor;
        Class<?>       type = piece.actualType;

        ApplicationTag appTag = applicationTag;
        if (appTag != null)
        {
            return readPrimitive(appTag, context, td, type);
        }

        if (type == String.class)
        {
            return readPrimitive(ApplicationTag.CharacterString, context, td, null);
        }

        if (type == BACnetObjectIdentifier.class)
        {
            return readPrimitive(ApplicationTag.BACnetObjectIdentifier, context, td, null);
        }

        if (TypedBitSet.class.isAssignableFrom(type))
        {
            return readPrimitive(ApplicationTag.BitString, context, td, type);
        }

        if (type == BACnetTime.class)
        {
            return readPrimitive(ApplicationTag.Time, context, td, null);
        }

        if (type == BACnetDate.class)
        {
            return readPrimitive(ApplicationTag.Date, context, td, null);
        }

        if (piece.isArray && type == byte.class)
        {
            return readPrimitive(ApplicationTag.OctetString, context, td, null);
        }

        if (td != null)
        {
            return piece.readPrimitiveValue(m_payload);
        }

        if (type == Object.class)
        {
            return readPrimitive(ApplicationTag.OctetString, context, null, null);
        }

        throw BACnetDecodingException.newException("INTERNAL ERROR: unexpected tag: %s", this);
    }

    public Object readPrimitive(ApplicationTag primitive,
                                TagContextForDecoding context,
                                TypeDescriptor td,
                                Class<?> actualType)
    {
        switch (primitive)
        {
            case BACnetObjectIdentifier:
            {
                if (length != 4)
                {
                    throw BACnetDecodingException.newException("Invalid BACnetObjectIdentifier length: should be 4, got %d", length);
                }

                BACnetObjectIdentifier res = new BACnetObjectIdentifier();
                SerializationHelper.read(m_payload, res);
                return res;
            }

            case BitString:
            {
                BitSet bs = readBitString(m_payload, -1);

                TagContextCommon.ContextType ct = context.inferTypeOfValueFromContext();
                if (ct != null)
                {
                    if (Reflection.isSubclassOf(TypedBitSet.class, ct.type))
                    {
                        return Reflection.newInstance(ct.type, bs);
                    }
                }

                if (actualType != null)
                {
                    if (Reflection.isSubclassOf(TypedBitSet.class, actualType))
                    {
                        return Reflection.newInstance(actualType, bs);
                    }
                }

                return bs;
            }

            case Boolean:
                return m_boolValue;

            case CharacterString:
                return readString(m_payload, -1);

            case Date:
            {
                BACnetDate res = new BACnetDate();
                SerializationHelper.read(m_payload, res);
                return res;
            }

            case Double:
                return m_payload.readDouble();

            case Enumerated:
            {
                if (td == null)
                {
                    TagContextCommon.ContextType ct = context.inferTypeOfValueFromContext();
                    if (ct != null)
                    {
                        td = Reflection.getDescriptor(ct.type);
                    }
                }

                if (td == null)
                {
                    td = Reflection.getDescriptor(Unsigned32.class);
                }

                return readInteger(length, TypeDescriptorKind.integerUnsigned, td);
            }

            case Null:
                return null;

            case OctetString:
                return m_payload.readByteArray(length);

            case Real:
                return m_payload.readFloat();

            case SignedInteger:
            {
                if (td == null)
                {
                    td = Reflection.getDescriptor(long.class);
                }

                return readInteger(length, TypeDescriptorKind.integerSigned, td);
            }

            case Time:
            {
                BACnetTime res = new BACnetTime();
                SerializationHelper.read(m_payload, res);
                return res;
            }

            case UnsignedInteger:
            {
                if (td == null)
                {
                    td = Reflection.getDescriptor(Unsigned32.class);
                }

                return readInteger(length, TypeDescriptorKind.integerUnsigned, td);
            }
        }

        throw BACnetDecodingException.newException("Unexpected application tag: %s", primitive);
    }

    private Object readInteger(int length,
                               TypeDescriptorKind tdk,
                               TypeDescriptor td)
    {
        long value = m_payload.readGenericInteger(length, tdk);
        return td.fromLongValue(value);
    }

    //--//

    public static BitSet readBitString(InputBuffer ib,
                                       int length)
    {
        //
        // A bit string value shall contain an initial octet and zero or more subsequent octets containing the bit string.
        // The initial octet shall encode, as an unsigned binary integer, the number of unused bits in the final subsequent octet.
        // The number of unused bits shall be in the range zero to seven, inclusive.
        // Bit strings defined in this standard, e.g., the Status_Flags property, shall be encoded in the order of definition,
        // with the first defined Boolean value in the most significant bit, i.e. bit 7, of the first subsequent octet.
        // The bits in the bitstring shall be placed in bits 7 to 0 of the first subsequent octet, followed by bits 7 to 0 of the second subsequent octet,
        // followed by bits 7 to 0 of each octet in turn, followed by as many bits as are needed of the final subsequent octet,
        // commencing with bit 7. Undefined bits shall be zero.
        //

        if (length < 0)
        {
            length = ib.remainingLength();
        }

        int unsetBitsInLastByte = ib.read1ByteUnsigned();
        int numBits             = (length - 1) * 8 - unsetBitsInLastByte;

        int    mask  = 0;
        int    value = 0;
        BitSet res   = new BitSet(numBits);

        for (int bitIndex = 0; bitIndex < numBits; bitIndex++)
        {
            if (mask == 0)
            {
                mask  = 0x80;
                value = ib.read1ByteUnsigned();
            }

            if ((value & mask) != 0)
            {
                res.set(bitIndex);
            }

            mask >>= 1;
        }

        return res;
    }

    public static String readString(InputBuffer ib,
                                    int length)
    {
        if (length < 0)
        {
            length = ib.remainingLength();
        }

        if (length == 0)
        {
            return null;
        }

        byte            charset = (byte) ib.read1ByteUnsigned();
        CharsetEncoding t       = CharsetEncoding.parse(charset);
        if (t == null)
        {
            throw BACnetDecodingException.newException("Encoding %s not supported", charset);
        }

        return ib.readBytesAsString(length - 1, t.charset());
    }
}
