/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.epsolar;

import java.util.BitSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.optio3.cloud.model.IEnumDescription;
import com.optio3.serialization.TypedBitSet;

public final class EpSolarDischargingFault extends TypedBitSet<EpSolarDischargingFault.Values>
{
    public enum Values implements TypedBitSet.ValueGetter,
                                  IEnumDescription
    {
        // @formatter:off
        Output_OverVoltage      ( 4 , "Output over voltage"               , null),
        Boost_OverVoltage       ( 5 , "Boost over voltage"                , null),
        HighVoltage_ShortCircuit( 6 , "Short circuit in high voltage side", null),
        Input_OverVoltage       ( 7 , "Input over voltage"                , null),
        OutputVoltageAbnormal   ( 8 , "Output voltage abnormal"           , null),
        CannotStopDischarging   ( 9 , "Unable to stop discharging"        , null),
        CannotDischarge         ( 10, "Unable to discharge"               , null),
        ShortCircuit            ( 11, "Short circuit"                     , null);
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

    public EpSolarDischargingFault()
    {
        super(Values.class, Values.values());
    }

    public EpSolarDischargingFault(BitSet bs)
    {
        super(Values.class, Values.values(), bs);
    }

    public EpSolarDischargingFault(int val)
    {
        super(Values.class, Values.values(), val);
    }

    @JsonCreator
    public EpSolarDischargingFault(List<String> inputs)
    {
        super(Values.class, Values.values(), inputs);
    }
}
