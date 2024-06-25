/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digitalmatter.wire;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.AsyncMessageRequestPayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.AsyncMessageResponsePayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.AsyncSessionCompletePayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.CommitRequestPayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.CommitResponsePayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.DisconnectSocketPayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.HelloPayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.HelloResponsePayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.RequestAsyncSessionStartPayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.SendDataRecordsPayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.TimeRequestPayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.TimeResponsePayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.UnknownDataPayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.VersionDataPayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.VersionDataResponsePayload;
import com.optio3.collection.ExpandableArrayOfBytes;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.serialization.CustomTypeDescriptor;
import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.SerializationHelper;
import com.optio3.serialization.SerializationTag;
import com.optio3.serialization.TypeDescriptor;
import com.optio3.serialization.TypeDescriptorFactory;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;
import com.optio3.util.BufferUtils;

public class Packet extends BaseWireModel
{
    public enum MessageType
    {
        // @formatter:off
        Hello                   (0x0 , HelloPayload.class),
        HelloResponse           (0x1 , HelloResponsePayload.class),
        SendDataRecords         (0x4 , SendDataRecordsPayload.class),
        CommitRequest           (0x5 , CommitRequestPayload.class),
        CommitResponse          (0x6 , CommitResponsePayload.class),
        VersionData             (0x14, VersionDataPayload.class),
        VersionDataResponse     (0x15, VersionDataResponsePayload.class),
        AsyncMessageRequest     (0x20, AsyncMessageRequestPayload.class),
        AsyncMessageResponse    (0x21, AsyncMessageResponsePayload.class),
        RequestAsyncSessionStart(0x22, RequestAsyncSessionStartPayload.class),
        AsyncSessionComplete    (0x23, AsyncSessionCompletePayload.class),
        DisconnectSocket        (0x26, DisconnectSocketPayload.class),
        TimeRequest             (0x30, TimeRequestPayload.class),
        TimeResponse            (0x31, TimeResponsePayload.class);
        // @formatter:on

        private final byte                           m_encoding;
        private final Class<? extends BaseWireModel> m_clz;
        private final MessageTypeOrUnknown           m_singleton;

        MessageType(int encoding,
                    Class<? extends BaseWireModel> clz)
        {
            m_encoding  = (byte) encoding;
            m_clz       = clz;
            m_singleton = new MessageTypeOrUnknown(this);
        }

        public static MessageType getEnum(BaseWireModel obj)
        {
            Class<? extends BaseWireModel> clz = obj.getClass();

            for (MessageType t : values())
            {
                if (t.m_clz == clz)
                {
                    return t;
                }
            }

            return null;
        }

        public static MessageType parse(String value)
        {
            for (MessageType t : values())
            {
                if (t.name()
                     .equalsIgnoreCase(value))
                {
                    return t;
                }
            }

            return null;
        }

        @HandlerForDecoding
        public static MessageType parse(byte value)
        {
            for (MessageType t : values())
            {
                if (t.m_encoding == value)
                {
                    return t;
                }
            }

            return null;
        }

        @HandlerForEncoding
        public byte encoding()
        {
            return m_encoding;
        }

        public Class<? extends BaseWireModel> getPayload()
        {
            return m_clz;
        }

        public MessageTypeOrUnknown forRequest()
        {
            return m_singleton;
        }
    }

    @CustomTypeDescriptor(factory = MessageTypeOrUnknown.Factory.class)
    public static class MessageTypeOrUnknown
    {
        private static final int c_unknownMarker = -1;

        public static class Factory extends TypeDescriptorFactory
        {
            @Override
            public TypeDescriptor create(Class<?> clz)
            {
                TypeDescriptor td = Reflection.getDescriptor(MessageType.class);

                return new TypeDescriptor(clz, td.size, td.kind)
                {
                    @Override
                    public Object fromLongValue(long value)
                    {
                        MessageType objectType = MessageType.parse((byte) value);

                        if (objectType != null)
                        {
                            return objectType.forRequest();
                        }

                        return new MessageTypeOrUnknown(null, value);
                    }

                    @Override
                    public long asLongValue(Object value)
                    {
                        MessageTypeOrUnknown v = (MessageTypeOrUnknown) value;

                        return v.asLongValue();
                    }
                };
            }
        }

        //--//

        public final MessageType value;
        public final long        unknown;

        private MessageTypeOrUnknown(MessageType value,
                                     long unknown)
        {
            this.value   = value;
            this.unknown = unknown;
        }

        MessageTypeOrUnknown(MessageType value)
        {
            this.value   = value;
            this.unknown = c_unknownMarker;
        }

