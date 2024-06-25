/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.epsolar;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.lang.Unsigned16;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownPointClass;

@JsonTypeName("Ipn:Modbus:EpSolar_RealTimeData")
public class EpSolar_RealTimeData extends BaseEpSolarModel
{
    @FieldModelDescription(description = "PV array input voltage", units = EngineeringUnits.volts, pointClass = WellKnownPointClass.ArrayVoltage, debounceSeconds = 5, minimumDelta = 0.100)
    @EpSolarField(pdu = 0x3100, length = 1, signed = true, fixedScaling = 1.0f / 100)
    public float array_input_voltage;

    @FieldModelDescription(description = "PV array input current", units = EngineeringUnits.amperes, pointClass = WellKnownPointClass.ArrayCurrent, debounceSeconds = 5, minimumDelta = 0.050)
    @EpSolarField(pdu = 0x3101, length = 1, signed = true, fixedScaling = 1.0f / 100)
    public float array_input_current;

    @FieldModelDescription(description = "PV array input power", units = EngineeringUnits.watts, pointClass = WellKnownPointClass.ArrayPower, debounceSeconds = 10, minimumDelta = 0.500)
    @EpSolarField(pdu = 0x3102, length = 2, signed = true, fixedScaling = 1.0f / 100)
    public float array_input_power;

    //--//

    @FieldModelDescription(description = "Load voltage", units = EngineeringUnits.volts, pointClass = WellKnownPointClass.LoadVoltage, debounceSeconds = 5, minimumDelta = 0.100)
    @EpSolarField(pdu = 0x310C, length = 1, signed = true, fixedScaling = 1.0f / 100)
    public float load_voltage;

    @FieldModelDescription(description = "Load current", units = EngineeringUnits.amperes, pointClass = WellKnownPointClass.LoadCurrent, debounceSeconds = 5, minimumDelta = 0.050)
    @EpSolarField(pdu = 0x310D, length = 1, signed = true, fixedScaling = 1.0f / 100)
    public float load_current;

    @FieldModelDescription(description = "Load power", units = EngineeringUnits.watts, pointClass = WellKnownPointClass.LoadPower, debounceSeconds = 10, minimumDelta = 0.500)
    @EpSolarField(pdu = 0x310E, length = 2, signed = true, fixedScaling = 1.0f / 100)
    public float load_power;

    //--//

    @FieldModelDescription(description = "Battery temperature", units = EngineeringUnits.degrees_celsius, pointClass = WellKnownPointClass.BatteryTemperature, debounceSeconds = 20,
                           minimumDelta = 0.50)
    @EpSolarField(pdu = 0x3110, length = 1, signed = true, fixedScaling = 1.0f / 100)
    public float battery_temperature;

    @FieldModelDescription(description = "Device temperature", units = EngineeringUnits.degrees_celsius, pointClass = WellKnownPointClass.HeatsinkTemperature, debounceSeconds = 20,
                           minimumDelta = 0.50)
    @EpSolarField(pdu = 0x3111, length = 1, signed = true, fixedScaling = 1.0f / 100)
    public float device_temperature;

    @FieldModelDescription(description = "Battery state of charge", units = EngineeringUnits.percent, pointClass = WellKnownPointClass.BatteryStateOfCharge, debounceSeconds = 5, minimumDelta = 0.050)
    @EpSolarField(pdu = 0x311A, length = 1, signed = false)
    public float battery_soc;

    @FieldModelDescription(description = "Battery's real rated voltage", units = EngineeringUnits.percent, debounceSeconds = 5, minimumDelta = 0.050)
    @EpSolarField(pdu = 0x311D, length = 1, signed = false, fixedScaling = 1.0f / 100)
    public float battery_real_rated_voltage;

    //--//

    @EpSolarField(pdu = 0x3200, length = 1, signed = false)
    @JsonIgnore
    public Unsigned16 battery_status;

    @FieldModelDescription(description = "Battery inner resistance abnormal", units = EngineeringUnits.onOff, debounceSeconds = 5, minimumDelta = 0.050)
    public boolean battery_inner_resistance_abnormal;

