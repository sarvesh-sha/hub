/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digineous.model;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModel;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.util.TimeUtils;

public class DigineousBlackBoxPayload
{
    public int ID;
    public int deviceID;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    public LocalDateTime ReceivedDateTime;

    @FieldModelDescription(description = "Voltage in R phase", units = EngineeringUnits.volts, pointTags = "phase_r")
    public Float VRN;
    @FieldModelDescription(description = "Voltage in Y phase", units = EngineeringUnits.volts, pointTags = "phase_y")
    public Float VYN;
    @FieldModelDescription(description = "Voltage in B phase", units = EngineeringUnits.volts, pointTags = "phase_b")
    public Float VBN;

    @FieldModelDescription(description = "Current in R phase", units = EngineeringUnits.amperes, pointTags = "phase_r")
    public Float IR;
    @FieldModelDescription(description = "Current in Y phase", units = EngineeringUnits.amperes, pointTags = "phase_y")
    public Float IY;
    @FieldModelDescription(description = "Current in B phase", units = EngineeringUnits.amperes, pointTags = "phase_b")
    public Float IB;

    @FieldModelDescription(description = "Power in R phase", units = EngineeringUnits.watts, pointTags = "phase_r")
    public Float W_R;
    @FieldModelDescription(description = "Power in Y phase", units = EngineeringUnits.watts, pointTags = "phase_y")
    public Float W_Y;
    @FieldModelDescription(description = "Power in B phase", units = EngineeringUnits.watts, pointTags = "phase_b")
    public Float W_B;

    @FieldModelDescription(description = "VA in R phase", units = EngineeringUnits.volt_amperes, pointTags = "phase_r")
    public Float VA_R;
    @FieldModelDescription(description = "VA in Y phase", units = EngineeringUnits.volt_amperes, pointTags = "phase_y")
    public Float VA_Y;
    @FieldModelDescription(description = "VA in B phase", units = EngineeringUnits.volt_amperes, pointTags = "phase_b")
    public Float VA_B;

    @FieldModelDescription(description = "VA reactive in R phase", units = EngineeringUnits.volt_amperes_reactive, pointTags = "phase_r")
    public Float VAR_R;
    @FieldModelDescription(description = "VA reactive in Y phase", units = EngineeringUnits.volt_amperes_reactive, pointTags = "phase_y")
    public Float VAR_Y;
    @FieldModelDescription(description = "VA reactive in B phase", units = EngineeringUnits.volt_amperes_reactive, pointTags = "phase_b")
    public Float VAR_B;

    @FieldModelDescription(description = "Voltage between R and Y phases", units = EngineeringUnits.volts, pointTags = "phase_ry")
    public Float RY;
    @FieldModelDescription(description = "Voltage between Y and B phases", units = EngineeringUnits.volts, pointTags = "phase_yb")
    public Float YB;
    @FieldModelDescription(description = "Voltage between B and R phases", units = EngineeringUnits.volts, pointTags = "phase_br")
    public Float BR;

    @FieldModelDescription(description = "Frequency", units = EngineeringUnits.hertz)
    public Float F;

    @FieldModelDescription(description = "Power consumption", units = EngineeringUnits.kilowatt_hours)
    public Float KWH;
    @FieldModelDescription(description = "Volt-Ampere consumption", units = EngineeringUnits.kilovolt_ampere_hours)
    public Float KVAH;

    @FieldModelDescription(description = "Power factor for R phase", units = EngineeringUnits.power_factor, pointTags = "phase_r")
    public Float PFR;
    @FieldModelDescription(description = "Power factor for Y phase", units = EngineeringUnits.power_factor, pointTags = "phase_y")
    public Float PFY;
    @FieldModelDescription(description = "Power factor for B phase", units = EngineeringUnits.power_factor, pointTags = "phase_b")
    public Float PFB;

    @FieldModelDescription(units = EngineeringUnits.no_units)
    public Float PA1;
    @FieldModelDescription(units = EngineeringUnits.no_units)
    public Float PA2;
    @FieldModelDescription(units = EngineeringUnits.no_units)
    public Float PA3;

    @FieldModelDescription(description = "Average Voltage", units = EngineeringUnits.volts)
    public Float VAVG;
    @FieldModelDescription(units = EngineeringUnits.volts)
    public Float VSM;

    @FieldModelDescription(description = "Average Current", units = EngineeringUnits.amperes)
    public Float IAVG;
    @FieldModelDescription(units = EngineeringUnits.amperes)
    public Float ISM;

    @FieldModelDescription(description = "Average Power", units = EngineeringUnits.watts)
    public Float W_AV;
    @FieldModelDescription(units = EngineeringUnits.watts)
    public Float W_SM;

    @FieldModelDescription(description = "Average Volt-Ampere", units = EngineeringUnits.volt_amperes)
    public Float VAAVG;
    @FieldModelDescription(units = EngineeringUnits.volt_amperes)
    public Float VASM;

    public Float PFAV;
    public Float PFSM;
    public Float PAAV;
    public Float PASM;