        @JsonCreator
        private MessageTypeOrUnknown(String value)
        {
            this.value   = MessageType.parse(value);
            this.unknown = this.value != null ? c_unknownMarker : Long.parseLong(value);
        }

        @JsonValue
        public String toJsonValue()
        {
            return unknown != c_unknownMarker ? Long.toString(unknown) : value.name();
        }

        public long asLongValue()
        {
            if (value != null)
            {
                return value.encoding();
            }

            return unknown;
        }

        @JsonIgnore
        public boolean isUnknown()
        {
            return unknown != c_unknownMarker;
        }

        public static MessageTypeOrUnknown parse(String value)
        {
            return new MessageTypeOrUnknown(value);
        }

        public Class<? extends BaseWireModel> getPayload()
        {
            if (value != null)
            {
                return value.getPayload();
            }

            return UnknownDataPayload.class;
        }

        //--//

        @Override
        public String toString()
        {
            if (value != null)
            {
                return value.toString();
            }

            return Long.toString(unknown);
        }
    }

    //--//

    //
    //Length = 5
    //        Offset   Data Type   Length   Description      Value
    //        0        BYTE        1        Sync Byte        0x02
    //        1        BYTE        1        Sync Byte        0x55
    //        2        BYTE        1        Message Type
    //        3        UINT16      2        Payload Length (excluding header)
    //
    private static final byte c_syncByte1 = 0x02;
    private static final byte c_syncByte2 = 0x55;

    @SerializationTag(number = 1)
    public byte syncByte1;

    @SerializationTag(number = 2)
    public byte syncByte2;

    @SerializationTag(number = 3, width = 8)
    public MessageTypeOrUnknown messageType;

    @SerializationTag(number = 4)
    public BaseWireModel payload;

    //--//

    @Override
    public boolean encodeValue(String fieldName,
                               OutputBuffer buffer,
                               Object value)
    {
        switch (fieldName)
        {
            case "payload":
                try (OutputBuffer nested = new OutputBuffer())
                {
                    nested.littleEndian = true;

                    SerializationHelper.write(nested, payload);

                    buffer.emit2Bytes(nested.size());
                    buffer.emitNestedBlock(nested);
                    return true;
                }
        }

        return false;
    }

    @Override
    public Optional<Object> provideValue(String fieldName,
                                         InputBuffer buffer)
    {
        switch (fieldName)
        {
            case "payload":
                int len = buffer.read2BytesUnsigned();
                InputBuffer nested = buffer.readNestedBlock(len);
                nested.littleEndian = true;

                BaseWireModel payload = Reflection.newInstance(messageType.getPayload());
                SerializationHelper.read(nested, payload);
                return Optional.of(payload);
        }

        return Optional.empty();
    }

    //--//

    public static Packet decode(Logger logger,
                                InputStream stream) throws
                                                    IOException
    {
        boolean dumpBuffer = true;

        try (InputBuffer ib = InputBuffer.takeOwnership(ExpandableArrayOfBytes.create(stream, -1)))
        {
            ib.littleEndian = true;

            try
            {
                Packet packet = new Packet();

                SerializationHelper.read(ib, packet);

                dumpBuffer = false;

                if (packet.syncByte1 != c_syncByte1 || packet.syncByte2 != c_syncByte2)
                {
                    return null;
                }

                return packet;
            }
            finally
            {
                if (dumpBuffer)
                {
                    int len = ib.size();
                    ib.setPosition(0);
                    byte[] badPacket = ib.readByteArray(len);

                    logger.error("Failed to parse DigitalMatter packet:");
                    BufferUtils.convertToHex(badPacket, 0, len, 32, true, (line) -> logger.error("  %s", line));
                }
                else if (logger.isEnabled(Severity.DebugObnoxious))
                {
                    int len = ib.size();
                    ib.setPosition(0);
                    byte[] badPacket = ib.readByteArray(len);

                    logger.debugObnoxious("Raw packet:");
                    BufferUtils.convertToHex(badPacket, 0, len, 32, true, (line) -> logger.debugObnoxious("  %s", line));
                }
            }
        }
    }

    public byte[] encode()
    {
        try (OutputBuffer ob = new OutputBuffer())
        {
            ob.littleEndian = true;

            syncByte1 = c_syncByte1;
            syncByte2 = c_syncByte2;

            MessageType val = MessageType.getEnum(payload);
            if (val != null)
            {
                messageType = val.forRequest();
            }

            SerializationHelper.write(ob, this);
            return ob.toByteArray();
        }
    }
}
