/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.ipn.explorer;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.logging.Logger;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.util.BoxingUtils;
import com.optio3.util.Exceptions;
import com.optio3.util.Resources;
import org.apache.commons.lang3.StringUtils;

public class J1939Decoder
{
    public static class SpnPosition
    {
        private static final Pattern s_bits       = Pattern.compile("(\\d+) bit[s]?");
        private static final Pattern s_bytes      = Pattern.compile("(\\d+) byte[s]?");
        private static final Pattern s_byteOffset = Pattern.compile("(\\d+)");
        private static final Pattern s_byteRange  = Pattern.compile("(\\d+)-(\\d+)");
        private static final Pattern s_bitOffset  = Pattern.compile("(\\d+)\\.(\\d+)");

        public final int index;
        public final int bitOffset;
        public final int lengthInBits;

        private SpnPosition(String length,
                            String position)
        {
            Matcher matcher;

            matcher = s_bits.matcher(length);
            if (matcher.matches())
            {
                lengthInBits = Integer.parseInt(matcher.group(1));

                matcher = s_bitOffset.matcher(position);
                if (matcher.matches())
                {
                    index = Integer.parseInt(matcher.group(1));
                    bitOffset = Integer.parseInt(matcher.group(2)) - 1;
                    return;
                }

                if (lengthInBits == 8 || lengthInBits == 16 || lengthInBits == 32)
                {
                    matcher = s_byteOffset.matcher(position);
                    if (matcher.matches())
                    {
                        index = Integer.parseInt(matcher.group(1));
                        bitOffset = 0;
                        return;
                    }
                }

                if ("7.6-8.1".equals(position))
                {
                    index = 7;
                    bitOffset = 5;
                    return;
                }

                throw Exceptions.newIllegalArgumentException("Unknown bit position %s", position);
            }

            matcher = s_bytes.matcher(length);
            if (matcher.matches())
            {
                lengthInBits = 8 * Integer.parseInt(matcher.group(1));
                bitOffset = 0;

                matcher = s_byteRange.matcher(position);
                if (matcher.matches())
                {
                    index = Integer.parseInt(matcher.group(1));
                    return;
                }

                matcher = s_byteOffset.matcher(position);
                if (matcher.matches())
                {
                    index = Integer.parseInt(matcher.group(1));
                    return;
                }

                matcher = s_bitOffset.matcher(position);
                if (matcher.matches() && Integer.parseInt(matcher.group(2)) == 1)
                {
                    index = Integer.parseInt(matcher.group(1));
                    return;
                }

                throw Exceptions.newIllegalArgumentException("Unknown byte position %s", position);
            }

            throw Exceptions.newIllegalArgumentException("Unknown length encoding: %s", length);
        }
    }

    public static class SpnScaling
    {
        private static final Pattern s_enum            = Pattern.compile("(\\d+) states/(\\d+) bit");
        private static final Pattern s_decimalFraction = Pattern.compile("1/(\\d+)\\.(\\d+) *(.*)");
        private static final Pattern s_integerFraction = Pattern.compile("1/(\\d+) *(.*)");
        private static final Pattern s_decimal         = Pattern.compile("(-?\\d+)\\.(\\d+) *(.*)");
        private static final Pattern s_integer         = Pattern.compile("(-?\\d+) *(.*)");

        public final float            value;
        public final EngineeringUnits unit;

