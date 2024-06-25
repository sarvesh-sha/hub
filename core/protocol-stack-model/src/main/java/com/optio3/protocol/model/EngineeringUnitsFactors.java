/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;

public class EngineeringUnitsFactors
{
    public static class Scaling
    {
        public static final Scaling Identity = new Scaling(1.0, 0.0);

        public final double multiplier;
        public final double offset;

        public Scaling()
        {
            this.multiplier = 1;
            this.offset     = 0;
        }

        @JsonCreator
        public Scaling(@JsonProperty("multiplier") double multiplier,
                       @JsonProperty("offset") double offset)
        {
            this.multiplier = multiplier;
            this.offset     = offset;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }

            Scaling that = Reflection.as(o, Scaling.class);
            if (that == null)
            {
                return false;
            }

            return Double.compare(that.multiplier, multiplier) == 0 && Double.compare(that.offset, offset) == 0;
        }

        @Override
        public int hashCode()
        {
            int result = 1;

            result = 31 * result + Double.hashCode(multiplier);
            result = 31 * result + Double.hashCode(offset);

            return result;
        }

        public double convertTo(double val)
        {
            return multiplier * val + offset;
        }

        public double convertFrom(double val)
        {
            return (val - offset) / multiplier;
        }

        public static Scaling build(double multiplier,
                                    double offset)
        {
            return (multiplier == 1.0 && offset == 0.0) ? Identity : new Scaling(multiplier, offset);
        }

        public Scaling multiplyBy(Scaling scaling)
        {
            if (this == Identity)
            {
                return scaling;
            }

            if (scaling == Identity)
            {
                return this;
            }

            double m1 = this.multiplier;
            double m2 = scaling.multiplier;

            double c1 = this.offset;
            double c2 = scaling.offset;

            return new Scaling(m1 * m2, m2 * c1 + c2);
        }

