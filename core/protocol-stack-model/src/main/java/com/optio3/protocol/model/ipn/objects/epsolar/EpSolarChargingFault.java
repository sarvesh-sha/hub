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

public final class EpSolarChargingFault extends TypedBitSet<EpSolarChargingFault.Values>
{
    public enum Values implements TypedBitSet.ValueGetter,
                                  IEnumDescription
    {
        // @formatter:off
        PV_ShortCircuit         ( 4 , "PV input is short circuit"                      , null),
        Disequilibrium          ( 6 , "Disequilibrium in three circuits"               , null),
        LoadMosfet_ShortCircuit ( 7 , "Load MOSFET is short circuit"                   , null),
        Load_ShortCircuit       ( 8 , "The load is short circuit"                      , null),
        Load_OverCurrent        ( 9 , "The load is over current"                       , null),
        Input_OverCurrent       ( 10, "Input is over current"                          , null),
        AntiReverse_ShortCircuit( 11, "Anti-reverse MOSFET is short circuit"           , null),
        AntiReverse_OpenCircuit ( 12, "Charging or Anti-reverse MOSFET is open circuit", null),
        Charging_ShortCircuit   ( 13, "Charging MOSFET is short circuit"               , null);
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

    public EpSolarChargingFault()
    {
        super(Values.class, Values.values());
    }

    public EpSolarChargingFault(BitSet bs)
    {
        super(Values.class, Values.values(), bs);
    }

    public EpSolarChargingFault(int val)
    {
        super(Values.class, Values.values(), val);
    }

    @JsonCreator
    public EpSolarChargingFault(List<String> inputs)
    {
        super(Values.class, Values.values(), inputs);
    }
}