        private SpnScaling(String val)
        {
            Matcher matcher;

            matcher = s_enum.matcher(val);
            if (matcher.matches())
            {
                value = Integer.parseInt(matcher.group(1));
                unit = EngineeringUnits.enumerated;
                return;
            }

            matcher = s_decimalFraction.matcher(val);
            if (matcher.matches())
            {
                value = 1.0f / Float.parseFloat(matcher.group(1) + "." + matcher.group(2));
                unit = extractUnit(val, matcher.group(3));
                return;
            }

            matcher = s_integerFraction.matcher(val);
            if (matcher.matches())
            {
                value = 1.0f / Float.parseFloat(matcher.group(1));
                unit = extractUnit(val, matcher.group(2));
                return;
            }

            matcher = s_decimal.matcher(val);
            if (matcher.matches())
            {
                value = Float.parseFloat(matcher.group(1) + "." + matcher.group(2));
                unit = extractUnit(val, matcher.group(3));
                return;
            }

            matcher = s_integer.matcher(val);
            if (matcher.matches())
            {
                value = Float.parseFloat(matcher.group(1));
                unit = extractUnit(val, matcher.group(2));
                return;
            }

            switch (val)
            {
                case "ASCII":
                case "Binary":
                    value = 0;
                    unit = EngineeringUnits.no_units;
                    return;
            }

            throw Exceptions.newIllegalArgumentException("Unknown scaling encoding: %s", val);
        }

        private static EngineeringUnits extractUnit(String whole,
                                                    String unitPart)
        {
            if (StringUtils.isBlank(unitPart))
            {
                return EngineeringUnits.no_units;
            }

            if (unitPart.endsWith("/bit"))
            {
                unitPart = unitPart.substring(0, unitPart.length() - "/bit".length());
            }

            if (unitPart.endsWith(" per bit"))
            {
                unitPart = unitPart.substring(0, unitPart.length() - " per bit".length());
            }

            switch (unitPart)
            {
                case "%":
                    return EngineeringUnits.percent;

                case "ID":
                case "source address":
                    return EngineeringUnits.no_units;

                case "¡C":
                    return EngineeringUnits.degrees_celsius;

                case "kPa":
                    return EngineeringUnits.kilopascals;

                case "MPa":
                    return EngineeringUnits.megapascals;

                case "kW":
                    return EngineeringUnits.kilowatts;

                case "mW/cm_":
                    return EngineeringUnits.milliwatts_per_square_centimeter;

                case "A":
                    return EngineeringUnits.amperes;

                case "V":
                    return EngineeringUnits.volts;

                case "g":
                    return EngineeringUnits.us_gallons;

                case "g/min":
                    return EngineeringUnits.us_gallons_per_minute;

                case "g/h":
                    return EngineeringUnits.us_gallons_per_hour;

                case "g/kg":
                    return EngineeringUnits.grams_of_water_per_kilogram_dry_air;

                case "kg":
                    return EngineeringUnits.kilograms;

                case "kg/h":
                    return EngineeringUnits.kilograms_per_hour;

                case "km":
                    return EngineeringUnits.kilometers;

                case "km/h":
                    return EngineeringUnits.kilometers_per_hour;

                case "km/L":
                    return EngineeringUnits.kilometers_per_liter;

                case "mm":
                    return EngineeringUnits.millimeters;

                case "m":
                    return EngineeringUnits.meters;

                case "m/s":
                    return EngineeringUnits.meters_per_second;

                case "m/s_":
                    return EngineeringUnits.meters_per_second_per_second;

                case "s":
                    return EngineeringUnits.seconds;

                case "min":
                    return EngineeringUnits.minutes;

                case "h":
                    return EngineeringUnits.hours;

                case "days":
                    return EngineeringUnits.days;

                case "month":
                    return EngineeringUnits.months;

                case "year":
                case "years":
                    return EngineeringUnits.years;

                case "l":
                    return EngineeringUnits.liters;

                case "l/h":
                    return EngineeringUnits.liters_per_hour;

                case "rpm":
                    return EngineeringUnits.revolutions_per_minute;

                case "deg":
                    return EngineeringUnits.degrees_angular;

                case "rad":
                    return EngineeringUnits.radians;

                case "rad/s":
                    return EngineeringUnits.radians_per_second;

                case "ppm":
                    return EngineeringUnits.parts_per_million;

                case "µSiemens/mm":
                    return EngineeringUnits.microsiemens_per_millimeter;

                case "Nm":
                    return EngineeringUnits.newton_meters;

                case "":
                case "gear value":
                case "count":
                case "turn":
                case "turns":
                case "r":
                    return EngineeringUnits.counts;

                case "1/km":
                    // Should be 1 over EngineeringUnits.kilometers...
                    return EngineeringUnits.counts;
            }

            throw Exceptions.newIllegalArgumentException("Unknown unit for %s: %s", whole, unitPart);
        }
    }

