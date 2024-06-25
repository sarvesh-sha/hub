/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed.choice;

import java.util.BitSet;
import java.util.List;
import java.util.Optional;

import com.optio3.lang.Unsigned16;
import com.optio3.lang.Unsigned32;
import com.optio3.protocol.model.bacnet.AnyValue;
import com.optio3.protocol.model.bacnet.Choice;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.constructed.BACnetDateTime;
import com.optio3.protocol.model.bacnet.constructed.BACnetDeviceObjectPropertyReference;
import com.optio3.protocol.model.bacnet.constructed.BACnetDeviceObjectPropertyValue;
import com.optio3.protocol.model.bacnet.constructed.BACnetPropertyValue;
import com.optio3.protocol.model.bacnet.enums.BACnetLifeSafetyMode;
import com.optio3.protocol.model.bacnet.enums.BACnetLifeSafetyOperation;
import com.optio3.protocol.model.bacnet.enums.BACnetLifeSafetyState;
import com.optio3.protocol.model.bacnet.enums.BACnetReliability;
import com.optio3.protocol.model.bacnet.enums.BACnetTimerState;
import com.optio3.protocol.model.bacnet.enums.BACnetTimerTransition;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetStatusFlags;
import com.optio3.serialization.SerializationTag;

public final class BACnetNotificationParameters extends Choice
{
    //        __ These choices have a one_to_one correspondence with the Event_Type enumeration with the exception of the
    //        __ complex_event_type, which is used for proprietary event types.

    @SerializationTag(number = 0)
    public typefor_change_of_bitstring change_of_bitstring;

    public static class typefor_change_of_bitstring extends Sequence
    {
        @SerializationTag(number = 0)
        public BitSet referenced_bitstring;

        @SerializationTag(number = 1)
        public BACnetStatusFlags status_flags;
    }

    //--//

    @SerializationTag(number = 1)
    public typefor_change_of_state change_of_state;

    public static class typefor_change_of_state extends Sequence
    {
        @SerializationTag(number = 0)
        public BACnetPropertyStates new_state;

        @SerializationTag(number = 1)
        public BACnetStatusFlags status_flags;
    }

    //--//

    @SerializationTag(number = 2)
    public typefor_change_of_value change_of_value;

    public static class typefor_change_of_value extends Sequence
    {
        public static class Value extends Choice
        {
            @SerializationTag(number = 0)
            public BitSet changed_bits;

            @SerializationTag(number = 1)
            public float changed_value;
        }

        @SerializationTag(number = 0)
        public Value new_value;

        @SerializationTag(number = 1)
        public BACnetStatusFlags status_flags;
    }

    //--//

    @SerializationTag(number = 3)
    public typefor_command_failure command_failure;

    public static class typefor_command_failure extends Sequence
    {
        public static class Value extends Choice
        {
            @SerializationTag(number = 0)
            public BitSet command_value;

            @SerializationTag(number = 1)
            public float changed_value;
        }

        @SerializationTag(number = 0)
        public Object command_value; // depends on ref property

        @SerializationTag(number = 1)
        public BACnetStatusFlags status_flags;

        @SerializationTag(number = 2)
        public Object feedback_value; // depends on ref property
    }

    //--//

    @SerializationTag(number = 4)
    public typefor_floating_limit floating_limit;

    public static class typefor_floating_limit extends Sequence
    {
        @SerializationTag(number = 0)
        public float reference_value;

        @SerializationTag(number = 1)
        public BACnetStatusFlags status_flags;

        @SerializationTag(number = 3)
        public float setpoint_value;

        @SerializationTag(number = 4)
        public float error_limit;
    }

    //--//

    @SerializationTag(number = 5)
    public typefor_out_of_range out_of_range;

    public static class typefor_out_of_range extends Sequence
    {
        @SerializationTag(number = 0)
        public float exceeding_value;

        @SerializationTag(number = 1)
        public BACnetStatusFlags status_flags;

        @SerializationTag(number = 3)
        public float deadband;

        @SerializationTag(number = 4)
        public float exceeded_limit;
    }

    //--//

    @SerializationTag(number = 6)
    public List<BACnetPropertyValue> complex_event_type;

    //--//

    @SerializationTag(number = 8)
    public typefor_change_of_life_safety change_of_life_safety;

    public static class typefor_change_of_life_safety extends Sequence
    {
        @SerializationTag(number = 0)
        public BACnetLifeSafetyState new_state;

        @SerializationTag(number = 1)
        public BACnetLifeSafetyMode new_mode;

        @SerializationTag(number = 2)
        public BACnetStatusFlags status_flags;

        @SerializationTag(number = 3)
        public BACnetLifeSafetyOperation operation_expected;
    }

    //--//

    @SerializationTag(number = 9)
    public typefor_extended extended;

    public static class typefor_extended extends Sequence
    {
        public static class Parameters extends AnyValue
        {
            @SerializationTag(number = 0)
            public BACnetDeviceObjectPropertyValue property_value;
        }

        @SerializationTag(number = 0)
        public Unsigned16 vendor_id;

        @SerializationTag(number = 1)
        public Unsigned32 extended_event_type;

        @SerializationTag(number = 2)
        public List<Parameters> parameters;
    }

    //--//

    @SerializationTag(number = 10)
    public typefor_buffer_ready buffer_ready;

