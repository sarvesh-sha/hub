/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed.choice;

import java.util.BitSet;
import java.util.List;
import java.util.Optional;

import com.optio3.lang.Unsigned32;
import com.optio3.protocol.model.bacnet.AnyValue;
import com.optio3.protocol.model.bacnet.Choice;
import com.optio3.protocol.model.bacnet.constructed.BACnetError;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetLogStatus;
import com.optio3.serialization.SerializationTag;

public final class BACnetLogData extends Choice
{
    @SerializationTag(number = 0)
    public BACnetLogStatus log_status;

    @SerializationTag(number = 1)
    public List<typefor_log_data> log_data;

    public static final class typefor_log_data extends Choice
    {
        @SerializationTag(number = 0)
        public boolean boolean_value;

        @SerializationTag(number = 1)
        public float real_value;

        @SerializationTag(number = 2)
        public Enum<?> enumerated_value;

        @SerializationTag(number = 3)
        public Unsigned32 unsigned_value;

        @SerializationTag(number = 4)
        public int integer_value;

        @SerializationTag(number = 5)
        public BitSet bitstring_value;

        @SerializationTag(number = 6)
        public Object null_value = null;

        @SerializationTag(number = 7)
        public BACnetError failure;

        @SerializationTag(number = 8)
        public Optional<AnyValue> any_value;
    }
}
