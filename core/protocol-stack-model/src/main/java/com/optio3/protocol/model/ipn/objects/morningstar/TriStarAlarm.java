/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.morningstar;

import java.util.BitSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.optio3.cloud.model.IEnumDescription;
import com.optio3.serialization.TypedBitSet;

public final class TriStarAlarm extends TypedBitSet<TriStarAlarm.Values>
{
    public enum Values implements TypedBitSet.ValueGetter,
                                  IEnumDescription
    {
        // @formatter:off
        RTS_open                        ( 0, "RTS open"                           , null),
        RTS_shorted                     ( 1, "RTS shorted"                        , null),
        RTS_disconnected                ( 2, "RTS disconnected"                   , null),
        Heatsink_temp_sensor_open       ( 3, "Heatsink temperature sensor open"   , null),
        Heatsink_temp_sensor_shorted    ( 4, "Heatsink temperature sensor shorted", null),
        High_temperature_current_limit  ( 5, "High temperature current limit"     , null),
        Current_limit                   ( 6, "Current limit"                      , null),
        Current_offset                  ( 7, "Current offset"                     , null),
        Battery_sense_out_of_range      ( 8, "Battery sense out of range"         , null),
        Battery_sense_disconnected      ( 9, "Battery sense disconnected"         , null),
        Uncalibrated                    (10, "Uncalibrated"                       , null),
        RTS_miswire                     (11, "RTS miswire"                        , null),
        High_voltage_disconnect         (12, "High voltage disconnect"            , null),
        system_miswire                  (14, "System miswire"                     , null),
        MOSFET_open                     (15, "MOSFET open"                        , null),
        P12_voltage_off                 (16, "P12 voltage off"                    , null),
        High_input_voltage_current_limit(17, "High input voltage current limit"   , null),
        ADC_input_max                   (18, "ADC input max"                      , null),
        Controller_was_reset            (19, "Controller was reset"               , null),
        Alarm21                         (20, "Alarm #21"                          , null),
        Alarm22                         (21, "Alarm #22"                          , null),
        Alarm23                         (22, "Alarm #23"                          , null),
        Alarm24                         (23, "Alarm #24"                          , null);
        // @formatter:on

        private final int    m_offset;
        private final String m_displayName;
        private final String m_description;

        Values(int offset,
               String displayName,
               String description)
        {
            m_offset = (byte) offset;
            m_displayName = displayName;
            m_description = description;
        }

        @Override
        public int getEncodingValue()
        {
            return m_offset;
        }

        @Override
        public String getDisplayName()
        {
            return m_displayName;
        }

        @Override
        public String getDescription()
        {
            return m_description;
        }
    }

    public TriStarAlarm()
    {
        super(Values.class, Values.values());
    }

    public TriStarAlarm(BitSet bs)
    {
        super(Values.class, Values.values(), bs);
    }

    @JsonCreator
    public TriStarAlarm(List<String> inputs)
    {
        super(Values.class, Values.values(), inputs);
    }
}
