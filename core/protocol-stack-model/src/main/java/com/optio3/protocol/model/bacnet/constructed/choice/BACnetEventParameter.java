/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed.choice;

import java.util.BitSet;
import java.util.List;

import com.optio3.lang.Unsigned16;
import com.optio3.lang.Unsigned32;
import com.optio3.protocol.model.bacnet.AnyValue;
import com.optio3.protocol.model.bacnet.Choice;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.constructed.BACnetDeviceObjectPropertyReference;
import com.optio3.protocol.model.bacnet.constructed.BACnetDeviceObjectPropertyValue;
import com.optio3.protocol.model.bacnet.constructed.BACnetDeviceObjectReference;
import com.optio3.protocol.model.bacnet.enums.BACnetAccessEvent;
import com.optio3.protocol.model.bacnet.enums.BACnetLifeSafetyState;
import com.optio3.protocol.model.bacnet.enums.BACnetTimerState;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetStatusFlags;
import com.optio3.serialization.SerializationTag;

public final class BACnetEventParameter extends Choice
{
    //        __ These choices have a one_to_one correspondence with the Event_Type enumeration with the exception of the
    //        __ complex_event_type, which is used for proprietary event types.

    @SerializationTag(number = 0)
    public typefor_change_of_bitstring change_of_bitstring;

    public static class typefor_change_of_bitstring extends Sequence
    {
        @SerializationTag(number = 0)
        public Unsigned32 time_delay;

        @SerializationTag(number = 1)
        public BitSet bitmask;

        @SerializationTag(number = 2)
        public List<BitSet> list_of_bitstring_values;
    }

    //--//

    @SerializationTag(number = 1)
    public typefor_change_of_state change_of_state;

    public static class typefor_change_of_state extends Sequence
    {
        @SerializationTag(number = 0)
        public Unsigned32 time_delay;

        @SerializationTag(number = 1)
        public List<BACnetPropertyStates> list_of_values;
    }

    //--//

    @SerializationTag(number = 2)
    public typefor_change_of_value change_of_value;

    public static class typefor_change_of_value extends Sequence
    {
        public static class Value extends Choice
        {
            @SerializationTag(number = 0)
            public BitSet bitmask;

            @SerializationTag(number = 1)
            public float referenced_property_increment;
        }

        @SerializationTag(number = 0)
        public Unsigned32 time_delay;

        @SerializationTag(number = 1)
        public Value cov_criteria;
    }

    //--//

    @SerializationTag(number = 3)
    public typefor_command_failure command_failure;

    public static class typefor_command_failure extends Sequence
    {
        @SerializationTag(number = 0)
        public Unsigned32 time_delay;

        @SerializationTag(number = 1)
        public BACnetDeviceObjectPropertyReference feedback_property_reference;
    }

    //--//

    @SerializationTag(number = 4)
    public typefor_floating_limit floating_limit;

    public static class typefor_floating_limit extends Sequence
    {
        @SerializationTag(number = 0)
        public Unsigned32 time_delay;

        @SerializationTag(number = 1)
        public BACnetDeviceObjectPropertyReference setpoint_reference;

        @SerializationTag(number = 2)
        public float low_diff_limit;

        @SerializationTag(number = 3)
        public float high_diff_limit;

        @SerializationTag(number = 4)
        public float deadband;
    }

    //--//

    @SerializationTag(number = 5)
    public typefor_out_of_range out_of_range;

    public static class typefor_out_of_range extends Sequence
    {
        @SerializationTag(number = 0)
        public Unsigned32 time_delay;

        @SerializationTag(number = 1)
        public float low_limit;

        @SerializationTag(number = 2)
        public float high_limit;

        @SerializationTag(number = 3)
        public float deadband;
    }

    //--//

    @SerializationTag(number = 8)
    public typefor_change_of_life_safety change_of_life_safety;

    public static class typefor_change_of_life_safety extends Sequence
    {
        @SerializationTag(number = 0)
        public Unsigned32 time_delay;

