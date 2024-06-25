/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.victron;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownPointClass;

@JsonTypeName("Ipn:RS232:Victron_RealTimeData")
public class Victron_RealTimeData extends BaseVictronModel
{
    @FieldModelDescription(description = "Panel voltage", units = EngineeringUnits.millivolts, pointClass = WellKnownPointClass.ArrayVoltage, debounceSeconds = 5, minimumDelta = 100)
    public float panel_voltage;

    @FieldModelDescription(description = "Panel power", units = EngineeringUnits.watts, pointClass = WellKnownPointClass.ArrayPower, debounceSeconds = 5, minimumDelta = 10)
    public float panel_power;

    //--//

    @FieldModelDescription(description = "Battery voltage", units = EngineeringUnits.millivolts, pointClass = WellKnownPointClass.BatteryVoltage, debounceSeconds = 5, minimumDelta = 100)
    public float battery_voltage;

    @FieldModelDescription(description = "Battery current", units = EngineeringUnits.milliamperes, pointClass = WellKnownPointClass.BatteryCurrent, debounceSeconds = 5, minimumDelta = 100)
    public float battery_current;

    //--//

    @FieldModelDescription(description = "Load current", units = EngineeringUnits.milliamperes, pointClass = WellKnownPointClass.LoadCurrent, debounceSeconds = 5, minimumDelta = 50)
    public float load_current;

    @FieldModelDescription(description = "Load power", units = EngineeringUnits.watts, pointClass = WellKnownPointClass.LoadPower, debounceSeconds = 5, minimumDelta = 0.100)
    public float load_power;

    //--//

    @FieldModelDescription(description = "Battery temperature", units = EngineeringUnits.degrees_celsius, pointClass = WellKnownPointClass.BatteryTemperature, debounceSeconds = 5,
                           minimumDelta = 0.050)
    public float battery_temperature;

    @FieldModelDescription(description = "Charging mode", units = EngineeringUnits.enumerated, pointClass = WellKnownPointClass.ChargingStatus)
    public VictronChargingMode charging_mode;

    @FieldModelDescription(description = "Error Code", units = EngineeringUnits.enumerated, pointClass = WellKnownPointClass.FaultCode)
    public VictronErrorCode error_code;

    //--//

    @FieldModelDescription(description = "Firmware Version", units = EngineeringUnits.constant, debounceSeconds = 5)
    public String firmware_version;

    @FieldModelDescription(description = "Product ID", units = EngineeringUnits.constant, debounceSeconds = 5)
    public String product_id;

    @FieldModelDescription(description = "Serial #", units = EngineeringUnits.constant, debounceSeconds = 5)
    public String serial_number;

    //--//

    @Override
    public String extractBaseId()
    {
        return "Victron_RealTimeData";
    }
}
