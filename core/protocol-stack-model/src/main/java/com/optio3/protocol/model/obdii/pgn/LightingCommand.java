/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.obdii.PgnMessageType;
import com.optio3.protocol.model.obdii.pgn.enums.PgnActivationMode;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:LightingCommand")
@PgnMessageType(pgn = 65089, littleEndian = true, ignoreWhenReceived = false)
public class LightingCommand extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Running Light Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 0)
    public PgnActivationMode Running_Light_Command;

    @FieldModelDescription(description = "Alternate Beam Head Light Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 2)
    public PgnActivationMode Alternate_Beam_Head_Light_Command;

    @FieldModelDescription(description = "Low Beam Head Light Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 4)
    public PgnActivationMode Low_Beam_Head_Light_Command;

    @FieldModelDescription(description = "High Beam Head Light Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 6)
    public PgnActivationMode High_Beam_Head_Light_Command;

    @FieldModelDescription(description = "Tractor Front Fog Lights Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 0)
    public PgnActivationMode Tractor_Front_Fog_Lights_Command;

    @FieldModelDescription(description = "Rotating Beacon Light Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 2)
    public PgnActivationMode Rotating_Beacon_Light_Command;

    @FieldModelDescription(description = "Right Turn Signal Lights Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 4)
    public PgnActivationMode Right_Turn_Signal_Lights_Command;

    @FieldModelDescription(description = "Left Turn Signal Lights Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 6)
    public PgnActivationMode Left_Turn_Signal_Lights_Command;

    @FieldModelDescription(description = "Back Up Light and Alarm Horn Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 0)
    public PgnActivationMode Back_Up_Light_and_Alarm_Horn_Command;

    @FieldModelDescription(description = "Center Stop Light Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 2)
    public PgnActivationMode Center_Stop_Light_Command;

    @FieldModelDescription(description = "Right Stop Light Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 4)
    public PgnActivationMode Right_Stop_Light_Command;

    @FieldModelDescription(description = "Left Stop Light Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 6)
    public PgnActivationMode Left_Stop_Light_Command;

    @FieldModelDescription(description = "Implement Clearance Light Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 0)
    public PgnActivationMode Implement_Clearance_Light_Command;

    @FieldModelDescription(description = "Tractor Clearance Light Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 2)
    public PgnActivationMode Tractor_Clearance_Light_Command;

    @FieldModelDescription(description = "Implement Marker Light Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 4)
    public PgnActivationMode Implement_Marker_Light_Command;

    @FieldModelDescription(description = "Tractor Marker Light Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 6)
    public PgnActivationMode Tractor_Marker_Light_Command;

    @FieldModelDescription(description = "Rear Fog Light Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 0)
    public PgnActivationMode Rear_Fog_Light_Command;

    @FieldModelDescription(description = "Tractor Underside Mounted Work Lights Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 2)
    public PgnActivationMode Tractor_Underside_Mounted_Work_Lights_Command;

    @FieldModelDescription(description = "Tractor Rear Low Mounted Work Lights Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 4)
    public PgnActivationMode Tractor_Rear_Low_Mounted_Work_Lights_Command;

    @FieldModelDescription(description = "Tractor Rear High Mounted Work Lights Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 6)
    public PgnActivationMode Tractor_Rear_High_Mounted_Work_Lights_Command;

    @FieldModelDescription(description = "Tractor Side Low Mounted Work Lights Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 0)
    public PgnActivationMode Tractor_Side_Low_Mounted_Work_Lights_Command;

    @FieldModelDescription(description = "Tractor Side High Mounted Work Lights Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 2)
    public PgnActivationMode Tractor_Side_High_Mounted_Work_Lights_Command;

    @FieldModelDescription(description = "Tractor Front Low Mounted Work Lights Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 4)
    public PgnActivationMode Tractor_Front_Low_Mounted_Work_Lights_Command;

    @FieldModelDescription(description = "Tractor Front High Mounted Work Lights Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 6)
    public PgnActivationMode Tractor_Front_High_Mounted_Work_Lights_Command;

    @FieldModelDescription(description = "Implement OEM Option 2 Light Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 2, bitOffset = 0)
    public PgnActivationMode Implement_OEM_Option2_Light_Command;

    @FieldModelDescription(description = "Implement OEM Option 1 Light Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 2, bitOffset = 2)
    public PgnActivationMode Implement_OEM_Option1_Light_Command;

    @FieldModelDescription(description = "Implement Right Facing Work Light Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 2, bitOffset = 4)
    public PgnActivationMode Implement_Right_Facing_Work_Light_Command;

    @FieldModelDescription(description = "Implement Left Forward Work Light Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 2, bitOffset = 6)
    public PgnActivationMode Implement_Left_Forward_Work_Light_Command;

    @FieldModelDescription(description = "Lighting Data Request Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 2, bitOffset = 0)
    public PgnActivationMode Lighting_Data_Request_Command;

    @FieldModelDescription(description = "Implement Right Forward Work Light Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 2, bitOffset = 2)
    public PgnActivationMode Implement_Right_Forward_Work_Light_Command;

    @FieldModelDescription(description = "Implement Left Facing Work Light Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 2, bitOffset = 4)
    public PgnActivationMode Implement_Left_Facing_Work_Light_Command;

    @FieldModelDescription(description = "Implement Rear Work Light Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 2, bitOffset = 6)
    public PgnActivationMode Implement_Rear_Work_Light_Command;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "LightingCommand";
    }
}