        @SerializationTag(number = 1)
        public List<BACnetLifeSafetyState> list_of_life_safety_alarm_values;

        @SerializationTag(number = 2)
        public List<BACnetLifeSafetyState> list_of_alarm_values;

        @SerializationTag(number = 3)
        public BACnetDeviceObjectPropertyReference mode_property_reference;
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
        public Unsigned32 notification_threshold;

        @SerializationTag(number = 1)
        public Unsigned32 previous_notification_count;
    }

    //--//

    @SerializationTag(number = 11)
    public typefor_unsigned_range unsigned_range;

    public static class typefor_unsigned_range extends Sequence
    {
        @SerializationTag(number = 0)
        public Unsigned32 time_delay;

        @SerializationTag(number = 1)
        public Unsigned32 low_limit;

        @SerializationTag(number = 2)
        public Unsigned32 high_limit;
    }

    //--//

    @SerializationTag(number = 13)
    public typefor_access_event access_event;

    public static class typefor_access_event extends Sequence
    {
        @SerializationTag(number = 0)
        public List<BACnetAccessEvent> list_of_access_events;

        @SerializationTag(number = 1)
        public BACnetDeviceObjectReference access_event_time_reference;
    }

    //--//

    @SerializationTag(number = 14)
    public typefor_double_out_of_range double_out_of_range;

    public static class typefor_double_out_of_range extends Sequence
    {
        @SerializationTag(number = 0)
        public Unsigned32 time_delay;

        @SerializationTag(number = 1)
        public double low_limit;

        @SerializationTag(number = 2)
        public double high_limit;

        @SerializationTag(number = 3)
        public double deadband;
    }

    //--//

    @SerializationTag(number = 15)
    public typefor_signed_out_of_range signed_out_of_range;

    public static class typefor_signed_out_of_range extends Sequence
    {
        @SerializationTag(number = 0)
        public Unsigned32 time_delay;

        @SerializationTag(number = 1)
        public int low_limit;

        @SerializationTag(number = 2)
        public int high_limit;

        @SerializationTag(number = 3)
        public Unsigned32 deadband;
    }

    //--//

    @SerializationTag(number = 16)
    public typefor_unsigned_out_of_range unsigned_out_of_range;

    public static class typefor_unsigned_out_of_range extends Sequence
    {
        @SerializationTag(number = 0)
        public Unsigned32 time_delay;

        @SerializationTag(number = 1)
        public Unsigned32 low_limit;

        @SerializationTag(number = 2)
        public Unsigned32 high_limit;

        @SerializationTag(number = 3)
        public Unsigned32 deadband;
    }

    //--//

    @SerializationTag(number = 17)
    public typefor_change_of_characterstring change_of_characterstring;

    public static class typefor_change_of_characterstring extends Sequence
    {
        @SerializationTag(number = 0)
        public Unsigned32 time_delay;

        @SerializationTag(number = 1)
        public List<String> list_of_alarm_values;
    }

    //--//

    @SerializationTag(number = 18)
    public typefor_change_of_status_flags change_of_status_flags;

    public static class typefor_change_of_status_flags extends Sequence
    {
        @SerializationTag(number = 0)
        public Unsigned32 time_delay;

        @SerializationTag(number = 1)
        public BACnetStatusFlags selected_flags;
    }

    //--//

    @SerializationTag(number = 21)
    public typefor_change_of_discrete_value change_of_discrete_value;

    public static class typefor_change_of_discrete_value extends Sequence
    {
        @SerializationTag(number = 0)
        public Unsigned32 time_delay;
    }

    //--//

    @SerializationTag(number = 22)
    public typefor_change_of_timer change_of_timer;

    public static class typefor_change_of_timer extends Sequence
    {
        @SerializationTag(number = 0)
        public Unsigned32 time_delay;

        @SerializationTag(number = 1)
        public List<BACnetTimerState> alarm_values;

        @SerializationTag(number = 2)
        public BACnetDeviceObjectPropertyReference update_time_reference;
    }
}
