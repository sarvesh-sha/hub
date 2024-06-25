/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.confirmed;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.lang.Unsigned32;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceRequest;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceResponse;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.Choice;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.serialization.SerializationTag;

public final class AtomicReadFile extends ConfirmedServiceRequest
{
    public static final class Ack extends ConfirmedServiceResponse<AtomicReadFile>
    {
        public static final class StreamAccess extends Sequence
        {
            @SerializationTag(number = 0)
            public int file_start_position;

            @SerializationTag(number = 1)
            public byte[] file_data;
        }

        public static final class RecordAccess extends Sequence
        {
            @SerializationTag(number = 0)
            public int file_start_record;

            @SerializationTag(number = 1)
            public Unsigned32 returned_record_count;

            @SerializationTag(number = 2)
            public List<byte[]> file_record_data = Lists.newArrayList();
        }

        public static final class AccessMethods extends Choice
        {
            @SerializationTag(number = 0)
            public StreamAccess stream_access;

            @SerializationTag(number = 1)
            public RecordAccess record_access;
        }

        @SerializationTag(number = 0)
        public boolean end_of_file;

        @SerializationTag(number = 1)
        public AccessMethods access_method;
    }

    //--//

    public static final class StreamAccess extends Sequence
    {
        @SerializationTag(number = 0)
        public int file_start_position;

        @SerializationTag(number = 1)
        public Unsigned32 requested_octet_count;
    }

    public static final class RecordAccess extends Sequence
    {
        @SerializationTag(number = 0)
        public int file_start_record;

        @SerializationTag(number = 1)
        public Unsigned32 requested_record_count;
    }

    public static final class AccessMethods extends Choice
    {
        @SerializationTag(number = 0)
        public StreamAccess stream_access;

        @SerializationTag(number = 1)
        public RecordAccess record_access;
    }

    @SerializationTag(number = 0)
    public BACnetObjectIdentifier file_identifier;

    @SerializationTag(number = 1)
    public AccessMethods access_method;
}