        public Scaling divideBy(Scaling scaling)
        {
            if (scaling == Scaling.Identity)
            {
                return this;
            }

            if (scaling.offset == 0.0)
            {
                return new Scaling(this.multiplier / scaling.multiplier, this.offset / scaling.multiplier);
            }

            if (equals(scaling))
            {
                // Dividing the same unit, essentially.
                return Identity;
            }

            throw new IllegalArgumentException("Can't divide by a scaling factor with bias");
        }
    }

    private static final Supplier<Map<EngineeringUnitsFactors, Set<EngineeringUnits>>> s_equivalentSets = Suppliers.memoize(EngineeringUnitsFactors::computeSets);

    private static Map<EngineeringUnitsFactors, Set<EngineeringUnits>> computeSets()
    {
        Map<EngineeringUnitsFactors, Set<EngineeringUnits>> map = Maps.newHashMap();

        for (EngineeringUnits unit : EngineeringUnits.values())
        {
            EngineeringUnitsFactors conv = unit.getConversionFactors();
            Set<EngineeringUnits>   set  = map.computeIfAbsent(conv, k -> Sets.newHashSet());

            set.add(unit);
        }

        for (EngineeringUnitsFactors key : map.keySet())
        {
            Set<EngineeringUnits> set = map.get(key);
            map.put(key, Collections.unmodifiableSet(set));
        }

        return Collections.unmodifiableMap(map);
    }

    public static final EngineeringUnitsFactors Dimensionless = new EngineeringUnitsFactors(Scaling.Identity, Collections.emptyList(), Collections.emptyList(), null, true);

    public final Scaling                scaling;
    public final List<EngineeringUnits> numeratorUnits;
    public final List<EngineeringUnits> denominatorUnits;

    private final boolean                 m_normalized;
    private       EngineeringUnits        m_primary;
    private       EngineeringUnits        m_closestUnit;
    private       EngineeringUnitsFactors m_simplified;
    private       EngineeringUnitsFactors m_simplifiedOnlyDuplicates;

    @JsonCreator
    public static EngineeringUnitsFactors fromValues(@JsonProperty("scaling") Scaling scaling,
                                                     @JsonProperty("numeratorUnits") List<EngineeringUnits> numeratorUnits,
                                                     @JsonProperty("denominatorUnits") List<EngineeringUnits> denominatorUnits,
                                                     @JsonProperty("primary") EngineeringUnits primary)
    {
        return new EngineeringUnitsFactors(scaling, numeratorUnits, denominatorUnits, primary, false);
    }

    EngineeringUnitsFactors(EngineeringUnits unit,
                            boolean dimensionLess)
    {
        this(EngineeringUnitsFactors.Scaling.Identity, dimensionLess ? Collections.emptyList() : Lists.newArrayList(unit), Collections.emptyList(), unit, true);

        m_closestUnit = unit;
    }

    EngineeringUnitsFactors(Scaling scaling,
                            List<EngineeringUnits> numeratorUnits,
                            List<EngineeringUnits> denominatorUnits,
                            EngineeringUnits primary,
                            boolean normalized)
    {
        this.scaling          = scaling != null ? scaling : Scaling.Identity;
        this.numeratorUnits   = sortAndMakeImmutable(numeratorUnits, normalized);
        this.denominatorUnits = sortAndMakeImmutable(denominatorUnits, normalized);
        this.m_primary        = primary;
        this.m_normalized     = normalized;
    }

    public static EngineeringUnitsFactors get(EngineeringUnits units)
    {
        return units != null ? units.getConversionFactors() : null;
    }

    private static List<EngineeringUnits> sortAndMakeImmutable(List<EngineeringUnits> list,
                                                               boolean normalized)
    {
        if (CollectionUtils.isEmpty(list))
        {
            return Collections.emptyList();
        }

        if (normalized)
        {
            list = Lists.newArrayList(list);
            list.sort(Comparator.comparingInt(Enum::ordinal));
        }

        list = removeDuplicateSingletons(list, EngineeringUnits.enumerated);
        list = removeDuplicateSingletons(list, EngineeringUnits.onOff);
        list = removeDuplicateSingletons(list, EngineeringUnits.activeInactive);

        return Collections.unmodifiableList(list);
    }

    private static List<EngineeringUnits> removeDuplicateSingletons(List<EngineeringUnits> units,
                                                                    EngineeringUnits singleton)
    {
        boolean seen = false;

        for (int pos = 0; pos < units.size(); pos++)
        {
            if (units.get(pos) == singleton)
            {
                if (seen)
                {
                    units = CollectionUtils.filter(units, (unit) -> unit != singleton);
                    units.add(singleton);
                    return units;
                }

                seen = true;
            }
        }

        return units;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        EngineeringUnitsFactors that = Reflection.as(o, EngineeringUnitsFactors.class);

        //
        // We use a hash table lookup to find equivalent units, don't change this to identity.
        //
        return isEquivalent(that);
    }

    @Override
    public int hashCode()
    {
        int result = 1;

        for (int i = 0; i < numeratorUnits.size(); i++)
        {
            result = 31 * result + numeratorUnits.get(i)
                                                 .hashCode();
        }

        for (int i = 0; i < denominatorUnits.size(); i++)
        {
            result = 31 * result + denominatorUnits.get(i)
                                                   .hashCode();
        }

        return result;
    }

    public EngineeringUnits getPrimary()
    {
        if (m_primary != null)
        {
            if (areIdentical(this, m_primary.getConversionFactors()))
            {
                return m_primary;
            }
        }

        if (m_closestUnit == null)
        {
            EngineeringUnitsFactors simplified        = simplify();
            Scaling                 simplifiedScaling = simplified.scaling;

            EngineeringUnits closest               = null;
            double           closestMultiplierDiff = 0;
            double           closestOffsetDiff     = 0;

            for (EngineeringUnits value : EngineeringUnits.values())
            {
                EngineeringUnitsFactors factors = value.getConversionFactors();

                if (simplified.isEquivalentRaw(factors))
                {
                    Scaling valueScaling = factors.scaling;

                    if (simplifiedScaling.equals(valueScaling))
                    {
                        // Exact match.
                        return value;
                    }

                    double thisMultiplierDiff = Math.abs(simplifiedScaling.multiplier - valueScaling.multiplier);
                    double thisOffsetDiff     = Math.abs(simplifiedScaling.offset - valueScaling.offset);

                    if (closest == null || thisMultiplierDiff < closestMultiplierDiff || (thisMultiplierDiff == closestMultiplierDiff && thisOffsetDiff < closestOffsetDiff))
                    {
                        closest               = value;
                        closestMultiplierDiff = thisMultiplierDiff;
                        closestOffsetDiff     = thisOffsetDiff;
                    }
                }
            }

            m_closestUnit = closest;
        }

        return m_closestUnit;
    }

    @JsonIgnore
    public boolean isDimensionless()
    {
        return numeratorUnits.isEmpty() && denominatorUnits.isEmpty();
    }

    public EngineeringUnitsFactors simplify()
    {
        return simplify(false);
    }

    private EngineeringUnitsFactors simplify(boolean onlyDuplicates)
    {
        if (m_normalized)
        {
            // Already minimized.
            return this;
        }

        // Return cached version, if available.
        EngineeringUnitsFactors simplified = onlyDuplicates ? m_simplifiedOnlyDuplicates : m_simplified;
        if (simplified != null)
        {
            return simplified;
        }

        //
        // First, we remove duplicate units.
        //
        Scaling                scaling          = this.scaling != null ? this.scaling : Scaling.Identity;
        List<EngineeringUnits> numeratorUnits   = Lists.newArrayList(this.numeratorUnits);
        List<EngineeringUnits> denominatorUnits = Lists.newArrayList(this.denominatorUnits);

        for (Iterator<EngineeringUnits> itNum = numeratorUnits.iterator(); itNum.hasNext(); )
        {
            EngineeringUnits        unitNum    = itNum.next();
            EngineeringUnitsFactors factorsNum = unitNum.getConversionFactors();

            for (Iterator<EngineeringUnits> itDen = denominatorUnits.iterator(); itDen.hasNext(); )
            {
                EngineeringUnits        unitDen    = itDen.next();
                EngineeringUnitsFactors factorsDen = unitDen.getConversionFactors();

                if (factorsNum.isEquivalentRaw(factorsDen))
                {
                    scaling = scaling.multiplyBy(factorsNum.scaling);
                    scaling = scaling.divideBy(factorsDen.scaling);

                    itNum.remove();
                    itDen.remove();
                    break;
                }
            }
        }

        if (onlyDuplicates)
        {
            m_simplifiedOnlyDuplicates = new EngineeringUnitsFactors(scaling, numeratorUnits, denominatorUnits, m_primary, false);
            return m_simplifiedOnlyDuplicates;
        }

        //
        // Then we compose the various parts through scaling, multiplication and division.
        //
        EngineeringUnitsFactors res = EngineeringUnitsFactors.Dimensionless.scaleBy(scaling);

        for (EngineeringUnits numeratorUnit : numeratorUnits)
        {
            res = res.multiplyBy(numeratorUnit.getConversionFactors(), true);
        }
        for (EngineeringUnits denominatorUnit : denominatorUnits)
        {
            res = res.divideBy(denominatorUnit.getConversionFactors(), true);
        }

        m_simplified = res;
        return res;
    }

    public EngineeringUnitsFactors scaleBy(Scaling scaling)
    {
        return new EngineeringUnitsFactors(this.scaling.multiplyBy(scaling), numeratorUnits, denominatorUnits, m_primary, false);
    }

    public EngineeringUnitsFactors scaleByInverse(Scaling scaling)
    {
        return new EngineeringUnitsFactors(this.scaling.divideBy(scaling), numeratorUnits, denominatorUnits, m_primary, false);
    }

    public EngineeringUnitsFactors multiplyBy(EngineeringUnitsFactors other,
                                              boolean normalize)
    {
        List<EngineeringUnits> numerators   = Lists.newArrayList();
        List<EngineeringUnits> denominators = Lists.newArrayList();

        numerators.addAll(numeratorUnits);
        numerators.addAll(other.numeratorUnits);

        denominators.addAll(denominatorUnits);
        denominators.addAll(other.denominatorUnits);

        Scaling scaling2 = scaling.multiplyBy(other.scaling);

        if (normalize)
        {
            EngineeringUnits[] numeratorsArray = new EngineeringUnits[numerators.size()];
            numerators.toArray(numeratorsArray);

            EngineeringUnits[] denominatorsArray = new EngineeringUnits[denominators.size()];
            denominators.toArray(denominatorsArray);

            EngineeringUnits.Ratio  ratio = new EngineeringUnits.Ratio(numeratorsArray, denominatorsArray, null, 1);
            EngineeringUnitsFactors res   = ratio.simplify(m_primary);

            return new EngineeringUnitsFactors(scaling2.multiplyBy(res.scaling), res.numeratorUnits, res.denominatorUnits, m_primary, normalize);
        }
        else
        {
            EngineeringUnitsFactors res = new EngineeringUnitsFactors(scaling2, numerators, denominators, m_primary, false);
            return res.compact();
        }
    }

    public EngineeringUnitsFactors divideBy(EngineeringUnitsFactors other,
                                            boolean normalize)
    {
        List<EngineeringUnits> numerators   = Lists.newArrayList();
        List<EngineeringUnits> denominators = Lists.newArrayList();

        numerators.addAll(numeratorUnits);
        numerators.addAll(other.denominatorUnits); // Use denominator for other!

        denominators.addAll(denominatorUnits);
        denominators.addAll(other.numeratorUnits); // Use numerator for other!

        Scaling scaling2 = scaling.divideBy(other.scaling);

        if (normalize)
        {
            EngineeringUnits[] numeratorsArray = new EngineeringUnits[numerators.size()];
            numerators.toArray(numeratorsArray);

            EngineeringUnits[] denominatorsArray = new EngineeringUnits[denominators.size()];
            denominators.toArray(denominatorsArray);

            EngineeringUnits.Ratio  ratio = new EngineeringUnits.Ratio(numeratorsArray, denominatorsArray, null, 1);
            EngineeringUnitsFactors res   = ratio.simplify(m_primary);

            return new EngineeringUnitsFactors(scaling2.multiplyBy(res.scaling), res.numeratorUnits, res.denominatorUnits, m_primary, normalize);
        }
        else
        {
            EngineeringUnitsFactors res = new EngineeringUnitsFactors(scaling2, numerators, denominators, m_primary, false);
            return res.compact();
        }
    }

    //--//

    public static Set<EngineeringUnits> getEquivalenceSet(EngineeringUnits unit)
    {
        return getEquivalenceSet(unit.getConversionFactors());
    }

    public static Set<EngineeringUnits> getEquivalenceSet(EngineeringUnitsFactors unit)
    {
        if (unit == null)
        {
            return Collections.emptySet();
        }

        Map<EngineeringUnitsFactors, Set<EngineeringUnits>> map = s_equivalentSets.get();
        return map.get(unit.simplify());
    }

    //--//

    @JsonIgnore
    public boolean isEquivalenceRoot()
    {
        return scaling.equals(Scaling.Identity) && denominatorUnits.isEmpty() && numeratorUnits.size() <= 1; // Zero is allowed for dimensionless units.
    }

    public static boolean areIdentical(EngineeringUnitsFactors a,
                                       EngineeringUnitsFactors b)
    {
        if (a == b)
        {
            return true;
        }

        if (a == null || b == null)
        {
            return false;
        }

        EngineeringUnitsFactors left  = a.simplify();
        EngineeringUnitsFactors right = b.simplify();

        return Objects.equals(left.scaling, right.scaling) && left.isEquivalentRaw(right);
    }

    public boolean isEquivalent(EngineeringUnitsFactors other)
    {
        if (other == null)
        {
            return false;
        }

        if (this == other)
        {
            return true;
        }

        EngineeringUnitsFactors left  = this.simplify();
        EngineeringUnitsFactors right = other.simplify();

        return left.isEquivalentRaw(right);
    }

    public boolean isEquivalentRaw(EngineeringUnitsFactors other)
    {
        if (other == null)
        {
            return false;
        }

        if (this == other)
        {
            return true;
        }

        if (this.numeratorUnits.size() != other.numeratorUnits.size() || this.denominatorUnits.size() != other.denominatorUnits.size())
        {
            return false;
        }

        for (int i = 0; i < this.numeratorUnits.size(); i++)
        {
            var numThis  = this.numeratorUnits.get(i);
            var numOther = other.numeratorUnits.get(i);

            if (!numThis.equals(numOther))
            {
                return false;
            }
        }

        for (int i = 0; i < this.denominatorUnits.size(); i++)
        {
            var denThis  = this.denominatorUnits.get(i);
            var denOther = other.denominatorUnits.get(i);

            if (!denThis.equals(denOther))
            {
                return false;
            }
        }

        return true;
    }

    public int isIncludedInOther(EngineeringUnitsFactors other)
    {
        // Dimensionless units should be excluded.
        if (this.isDimensionless() || other.isDimensionless())
        {
            return 0;
        }

        if (this == other)
        {
            return 1;
        }

        if (isIncludedInOther(numeratorUnits, other.numeratorUnits) && isIncludedInOther(denominatorUnits, other.denominatorUnits))
        {
            return 1;
        }

        if (isIncludedInOther(numeratorUnits, other.denominatorUnits) && isIncludedInOther(denominatorUnits, other.numeratorUnits))
        {
            return -1;
        }

        return 0;
    }

    public static boolean isIncludedInOther(List<EngineeringUnits> source,
                                            List<EngineeringUnits> target)
    {
        int posSource = 0;
        int posTarget = 0;

        while (true)
        {
            if (posSource == source.size())
            {
                // We reached the end of the source list and we matched all the source entries => source is included in target
                return true;
            }

            if (posTarget == target.size())
            {
                // We reached the end of the target list => target has extra entries not in source.
                return false;
            }

            EngineeringUnits currentSource = source.get(posSource);
            EngineeringUnits currentTarget = target.get(posSource);

            if (currentSource == currentTarget)
            {
                posSource++;
                posTarget++;
            }
            else if (currentSource.ordinal() < currentTarget.ordinal())
            {
                //
                // The units in the lists are sorted by ordinal value.
                // Since source is before target, it means target doesn't have source.
                //
                return false;
            }
            else
            {
                //
                // Mismatch, move to the next target position.
                //
                posTarget++;
            }
        }
    }

    public EngineeringUnitsFactors compact()
    {
        if (m_normalized)
        {
            return this;
        }

        return simplify(true);
    }

    public String toIndexingString()
    {
        return toString(false);
    }

    @Override
    public String toString()
    {
        return toString(true);
    }

    private String toString(boolean display)
    {
        EngineeringUnits unitSimple = getPrimary();
        if (unitSimple != null)
        {
            return display ? unitSimple.getDisplayName() : unitSimple.name();
        }

        if (numeratorUnits.isEmpty() && denominatorUnits.isEmpty())
        {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        if (numeratorUnits.isEmpty())
        {
            sb.append("1");
        }
        else
        {
            boolean first = true;
            for (EngineeringUnits unit : numeratorUnits)
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    sb.append(" * ");
                }
                sb.append(display ? unit.getDisplayName() : unit.name());
            }
        }

        if (!denominatorUnits.isEmpty())
        {
            sb.append(" / ");

            boolean first = true;
            for (EngineeringUnits unit : denominatorUnits)
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    sb.append(" * ");
                }
                sb.append(display ? unit.getDisplayName() : unit.name());
            }
        }

        return sb.toString();
    }
}
