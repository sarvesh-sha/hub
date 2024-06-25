/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.protocol.bacnet;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import com.google.common.collect.Sets;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.test.common.Optio3Test;
import org.junit.Test;

public class EngineeringUnitsTest extends Optio3Test
{
    @Test
    public void validateScaling()
    {
        Set<EngineeringUnits> seen = Sets.newHashSet();

        for (EngineeringUnits unit : EngineeringUnits.values())
        {
            boolean skipLine = false;

            Set<EngineeringUnits> set = EngineeringUnitsFactors.getEquivalenceSet(unit);
            if (set.size() > 1)
            {
                System.out.printf("Equivalence: %s => %s%n", unit, set);

                for (EngineeringUnits equivUnit : set)
                {
                    if (seen.add(equivUnit))
                    {
                        EngineeringUnitsFactors conv = equivUnit.getConversionFactors();
                        System.out.printf("  Conversion : %s => (%g) * x + (%g) # %s / %s%n", equivUnit, conv.scaling.multiplier, conv.scaling.offset, conv.numeratorUnits, conv.denominatorUnits);
                    }
                }

                skipLine = true;
            }
            else
            {
                EngineeringUnitsFactors conv = unit.getConversionFactors();
                if (!conv.scaling.equals(EngineeringUnitsFactors.Scaling.Identity))
                {
                    System.out.printf("Conversion : %s => (%g) * x + (%g) # %s / %s%n", unit, conv.scaling.multiplier, conv.scaling.offset, conv.numeratorUnits, conv.denominatorUnits);
                    skipLine = true;
                }
            }

            if (skipLine)
            {
                System.out.println();
            }
        }
    }

    @Test
    public void testConversion()
    {
        assertEquals(32, EngineeringUnits.convert(0, EngineeringUnits.degrees_celsius, EngineeringUnits.degrees_fahrenheit), 0.00001);
        assertEquals(212, EngineeringUnits.convert(100, EngineeringUnits.degrees_celsius, EngineeringUnits.degrees_fahrenheit), 0.00001);

        assertEquals(0, EngineeringUnits.convert(32, EngineeringUnits.degrees_fahrenheit, EngineeringUnits.degrees_celsius), 0.00001);
        assertEquals(100, EngineeringUnits.convert(212, EngineeringUnits.degrees_fahrenheit, EngineeringUnits.degrees_celsius), 0.00001);

        assertEquals(3.78541178, EngineeringUnits.convert(1, EngineeringUnits.us_gallons, EngineeringUnits.liters), 0.00001);

        assertEquals(100, EngineeringUnits.convert(62.1, EngineeringUnits.miles_per_hour, EngineeringUnits.kilometers_per_hour), 0.1);

        // Incompatible units, don't convert.
        assertEquals(1, EngineeringUnits.convert(1, EngineeringUnits.liters, EngineeringUnits.amperes), 0.1);
    }

    @Test
    public void testUnitsOperations()
    {
        EngineeringUnitsFactors n = EngineeringUnits.kilometers.getConversionFactors();
        EngineeringUnitsFactors s = EngineeringUnits.hours.getConversionFactors();

        // kph
        EngineeringUnitsFactors r1 = n.divideBy(s, false);

        assertEquals(1, r1.numeratorUnits.size());
        assertEquals(1, r1.denominatorUnits.size());
        assertEquals(100_000f / 3600, r1.scaling.convertTo(100), 0.001);

        // Hours per kilometer
        EngineeringUnitsFactors r2 = s.divideBy(n, true);

        // Should be unity...
        EngineeringUnitsFactors r3 = r1.multiplyBy(r2, true);
        assertEquals(0, r3.numeratorUnits.size());
        assertEquals(0, r3.denominatorUnits.size());
        assertEquals(1, r3.scaling.convertTo(1), 0.001);

        // km / kph => hours
        EngineeringUnitsFactors r4 = n.divideBy(EngineeringUnits.kilometers_per_hour.getConversionFactors(), false);
        assertEquals(0, r4.denominatorUnits.size());
        assertEquals(1, r4.numeratorUnits.size());
        assertEquals(EngineeringUnits.seconds, r4.numeratorUnits.get(0));
        assertEquals(3600, r4.scaling.convertTo(1), 0.001);
    }
}