    @FieldModelDescription(description = "Battery temperature status", units = EngineeringUnits.enumerated, debounceSeconds = 5, minimumDelta = 0.050)
    public EpSolarBatteryTemperatureStatus battery_temperature_status;

    @FieldModelDescription(description = "Battery voltage status", units = EngineeringUnits.enumerated, debounceSeconds = 5, minimumDelta = 0.050)
    public EpSolarBatteryVoltageStatus battery_voltage_status;

    //--//

    @EpSolarField(pdu = 0x3201, length = 1, signed = false)
    @JsonIgnore
    public Unsigned16 charging_equipment_status;

    @FieldModelDescription(description = "Charging Faults", units = EngineeringUnits.enumerated, pointClass = WellKnownPointClass.FaultCodeCharging, debounceSeconds = 5)
    public EpSolarChargingFault charging_faults;

    //--//

    @EpSolarField(pdu = 0x3202, length = 1, signed = false)
    @JsonIgnore
    public Unsigned16 discharging_equipment_status;

    @FieldModelDescription(description = "Discharging Faults", units = EngineeringUnits.enumerated, pointClass = WellKnownPointClass.FaultCodeDischarging, debounceSeconds = 5)
    public EpSolarDischargingFault discharging_faults;

    //--//

    @FieldModelDescription(description = "Maximum battery voltage today", units = EngineeringUnits.volts, debounceSeconds = 5, minimumDelta = 0.100)
    @EpSolarField(pdu = 0x3302, length = 1, signed = false, fixedScaling = 1.0f / 100)
    public float max_battery_voltage;

    @FieldModelDescription(description = "Minimum battery voltage today", units = EngineeringUnits.volts, debounceSeconds = 5, minimumDelta = 0.100)
    @EpSolarField(pdu = 0x3303, length = 1, signed = false, fixedScaling = 1.0f / 100)
    public float min_battery_voltage;

    @FieldModelDescription(description = "Consumed energy (today)", units = EngineeringUnits.kilowatt_hours, debounceSeconds = 5, minimumDelta = 0.100)
    @EpSolarField(pdu = 0x3304, length = 2, signed = false, fixedScaling = 1.0f / 100)
    public float consumed_energy_today;

    @FieldModelDescription(description = "Consumed energy (this month)", units = EngineeringUnits.kilowatt_hours, debounceSeconds = 5, minimumDelta = 0.100)
    @EpSolarField(pdu = 0x3306, length = 2, signed = false, fixedScaling = 1.0f / 100)
    public float consumed_energy_month;

    @FieldModelDescription(description = "Consumed energy (this year)", units = EngineeringUnits.kilowatt_hours, debounceSeconds = 5, minimumDelta = 0.100)
    @EpSolarField(pdu = 0x3308, length = 2, signed = false, fixedScaling = 1.0f / 100)
    public float consumed_energy_year;

    @FieldModelDescription(description = "Total consumed energy", units = EngineeringUnits.kilowatt_hours, pointClass = WellKnownPointClass.TotalDischarge, debounceSeconds = 5, minimumDelta = 0.100)
    @EpSolarField(pdu = 0x330A, length = 2, signed = false, fixedScaling = 1.0f / 100)
    public float total_consumed_energy;

    @FieldModelDescription(description = "Generated energy (today)", units = EngineeringUnits.kilowatt_hours, debounceSeconds = 5, minimumDelta = 0.100)
    @EpSolarField(pdu = 0x330C, length = 2, signed = false, fixedScaling = 1.0f / 100)
    public float generated_energy_today;

    @FieldModelDescription(description = "Generated energy (this month)", units = EngineeringUnits.kilowatt_hours, debounceSeconds = 5, minimumDelta = 0.100)
    @EpSolarField(pdu = 0x330E, length = 2, signed = false, fixedScaling = 1.0f / 100)
    public float generated_energy_month;

    @FieldModelDescription(description = "Generated energy (this year)", units = EngineeringUnits.kilowatt_hours, debounceSeconds = 5, minimumDelta = 0.100)
    @EpSolarField(pdu = 0x3310, length = 2, signed = false, fixedScaling = 1.0f / 100)
    public float generated_energy_year;

