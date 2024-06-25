/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed.choice;

import com.optio3.lang.Unsigned32;
import com.optio3.protocol.model.bacnet.Choice;
import com.optio3.protocol.model.bacnet.enums.BACnetBackupState;
import com.optio3.protocol.model.bacnet.enums.BACnetBinaryLightingPV;
import com.optio3.protocol.model.bacnet.enums.BACnetBinaryPV;
import com.optio3.protocol.model.bacnet.enums.BACnetDeviceStatus;
import com.optio3.protocol.model.bacnet.enums.BACnetEngineeringUnits;
import com.optio3.protocol.model.bacnet.enums.BACnetEventState;
import com.optio3.protocol.model.bacnet.enums.BACnetEventType;
import com.optio3.protocol.model.bacnet.enums.BACnetFileAccessMethod;
import com.optio3.protocol.model.bacnet.enums.BACnetLightingInProgress;
import com.optio3.protocol.model.bacnet.enums.BACnetLightingOperation;
import com.optio3.protocol.model.bacnet.enums.BACnetLightingTransition;
import com.optio3.protocol.model.bacnet.enums.BACnetNotifyType;
import com.optio3.protocol.model.bacnet.enums.BACnetPolarity;
import com.optio3.protocol.model.bacnet.enums.BACnetProgramError;
import com.optio3.protocol.model.bacnet.enums.BACnetProgramRequest;
import com.optio3.protocol.model.bacnet.enums.BACnetProgramState;
import com.optio3.protocol.model.bacnet.enums.BACnetProtocolLevel;
import com.optio3.protocol.model.bacnet.enums.BACnetReliability;
import com.optio3.protocol.model.bacnet.enums.BACnetRestartReason;
import com.optio3.protocol.model.bacnet.enums.BACnetTimerState;
import com.optio3.protocol.model.bacnet.enums.BACnetTimerTransition;
import com.optio3.protocol.model.bacnet.enums.BACnetWriteStatus;
import com.optio3.serialization.SerializationTag;

public final class BACnetPropertyStates extends Choice
{
    //__ This production represents the possible datatypes for properties that
    //__ have discrete or enumerated values. The choice shall be consistent with the
    //__ datatype of the property referenced in the Event Enrollment Object.

    @SerializationTag(number = 0)
    public boolean boolean_value;

    @SerializationTag(number = 1)
    public BACnetBinaryPV binary_value;

    @SerializationTag(number = 2)
    public BACnetEventType event_type;

    @SerializationTag(number = 3)
    public BACnetPolarity polarity;

    @SerializationTag(number = 4)
    public BACnetProgramRequest program_change;

    @SerializationTag(number = 5)
    public BACnetProgramState program_state;

    @SerializationTag(number = 6)
    public BACnetProgramError reason_for_halt;

    @SerializationTag(number = 7)
    public BACnetReliability reliability;

    @SerializationTag(number = 8)
    public BACnetEventState state;

    @SerializationTag(number = 9)
    public BACnetDeviceStatus system_status;

    @SerializationTag(number = 10)
    public BACnetEngineeringUnits units;

    @SerializationTag(number = 11)
    public Unsigned32 unsigned_value;

    //    @Tag(number = 12)
    //    public BACnetLifeSafetyMode life_safety_mode;
    //    
    //    @Tag(number = 13)
    //    public BACnetLifeSafetyState life_safety_state;

    @SerializationTag(number = 14)
    public BACnetRestartReason restart_reason;

    //    @Tag(number = 15)
    //    public BACnetDoorAlarmState door_alarm_state;
    //    
    //    @Tag(number = 16)
    //    public BACnetAction action;
    //    
    //    @Tag(number = 17)
    //    public BACnetDoorSecuredStatus door_secured_status;
    //    
    //    @Tag(number = 18)
    //    public BACnetDoorStatus door_status;
    //    
    //    @Tag(number = 19)
    //    public BACnetDoorValue door_value;

    @SerializationTag(number = 20)
    public BACnetFileAccessMethod file_access_method;

    //    @Tag(number = 21)
    //    public BACnetLockStatus lock_status;
    //    
    //    @Tag(number = 22)
    //    public BACnetLifeSafetyOperation life_safety_operation;
    //    
    //    @Tag(number = 23)
    //    public BACnetMaintenance maintenance;
    //    
    //    @Tag(number = 24)
    //    public BACnetNodeType node_type;

    @SerializationTag(number = 25)
    public BACnetNotifyType notify_type;

    //    @Tag(number = 26)
    //    public BACnetSecurityLevel security_level;
    //    
    //    @Tag(number = 27)
    //    public BACnetShedState shed_state;
    //    
    //    @Tag(number = 28)
    //    public BACnetSilencedState silenced_state;
    //
    //    @Tag(number = 30)
    //    public BACnetAccessEvent access_event;
    //    
    //    @Tag(number = 31)
    //    public BACnetAccessZoneOccupancyState zone_occupancy_state;
    //    
    //    @Tag(number = 32)
    //    public BACnetAccessCredentialDisableReason access_credential_disable_reason;
    //    
    //    @Tag(number = 33)
    //    public BACnetAccessCredentialDisable access_credential_disable;
    //    
    //    @Tag(number = 34)
    //    public BACnetAuthenticationStatus authentication_status;

    @SerializationTag(number = 36)
    public BACnetBackupState backup_state;

    @SerializationTag(number = 37)
    public BACnetWriteStatus write_status;

    @SerializationTag(number = 38)
    public BACnetLightingInProgress lighting_in_progress;

    @SerializationTag(number = 39)
    public BACnetLightingOperation lighting_operation;

    @SerializationTag(number = 40)
    public BACnetLightingTransition lighting_transition;

    @SerializationTag(number = 41)
    public int integer_value;

    @SerializationTag(number = 42)
    public BACnetBinaryLightingPV binary_lighting_value;

    @SerializationTag(number = 43)
    public BACnetTimerState timer_state;

    @SerializationTag(number = 44)
    public BACnetTimerTransition timer_transition;

    //    @Tag(number = 45)
    //    public BACnetIPMode bacnet_ip_mode;
    //    
    //    @Tag(number = 46)
    //    public BACnetNetworkPortCommand network_port_command;
    //    
    //    @Tag(number = 47)
    //    public BACnetNetworkType network_type;
    //    
    //    @Tag(number = 48)
    //    public BACnetNetworkNumberQuality network_number_quality;
    //    
    //    @Tag(number = 49)
    //    public BACnetEscalatorOperationDirection escalator_operation_direction;
    //    
    //    @Tag(number = 50)
    //    public BACnetEscalatorFault escalator_fault;
    //    
    //    @Tag(number = 51)
    //    public BACnetEscalatorMode escalator_mode;
    //    
    //    @Tag(number = 52)
    //    public BACnetLiftCarDirection lift_car_direction;
    //    
    //    @Tag(number = 53)
    //    public BACnetLiftCarDoorCommand lift_car_door_command;
    //    
    //    @Tag(number = 54)
    //    public BACnetLiftCarDriveStatus lift_car_drive_status;
    //    
    //    @Tag(number = 55)
    //    public BACnetLiftCarMode lift_car_mode;
    //    
    //    @Tag(number = 56)
    //    public BACnetLiftGroupMode lift_group_mode;
    //    
    //    @Tag(number = 57)
    //    public BACnetLiftFault lift_fault;

    @SerializationTag(number = 58)
    public BACnetProtocolLevel protocol_level;

    @SerializationTag(number = 63)
    public Unsigned32 extended_value;
}
