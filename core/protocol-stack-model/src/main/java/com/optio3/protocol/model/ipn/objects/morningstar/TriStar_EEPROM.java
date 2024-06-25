/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.morningstar;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;

@JsonTypeName("Ipn:Modbus:TriStar_EEPROM")
public class TriStar_EEPROM extends BaseTriStarModel
{
    @FieldModelDescription(description = "Absorption voltage @ 25ºC", units = EngineeringUnits.volts)
    @TriStarField(pdu = 0xE000, length = 1, signed = true, fixedScaling = 1.0f / 32768, voltageScaling = true)
    public float EV_absorp;

    @FieldModelDescription(description = "Float voltage @ 25ºC", units = EngineeringUnits.volts)
    @TriStarField(pdu = 0xE001, length = 1, signed = true, fixedScaling = 1.0f / 32768, voltageScaling = true)
    public float EV_float;

    @FieldModelDescription(description = "Absorption time", units = EngineeringUnits.seconds)
    @TriStarField(pdu = 0xE002, length = 1, signed = false)
    public float Et_absorp;

    @FieldModelDescription(description = "Absorption extension time", units = EngineeringUnits.seconds)
    @TriStarField(pdu = 0xE003, length = 1, signed = false)
    public float Et_absorp_ext;

    @FieldModelDescription(description = "Absorption Extension Threshold Voltage", units = EngineeringUnits.volts)
    @TriStarField(pdu = 0xE004, length = 1, signed = true, fixedScaling = 1.0f / 32768, voltageScaling = true)
    public float EV_absorp_ext;

    @FieldModelDescription(description = "Voltage that cancels float", units = EngineeringUnits.volts)
    @TriStarField(pdu = 0xE005, length = 1, signed = true, fixedScaling = 1.0f / 32768, voltageScaling = true)
    public float EV_float_cancel;

    @FieldModelDescription(description = "Exit float timer", units = EngineeringUnits.seconds)
    @TriStarField(pdu = 0xE006, length = 1, signed = false)
    public float Et_float_exit_cum;

    @FieldModelDescription(description = "Equalize voltage @ 25ºC", units = EngineeringUnits.volts)
    @TriStarField(pdu = 0xE007, length = 1, signed = true, fixedScaling = 1.0f / 32768, voltageScaling = true)
    public float EV_eq;

    @FieldModelDescription(description = "Days between eq cycles", units = EngineeringUnits.days)
    @TriStarField(pdu = 0xE008, length = 1, signed = false)
    public float Et_eqcalendar;

    @FieldModelDescription(description = "Equalize time limit above Vreg", units = EngineeringUnits.seconds)
    @TriStarField(pdu = 0xE009, length = 1, signed = false)
    public float Et_eq_above;

    @FieldModelDescription(description = "Equalize time limit at Veq", units = EngineeringUnits.seconds)
    @TriStarField(pdu = 0xE00A, length = 1, signed = false)
    public float Et_eq_reg;

    @FieldModelDescription(description = "Battery service timer", units = EngineeringUnits.days)
    @TriStarField(pdu = 0xE00B, length = 1, signed = false)
    public float Et_batt_service;

    @TriStarField(pdu = 0xE00C, length = 1, signed = false)
    @JsonIgnore
    public int reserved_0xE00C; // Padding for read requests, otherwise TriStar hangs.

    @FieldModelDescription(description = "Temperature compensation coefficient", units = EngineeringUnits.volts)
    @TriStarField(pdu = 0xE00D, length = 1, signed = true, fixedScaling = -1.0f / 65536, voltageScaling = true)
    public float EV_tempcomp;

    @FieldModelDescription(description = "Battery High Voltage Disconnect", units = EngineeringUnits.volts)
    @TriStarField(pdu = 0xE00E, length = 1, signed = true, fixedScaling = 1.0f / 32768, voltageScaling = true)
    public float EV_hvd;

    @FieldModelDescription(description = "Battery High Voltage Reconnect", units = EngineeringUnits.volts)
    @TriStarField(pdu = 0xE00F, length = 1, signed = true, fixedScaling = 1.0f / 32768, voltageScaling = true)
    public float EV_hvr;

    @FieldModelDescription(description = "Battery charge reference limit", units = EngineeringUnits.volts)
    @TriStarField(pdu = 0xE010, length = 1, signed = true, fixedScaling = 1.0f / 32768, voltageScaling = true)
    public float Evb_ref_lim;

    @FieldModelDescription(description = "Maximum temp comp limit", units = EngineeringUnits.degrees_celsius)
    @TriStarField(pdu = 0xE011, length = 1, signed = true)
    public float ETb_max;

    @FieldModelDescription(description = "Minimum temp comp limit", units = EngineeringUnits.degrees_celsius)
    @TriStarField(pdu = 0xE012, length = 1, signed = true)
    public float ETb_min;

    @TriStarField(pdu = 0xE013, length = 1, signed = false)
    @JsonIgnore
    public int reserved_0xE013; // Padding for read requests, otherwise TriStar hangs.

    @TriStarField(pdu = 0xE014, length = 1, signed = false)
    @JsonIgnore
    public int reserved_0xE014; // Padding for read requests, otherwise TriStar hangs.

    @FieldModelDescription(description = "LED threshold: green to green/yellow", units = EngineeringUnits.volts)
    @TriStarField(pdu = 0xE015, length = 1, signed = true, fixedScaling = 1.0f / 32768, voltageScaling = true)
    public float EV_soc_g_gy;

    @FieldModelDescription(description = "LED threshold: green/yellow to yellow", units = EngineeringUnits.volts)
    @TriStarField(pdu = 0xE016, length = 1, signed = true, fixedScaling = 1.0f / 32768, voltageScaling = true)
    public float EV_soc_gy_y;

    @FieldModelDescription(description = "LED threshold: yel to yel/red", units = EngineeringUnits.volts)
    @TriStarField(pdu = 0xE017, length = 1, signed = true, fixedScaling = 1.0f / 32768, voltageScaling = true)
    public float EV_soc_y_yr;

    @FieldModelDescription(description = "LED threshold: yellow/red to red", units = EngineeringUnits.volts)
    @TriStarField(pdu = 0xE018, length = 1, signed = true, fixedScaling = 1.0f / 32768, voltageScaling = true)
    public float EV_soc_yr_r;

    @FieldModelDescription(description = "MODBUS slave address", units = EngineeringUnits.no_units)
    @TriStarField(pdu = 0xE019, length = 1, signed = false)
    public int Emodbus_id;

    @FieldModelDescription(description = "MeterBus address", units = EngineeringUnits.no_units)
    @TriStarField(pdu = 0xE01A, length = 1, signed = false)
    public int Emeterbus_id;

    @FieldModelDescription(description = "Battery Current Limit", units = EngineeringUnits.amperes)
    @TriStarField(pdu = 0xE01D, length = 1, signed = true, fixedScaling = 1.0f / 32768, currentScaling = true)
    public int EIb_lim;

    @FieldModelDescription(description = "Battery Current Limit", units = EngineeringUnits.volts)
    @TriStarField(pdu = 0xE020, length = 1, signed = true, fixedScaling = 1.0f / 32768, voltageScaling = true)
    public int EVa_ref_fixed_init;

    @FieldModelDescription(description = "Array V fixed target V (% of Voc)", units = EngineeringUnits.volts)
    @TriStarField(pdu = 0xE021, length = 1, signed = true, fixedScaling = 100.0f / 65536)
    public int EVa_ref_fixed_pct_init;

    //--//

    @Override
    public String extractBaseId()
    {
        return "TriStar_EEPROM";
    }
}