    public static class typefor_buffer_ready extends Sequence
    {
        @SerializationTag(number = 0)
        public BACnetDeviceObjectPropertyReference buffer_property;

        @SerializationTag(number = 1)
        public Unsigned32 previous_notification;

        @SerializationTag(number = 2)
        public Unsigned32 current_notification;
    }

    //--//

    @SerializationTag(number = 11)
    public typefor_unsigned_range unsigned_range;

    public static class typefor_unsigned_range extends Sequence
    {
        @SerializationTag(number = 0)
        public Unsigned32 exceeding_value;

        @SerializationTag(number = 1)
        public BACnetStatusFlags status_flags;

        @SerializationTag(number = 2)
        public Unsigned32 exceeded_limit;
    }

    //--//

    //    @Tag(number = 13)
    //    public typefor_access_event access_event;
    //
    //    public static class typefor_access_event extends Sequence
    //    {
    //        @Tag(number = 0)
    //        public BACnetAccessEvent access_event;
    //
    //        @Tag(number = 1)
    //        public BACnetStatusFlags status_flags;
    //        
    //        @Tag(number = 2)
    //        public Unsigned32 access_event_tag;
    //        
    //        @Tag(number = 3)
    //        public BACnetTimeStamp access_event_time;
    //        
    //        @Tag(number = 4)
    //        public BACnetDeviceObjectReference access_credential;
    //
    //        @Tag(number = 5)
    //        public Optional<BACnetAuthenticationFactor> authentication_factor;
    //    }

    //--//

    @SerializationTag(number = 14)
    public typefor_double_out_of_range double_out_of_range;

    public static class typefor_double_out_of_range extends Sequence
    {
        @SerializationTag(number = 0)
        public double exceeding_value;

        @SerializationTag(number = 1)
        public BACnetStatusFlags status_flags;

        @SerializationTag(number = 2)
        public double deadband;

        @SerializationTag(number = 3)
        public double exceeded_limit;
    }

    //--//

    @SerializationTag(number = 15)
    public typefor_signed_out_of_range signed_out_of_range;

    public static class typefor_signed_out_of_range extends Sequence
    {
        @SerializationTag(number = 0)
        public int exceeding_value;

        @SerializationTag(number = 1)
        public BACnetStatusFlags status_flags;

        @SerializationTag(number = 2)
        public int deadband;

        @SerializationTag(number = 3)
        public int exceeded_limit;
    }

    //--//

    @SerializationTag(number = 16)
    public typefor_unsigned_out_of_range unsigned_out_of_range;

    public static class typefor_unsigned_out_of_range extends Sequence
    {
        @SerializationTag(number = 0)
        public Unsigned32 exceeding_value;

        @SerializationTag(number = 1)
        public BACnetStatusFlags status_flags;

        @SerializationTag(number = 2)
        public Unsigned32 deadband;

        @SerializationTag(number = 3)
        public Unsigned32 exceeded_limit;
    }

    //--//

    @SerializationTag(number = 17)
    public typefor_change_of_characterstring change_of_characterstring;

    public static class typefor_change_of_characterstring extends Sequence
    {
        @SerializationTag(number = 0)
        public String changed_value;

        @SerializationTag(number = 1)
        public BACnetStatusFlags status_flags;

        @SerializationTag(number = 2)
        public String alarm_value;
    }

    //--//

    @SerializationTag(number = 18)
    public typefor_change_of_status_flags change_of_status_flags;

    public static class typefor_change_of_status_flags extends Sequence
    {
        @SerializationTag(number = 0)
        public Optional<Object> present_value; // depends on referenced property

        @SerializationTag(number = 1)
        public BACnetStatusFlags referenced_flags;
    }

    //--//

    @SerializationTag(number = 19)
    public typefor_change_of_reliability change_of_reliability;

    public static class typefor_change_of_reliability extends Sequence
    {
        @SerializationTag(number = 0)
        public BACnetReliability reliability;

        @SerializationTag(number = 1)
        public BACnetStatusFlags status_flags;

        @SerializationTag(number = 2)
        public List<BACnetPropertyValue> property_values;
    }

    //--//

    @SerializationTag(number = 21)
    public typefor_change_of_discrete_value change_of_discrete_value;

    public static class typefor_change_of_discrete_value extends Sequence
    {
        public static class NewValue extends AnyValue
        {
            @SerializationTag(number = 0)
            public BACnetDateTime datetime;
        }

        @SerializationTag(number = 0)
        public NewValue new_value;

        @SerializationTag(number = 1)
        public BACnetStatusFlags status_flags;
    }

    //--//

    @SerializationTag(number = 22)
    public typefor_change_of_timer change_of_timer;

    public static class typefor_change_of_timer extends Sequence
    {
        @SerializationTag(number = 0)
        public BACnetTimerState new_state;

        @SerializationTag(number = 1)
        public BACnetStatusFlags status_flags;

        @SerializationTag(number = 2)
        public BACnetDateTime update_time;

        @SerializationTag(number = 3)
        public Optional<BACnetTimerTransition> last_state_change;

        @SerializationTag(number = 4)
        public Optional<Unsigned32> initial_timeout;

        @SerializationTag(number = 5)
        public Optional<BACnetDateTime> expiration_time;
    }
}
