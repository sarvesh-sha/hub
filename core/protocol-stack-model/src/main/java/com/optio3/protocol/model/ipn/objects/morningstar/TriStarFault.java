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

public final class TriStarFault extends TypedBitSet<TriStarFault.Values>
{
    public enum Values implements TypedBitSet.ValueGetter,
                                  IEnumDescription
    {
        // @formatter:off
        Overcurrent          ( 0, "Overcurrent"            , null),
        FETs_shorted         ( 1, "FETs Shorted"           , null),
        SoftwareBug          ( 2, "Software Bug"           , null),
        BatteryHVD           ( 3, "Battery HVD"            , null),
        ArrayHVD             ( 4, "Array HVD"              , null),
        SettingsSwitchChanged( 5, "Settings Switch Changed", null),
        CustomSettings       ( 6, "Custom Settings Edit"   , null),
        RtsShorted           ( 7, "RTS Shorted"            , null),
        RtsDisconnected      ( 8, "RTS Disconnected"       , null),
        EepromRetryLimit     ( 9, "EEPROM retry limit"     , null),
        SlaveControlTimeout  (11, "Slave Control Timeout"  , null);
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

    public TriStarFault()
    {
        super(Values.class, Values.values());
    }

    public TriStarFault(BitSet bs)
    {
        super(Values.class, Values.values(), bs);
    }

    @JsonCreator
    public TriStarFault(List<String> inputs)
    {
        super(Values.class, Values.values(), inputs);
    }
}