    public static class Spn
    {
        public final int    spn;
        public       String type;
        public       String name;
        public       String description;
        public       String length;
        public       String position;
        public       String resolution;
        public       String offset;
        public       String dataRange;
        public       String operationalRange;
        public       String units;
        public       String slotId;
        public       String slotName;

        public Spn(int spn)
        {
            this.spn = spn;
        }

        public SpnPosition decodePosition()
        {
            return new SpnPosition(length, position);
        }

        public SpnScaling decodeResolution()
        {
            return new SpnScaling(resolution);
        }

        public SpnScaling decodeValueOffset()
        {
            return new SpnScaling(offset);
        }
    }

    public static class Pgn
    {
        public final int    pgn;
        public       String label;
        public       String acronym;
        public       String description;

        public int     edp;
        public int     dp;
        public int     pf;
        public int     ps;
        public boolean multipacket;

        public String rate;
        public int    length;
        public int    priority;
        public String reference;

        public final List<Spn> values = Lists.newArrayList();

        private Pgn(int pgn)
        {
            this.pgn = pgn;
        }
    }

    //--//

    public static final Logger LoggerInstance = new Logger(J1939Decoder.class);

    private final Map<Integer, Pgn> m_pgns = Maps.newHashMap();

    public J1939Decoder()
    {
        try
        {
            List<String> lines = Resources.loadResourceAsLines(J1939Decoder.class, "J1939/PGNs.txt", false);

            parse(lines);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public Pgn get(int pgn)
    {
        return m_pgns.get(pgn);
    }

    //--//

    private void parse(List<String> lines)
    {
        Map<String, Integer> lookupHeader = Maps.newHashMap();
        int                  pos          = 0;

        for (String header : StringUtils.split(lines.get(0), '\t'))
        {
            lookupHeader.put(header, pos++);
        }

        Integer offset_PGN                = lookupHeader.get("PGN");
        Integer offset_PG_Label           = lookupHeader.get("Parameter Group Label");
        Integer offset_PG_Acronym         = lookupHeader.get("PG Acronym");
        Integer offset_PG_Description     = lookupHeader.get("PG Description");
        Integer offset_EDP                = lookupHeader.get("EDP");
        Integer offset_DP                 = lookupHeader.get("DP");
        Integer offset_PF                 = lookupHeader.get("PF");
        Integer offset_PS                 = lookupHeader.get("PS");
        Integer offset_Multipacket        = lookupHeader.get("Multipacket");
        Integer offset_Transmission_Rate  = lookupHeader.get("Transmission Rate");
        Integer offset_PG_Data_Length     = lookupHeader.get("PG Data Length");
        Integer offset_Default_Priority   = lookupHeader.get("Default Priority");
        Integer offset_PGN_Reference      = lookupHeader.get("PGN Reference");
        Integer offset_SPN_Position_in_PG = lookupHeader.get("SPN Position in PG");
        Integer offset_SPN                = lookupHeader.get("SPN");
        Integer offset_SPN_Name           = lookupHeader.get("SPN Name");
        Integer offset_SPN_Description    = lookupHeader.get("SPN Description");
        Integer offset_SPN_Length         = lookupHeader.get("SPN Length");
        Integer offset_Resolution         = lookupHeader.get("Resolution");
        Integer offset_Offset             = lookupHeader.get("Offset");
        Integer offset_Data_Range         = lookupHeader.get("Data Range");
        Integer offset_Operational_Range  = lookupHeader.get("Operational Range");
        Integer offset_Units              = lookupHeader.get("Units");
        Integer offset_SLOT_Identifier    = lookupHeader.get("SLOT Identifier");
        Integer offset_SLOT_Name          = lookupHeader.get("SLOT Name");
        Integer offset_SPN_Type           = lookupHeader.get("SPN Type");

        //--//

        StringBuilder sb        = new StringBuilder();
        boolean       quoteOpen = false;

        for (int i = 1; i < lines.size(); i++)
        {
            String  line         = lines.get(i);
            boolean lastWasQuote = false;

            // Deal with line-line text fields.
            for (char c : line.toCharArray())
            {
                if (c == '"')
                {
                    if (lastWasQuote)
                    {
                        sb.append(c);
                        lastWasQuote = false;
                    }
                    else
                    {
                        lastWasQuote = true;
                    }
                }
                else
                {
                    if (lastWasQuote)
                    {
                        quoteOpen = !quoteOpen;
                        lastWasQuote = false;
                    }

                    if (c == '\t' && quoteOpen)
                    {
                        c = ' ';
                    }

                    sb.append(c);
                }
            }

            if (lastWasQuote)
            {
                quoteOpen = !quoteOpen;
            }

            if (quoteOpen)
            {
                sb.append("\n");
                continue;
            }

            String[] values = StringUtils.splitPreserveAllTokens(sb.toString(), "\t", 0);
            sb.setLength(0);

            Integer pgnId = getIntHeader(offset_PGN, values);
            if (pgnId != null)
            {
                Pgn pgn = m_pgns.get(pgnId);
                if (pgn == null)
                {
                    pgn = new Pgn(pgnId);
                    pgn.acronym = getHeader(offset_PG_Acronym, values);
                    pgn.description = getHeader(offset_PG_Description, values);
                    pgn.label = getHeader(offset_PG_Label, values);

                    pgn.edp = getIntHeader(offset_EDP, values, 0);
                    pgn.dp = getIntHeader(offset_DP, values, 0);

                    pgn.pf = getIntHeader(offset_PF, values, 0);
                    pgn.ps = getIntHeader(offset_PS, values, -1);
                    pgn.multipacket = StringUtils.compareIgnoreCase(getHeader(offset_Multipacket, values), "yes") == 0;
                    pgn.rate = getHeader(offset_Transmission_Rate, values);
                    pgn.length = getIntHeader(offset_PG_Data_Length, values, 0);
                    pgn.priority = getIntHeader(offset_Default_Priority, values, 0);
                    pgn.reference = getHeader(offset_PGN_Reference, values);

                    m_pgns.put(pgnId, pgn);
                }

                int spnId = getIntHeader(offset_SPN, values, -1);
                if (spnId >= 0)
                {
                    Spn spn = new Spn(spnId);
                    spn.position = getHeader(offset_SPN_Position_in_PG, values);

                    spn.name = getHeader(offset_SPN_Name, values);
                    spn.description = getHeader(offset_SPN_Description, values);
                    spn.length = getHeader(offset_SPN_Length, values);
                    spn.resolution = getHeader(offset_Resolution, values);
                    spn.offset = getHeader(offset_Offset, values);
                    spn.dataRange = getHeader(offset_Data_Range, values);
                    spn.operationalRange = getHeader(offset_Operational_Range, values);
                    spn.units = getHeader(offset_Units, values);
                    spn.slotId = getHeader(offset_SLOT_Identifier, values);
                    spn.slotName = getHeader(offset_SLOT_Name, values);
                    spn.type = getHeader(offset_SPN_Type, values);

                    pgn.values.add(spn);
                }
            }
        }
    }

    private static int getIntHeader(Integer offset,
                                    String[] values,
                                    int defaultValue)
    {
        return BoxingUtils.get(getIntHeader(offset, values), defaultValue);
    }

    private static Integer getIntHeader(Integer offset,
                                        String[] values)
    {
        try
        {
            String val = getHeader(offset, values);
            return StringUtils.isNotBlank(val) ? Integer.parseInt(val) : null;
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    private static String getHeader(Integer offset,
                                    String[] values)
    {
        return (offset != null && offset < values.length) ? values[offset] : null;
    }
}