    @FieldModelDescription(description = "Total generated energy", units = EngineeringUnits.kilowatt_hours, pointClass = WellKnownPointClass.TotalCharge, debounceSeconds = 5, minimumDelta = 0.100)
    @EpSolarField(pdu = 0x3312, length = 2, signed = false, fixedScaling = 1.0f / 100)
    public float total_generated_energy;

    //--//

    @FieldModelDescription(description = "Battery voltage", units = EngineeringUnits.volts, pointClass = WellKnownPointClass.BatteryVoltage, debounceSeconds = 5, minimumDelta = 0.100)
    @EpSolarField(pdu = 0x331A, length = 1, signed = false, fixedScaling = 1.0f / 100)
    public float battery_voltage;

    @FieldModelDescription(description = "Battery current", units = EngineeringUnits.amperes, pointClass = WellKnownPointClass.BatteryCurrent, debounceSeconds = 5, minimumDelta = 0.100)
    @EpSolarField(pdu = 0x331B, length = 2, signed = false, fixedScaling = 1.0f / 100)
    public float battery_current;

    //--//

    @Override
    public String extractBaseId()
    {
        return "EpSolar_RealTimeData";
    }

    @Override
    public void postProcess()
    {
        if (battery_status != null)
        {
            final int val = battery_status.unboxUnsigned();

            // D15: 1-Wrong identification for rated voltage
            // D8: Battery inner resistance abnormal 1, normal 0
            // D7-D4: 00H Normal, 01H Over Temp.(Higher than the warning settings), 02H Low Temp.(Lower than the warning settings)
            // D3-D0: 00H Normal ,01H Over Voltage. , 02H Under Voltage, 03H Over discharge, 04H Fault
            battery_inner_resistance_abnormal = check(val, 8);
            battery_temperature_status = EpSolarBatteryTemperatureStatus.parse(extract(val, 4, 4));
            battery_voltage_status = EpSolarBatteryVoltageStatus.parse(extract(val, 0, 4));
        }

        if (charging_equipment_status != null)
        {
            final int val = charging_equipment_status.unboxUnsigned();

            // D15-D14: Input voltage status. 00H normal, 01H No input power connected, 02H Higher input voltage , 03H Input voltage error.
            // D13: Charging MOSFET is short circuit.
            // D12: Charging or Anti-reverse MOSFET is open circuit.
            // D11: Anti-reverse MOSFET is short circuit.
            // D10: Input is over current.
            // D9: The load is over current.
            // D8: The load is short circuit.
            // D7: Load MOSFET is short circuit.
            // D6：Disequilibrium in three circuits.
            // D4: PV input is short circuit.
            // D3-D2: Charging status. 00H No charging,01H Float,02H Boost, 03H Equalization.
            // D1: 0 Normal, 1 Fault.
            // D0: 1 Running, 0 Standby.

            charging_faults = new EpSolarChargingFault(val);
        }

        if (discharging_equipment_status != null)
        {
            final int val = discharging_equipment_status.unboxUnsigned();

            // D15-D14: 00H Input voltage normal, 01H Input voltage low, 02H Input voltage high, 03H no access.
            // D13-D12: Output power. 00H Light load, 01H Moderate, 02H rated, 03H overload
            // D11: Short circuit
            // D10: Unable to discharge
            // D9: Unable to stop discharging
            // D8: Output voltage abnormal
            // D7: Input over voltage
            // D6: Short circuit in high voltage side
            // D5: Boost over voltage
            // D4: Output over voltage
            // D1: 0 Normal, 1 Fault.
            // D0: 1 Running, 0 Standby.

            discharging_faults = new EpSolarDischargingFault(val);
        }
    }

    private static byte extract(int val,
                                int shift,
                                int width)
    {
        return (byte) ((val >> shift) & ((1 << width) - 1));
    }

    private static boolean check(int val,
                                 int shift)
    {
        return (val & (1 << shift)) != 0;
    }
}