    @FieldModelDescription(units = EngineeringUnits.no_units)
    public Float AI1;
    @FieldModelDescription(units = EngineeringUnits.no_units)
    public Float AI2;
    @FieldModelDescription(units = EngineeringUnits.no_units)
    public Float AI3;
    @FieldModelDescription(units = EngineeringUnits.no_units)
    public Float AI4;
    @FieldModelDescription(units = EngineeringUnits.no_units)
    public Float AI5;
    @FieldModelDescription(units = EngineeringUnits.no_units)
    public Float AI6;
    @FieldModelDescription(units = EngineeringUnits.no_units)
    public Float AI7;
    @FieldModelDescription(units = EngineeringUnits.no_units)
    public Float AI8;

    @FieldModelDescription(units = EngineeringUnits.activeInactive)
    public Boolean DI1;
    @FieldModelDescription(units = EngineeringUnits.activeInactive)
    public Boolean DI2;
    @FieldModelDescription(units = EngineeringUnits.activeInactive)
    public Boolean DI3;
    @FieldModelDescription(units = EngineeringUnits.activeInactive)
    public Boolean DI4;
    @FieldModelDescription(units = EngineeringUnits.activeInactive)
    public Boolean DI5;
    @FieldModelDescription(units = EngineeringUnits.activeInactive)
    public Boolean DI6;
    @FieldModelDescription(units = EngineeringUnits.activeInactive)
    public Boolean DI7;
    @FieldModelDescription(units = EngineeringUnits.activeInactive)
    public Boolean DI8;

    @FieldModelDescription(units = EngineeringUnits.activeInactive)
    public Integer DO1;
    @FieldModelDescription(units = EngineeringUnits.activeInactive)
    public Integer DO2;
    @FieldModelDescription(units = EngineeringUnits.activeInactive)
    public Integer DO3;
    @FieldModelDescription(units = EngineeringUnits.activeInactive)
    public Integer DO4;
    @FieldModelDescription(units = EngineeringUnits.activeInactive)
    public Integer DO5;
    @FieldModelDescription(units = EngineeringUnits.activeInactive)
    public Integer DO6;
    @FieldModelDescription(units = EngineeringUnits.activeInactive)
    public Integer DO7;
    @FieldModelDescription(units = EngineeringUnits.activeInactive)
    public Integer DO8;

    @FieldModelDescription(description = "Power Consumption", units = EngineeringUnits.kilowatts)
    public Float KW;
    @FieldModelDescription(description = "Volt-Ampere Consumption", units = EngineeringUnits.kilovolt_amperes)
    public Float KVA;
    @FieldModelDescription(description = "Volt-Ampere Reactive Consumption", units = EngineeringUnits.kilovolt_amperes_reactive)
    public Float KVAR;

    public String Comments;

    @FieldModelDescription(description = "Desired State", units = EngineeringUnits.enumerated)
    public String desiredState;

    @FieldModelDescription(description = "State Machine", units = EngineeringUnits.enumerated)
    public String stateMachine;

    @FieldModelDescription(description = "State Machine Value", units = EngineeringUnits.counts)
    public Float stateMachineValue;

    //--//

    public static FieldModel[] getDescriptors()
    {
        return BaseObjectModel.collectDescriptors(DigineousBlackBoxPayload.class);
    }

    public static ObjectMapper getFixupObjectMapper(ObjectMapper objectMapper)
    {
        objectMapper = objectMapper.copy();

        objectMapper.addHandler(new DeserializationProblemHandler()
        {
            @Override
            public Object handleUnexpectedToken(DeserializationContext ctxt,
                                                JavaType targetType,
                                                JsonToken t,
                                                JsonParser p,
                                                String failureMsg) throws
                                                                   IOException
            {
                if (targetType.hasRawClass(Boolean.class))
                {
                    if (t.isNumeric())
                    {
                        Number valueToConvert = p.getNumberValue();
                        return valueToConvert.intValue() != 0;
                    }
                }

                if (targetType.hasRawClass(LocalDateTime.class))
                {
                    if (t.isNumeric())
                    {
                        Number        val  = p.getNumberValue();
                        ZonedDateTime time = TimeUtils.fromSecondsToUtcTime(val.longValue());
                        return time != null ? time.toLocalDateTime() : null;
                    }
                }

                return super.handleUnexpectedToken(ctxt, targetType, t, p, failureMsg);
            }

            @Override
            public Object handleWeirdNumberValue(DeserializationContext ctxt,
                                                 Class<?> targetType,
                                                 Number valueToConvert,
                                                 String failureMsg) throws
                                                                    IOException
            {
                if (targetType == Boolean.class)
                {
                    return valueToConvert.intValue() != 0;
                }

                return super.handleWeirdNumberValue(ctxt, targetType, valueToConvert, failureMsg);
            }

            @Override
            public Object handleWeirdStringValue(DeserializationContext ctxt,
                                                 Class<?> targetType,
                                                 String valueToConvert,
                                                 String failureMsg) throws
                                                                    IOException
            {
                if ("NULL".equals(valueToConvert))
                {
                    return null;
                }

                if (targetType == Boolean.class)
                {
                    if ("1".equals(valueToConvert))
                    {
                        return Boolean.TRUE;
                    }

                    if ("0".equals(valueToConvert))
                    {
                        return Boolean.FALSE;
                    }
                }

                return super.handleWeirdStringValue(ctxt, targetType, valueToConvert, failureMsg);
            }
        });

        return objectMapper;
    }
}
