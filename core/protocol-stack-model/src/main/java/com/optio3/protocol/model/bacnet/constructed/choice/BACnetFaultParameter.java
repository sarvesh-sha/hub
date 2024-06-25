/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed.choice;

import java.util.List;
import java.util.Optional;

import com.optio3.lang.Unsigned16;
import com.optio3.lang.Unsigned32;
import com.optio3.protocol.model.bacnet.AnyValue;
import com.optio3.protocol.model.bacnet.Choice;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.constructed.BACnetDeviceObjectPropertyReference;
import com.optio3.protocol.model.bacnet.enums.BACnetLifeSafetyState;
import com.optio3.serialization.SerializationTag;

public final class BACnetFaultParameter extends Choice
{
    @SerializationTag(number = 0)
    public Object onlyNull = null; // Just to capture a null value. 

    //--//

    @SerializationTag(number = 1)
    public typefor_fault_characterstring fault_characterstring;

    public static final class typefor_fault_characterstring extends Sequence
    {
        @SerializationTag(number = 0)
        public List<String> list_of_fault_values;
    }

    //--//

    @SerializationTag(number = 2)
    public typefor_fault_extended fault_extended;

    public static final class typefor_fault_extended extends Sequence
    {
        public static final class Details extends AnyValue
        {
            @SerializationTag(number = 0)
            public BACnetDeviceObjectPropertyReference reference;
        }

        @SerializationTag(number = 0)
        public Unsigned16 vendor_id;

        @SerializationTag(number = 1)
        public Unsigned32 extended_fault_type;

        @SerializationTag(number = 2)
        public List<Details> parameters;
    }

    //--//

    @SerializationTag(number = 3)
    public typefor_fault_life_safety fault_life_safety;

    public static final class typefor_fault_life_safety extends Sequence
    {
        @SerializationTag(number = 0)
        public List<BACnetLifeSafetyState> list_of_fault_values;

        @SerializationTag(number = 1)
        public BACnetDeviceObjectPropertyReference mode_property_reference;
    }

    //--//

    @SerializationTag(number = 4)
    public typefor_fault_state fault_state;

    public static final class typefor_fault_state extends Sequence
    {
        @SerializationTag(number = 0)
        public List<BACnetPropertyStates> list_of_fault_values;
    }

    //--//

    @SerializationTag(number = 5)
    public typefor_fault_status_flags fault_status_flags;

    public static final class typefor_fault_status_flags extends Sequence
    {
        @SerializationTag(number = 0)
        BACnetDeviceObjectPropertyReference status_flags_reference;
    }

    //--//

    @SerializationTag(number = 6)
    public typefor_fault_out_of_range fault_out_of_range;

    public static final class typefor_fault_out_of_range extends Sequence
    {
        @SerializationTag(number = 0)
        public Optional<AnyValue> min_normal_value;

        @SerializationTag(number = 1)
        public Optional<AnyValue> max_normal_value;
    }

    @SerializationTag(number = 7)
    public typefor_fault_listed fault_listed;

    public static final class typefor_fault_listed extends Sequence
    {
        @SerializationTag(number = 0)
        public BACnetDeviceObjectPropertyReference fault_list_reference;
    }
}
