/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import java.util.BitSet;
import java.util.Optional;

import com.optio3.lang.Unsigned32;
import com.optio3.protocol.model.bacnet.Choice;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetLogStatus;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetStatusFlags;
import com.optio3.serialization.SerializationTag;

public final class BACnetLogRecord extends Sequence
{
    public static final class Datum extends Choice
    {
        @SerializationTag(number = 0)
        public BACnetLogStatus log_status;

        @SerializationTag(number = 1)
        public boolean boolean_value;

        @SerializationTag(number = 2)
        public float real_value;

        @SerializationTag(number = 3)
        public Unsigned32 enumerated_value; // ENUMERATED

        @SerializationTag(number = 4)
        public Unsigned32 unsigned_value;

        @SerializationTag(number = 5)
        public long integer_value;

        @SerializationTag(number = 6)
        public BitSet bitstring_value;

        @SerializationTag(number = 7)
        public Object null_value;

        @SerializationTag(number = 8)
        public BACnetError failure;

        @SerializationTag(number = 9)
        public float time_change;

        @SerializationTag(number = 10)
        public Object any_value;
    }

    @SerializationTag(number = 0)
    public BACnetDateTime timestamp;

    @SerializationTag(number = 1)
    public Datum log_datum;

    @SerializationTag(number = 2)
    public Optional<BACnetStatusFlags> status_flags;
}
