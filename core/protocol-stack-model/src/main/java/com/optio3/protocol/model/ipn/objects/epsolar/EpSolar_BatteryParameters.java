/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.epsolar;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;

@JsonTypeName("Ipn:Modbus:EpSolar_BatteryParameters")
public class EpSolar_BatteryParameters extends BaseEpSolarModel
{
    @FieldModelDescription(description = "Rated charging current", units = EngineeringUnits.amperes)
    @EpSolarField(pdu = 0x3005, length = 1, signed = true, fixedScaling = 1.0f / 100)
    public float rated_charging_current;

    @FieldModelDescription(description = "Rated load current", units = EngineeringUnits.amperes)
    @EpSolarField(pdu = 0x300E, length = 1, signed = true, fixedScaling = 1.0f / 100)
    public float rated_load_current;

    //--//

    @FieldModelDescription(description = "Battery type", units = EngineeringUnits.enumerated)
    @EpSolarField(pdu = 0x9000, length = 1, signed = false, writable = true)
    public EpSolarBatteryType battery_type;

    @FieldModelDescription(description = "Battery capacity", units = EngineeringUnits.ampere_hours)
    @EpSolarField(pdu = 0x9001, length = 1, signed = false, writable = true)
    public int battery_capacity;

    @FieldModelDescription(description = "Temperature compensation coefficient", units = EngineeringUnits.volts_per_degree_kelvin)
    @EpSolarField(pdu = 0x9002, length = 1, signed = false, writable = true, fixedScaling = 1000.0f / 100)
    public float temperature_coeff;

    @FieldModelDescription(description = "Over voltage disconnect voltage", units = EngineeringUnits.volts)
    @EpSolarField(pdu = 0x9003, length = 1, signed = false, writable = true, fixedScaling = 1.0f / 100)
    public float over_voltage_disconnect_voltage;

    @FieldModelDescription(description = "Charging limit voltage", units = EngineeringUnits.volts)
    @EpSolarField(pdu = 0x9004, length = 1, signed = false, writable = true, fixedScaling = 1.0f / 100)
    public float charging_limit_voltage;

    @FieldModelDescription(description = "Over voltage reconnect voltage", units = EngineeringUnits.volts)
    @EpSolarField(pdu = 0x9005, length = 1, signed = false, writable = true, fixedScaling = 1.0f / 100)
    public float over_voltage_reconnect_voltage;

    @FieldModelDescription(description = "Equalize charging voltage", units = EngineeringUnits.volts)
    @EpSolarField(pdu = 0x9006, length = 1, signed = false, writable = true, fixedScaling = 1.0f / 100)
    public float equalize_charging_voltage;

    @FieldModelDescription(description = "Boost charging voltage", units = EngineeringUnits.volts)
    @EpSolarField(pdu = 0x9007, length = 1, signed = false, writable = true, fixedScaling = 1.0f / 100)
    public float boost_charging_voltage;

    @FieldModelDescription(description = "Float charging voltage", units = EngineeringUnits.volts)
    @EpSolarField(pdu = 0x9008, length = 1, signed = false, writable = true, fixedScaling = 1.0f / 100)
    public float float_charging_voltage;

    @FieldModelDescription(description = "Boost reconnect charging voltage", units = EngineeringUnits.volts)
    @EpSolarField(pdu = 0x9009, length = 1, signed = false, writable = true, fixedScaling = 1.0f / 100)
    public float boost_reconnect_charging_voltage;

    @FieldModelDescription(description = "Low voltage reconnect voltage", units = EngineeringUnits.volts)
    @EpSolarField(pdu = 0x900A, length = 1, signed = false, writable = true, fixedScaling = 1.0f / 100)
    public float low_voltage_reconnect_voltage;

    @FieldModelDescription(description = "Under voltage warning recover voltage", units = EngineeringUnits.volts)
    @EpSolarField(pdu = 0x900B, length = 1, signed = false, writable = true, fixedScaling = 1.0f / 100)
    public float under_voltage_warning_recover_voltage;

    @FieldModelDescription(description = "Under voltage warning voltage", units = EngineeringUnits.volts)
    @EpSolarField(pdu = 0x900C, length = 1, signed = false, writable = true, fixedScaling = 1.0f / 100)
    public float under_voltage_warning_voltage;

    @FieldModelDescription(description = "Low voltage disconnect voltage", units = EngineeringUnits.volts)
    @EpSolarField(pdu = 0x900D, length = 1, signed = false, writable = true, fixedScaling = 1.0f / 100)
    public float low_voltage_disconnect_voltage;

    @FieldModelDescription(description = "Discharging limit voltage", units = EngineeringUnits.volts)
    @EpSolarField(pdu = 0x900E, length = 1, signed = false, writable = true, fixedScaling = 1.0f / 100)
    public float discharging_limit_voltage;

    //--//

    @FieldModelDescription(description = "Battery rated voltage level", units = EngineeringUnits.enumerated)
    @EpSolarField(pdu = 0x9067, length = 1, signed = false, writable = true)
    public EpSolarBatteryRateVoltageLevel battery_rated_voltage_level;

    @FieldModelDescription(description = "Default load On/Off in manual mode", units = EngineeringUnits.enumerated)
    @EpSolarField(pdu = 0x906A, length = 1, signed = false, writable = true)
    public EpSolarOnOff default_load_onoff_in_manual_mode;

    @FieldModelDescription(description = "Equalize duration", units = EngineeringUnits.minutes)
    @EpSolarField(pdu = 0x906B, length = 1, signed = false, writable = true)
    public int equalize_duration;

    @FieldModelDescription(description = "Boost duration", units = EngineeringUnits.minutes)
    @EpSolarField(pdu = 0x906C, length = 1, signed = false, writable = true)
    public int boost_duration;

    @FieldModelDescription(description = "Battery discharge", units = EngineeringUnits.percent)
    @EpSolarField(pdu = 0x906D, length = 1, signed = false, writable = true)
    public int battery_discharge;

    @FieldModelDescription(description = "Battery charge", units = EngineeringUnits.percent)
    @EpSolarField(pdu = 0x906E, length = 1, signed = false, writable = true)
    public int battery_charge;

    @FieldModelDescription(description = "Charging mode", units = EngineeringUnits.enumerated)
    @EpSolarField(pdu = 0x906E, length = 1, signed = false, writable = true)
    public EpSolarChargingMode charging_mode;

    //--//

    @Override
    public String extractBaseId()
    {
        return "EpSolar_BatteryParameters";
    }
}
