/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model;

public abstract class EngineeringUnitsValidator
{
    public static final EngineeringUnitsValidator NoOp = new EngineeringUnitsValidator()
    {
        @Override
        public boolean isIdentityTransformation()
        {
            return true;
        }

        @Override
        public double validate(double value)
        {
            return value;
        }
    };

    //--//

    public static abstract class Factory
    {
        public abstract EngineeringUnitsValidator build(EngineeringUnitsFactors convertFrom,
                                                        EngineeringUnitsFactors convertTo);
    }

    public abstract boolean isIdentityTransformation();

    public abstract double validate(double value);

    static final class Temperature extends EngineeringUnitsValidator.Factory
    {
        @Override
        public EngineeringUnitsValidator build(EngineeringUnitsFactors convertFrom,
                                               EngineeringUnitsFactors convertTo)
        {
            EngineeringUnitsConverter converter = EngineeringUnits.buildConverter(convertFrom, EngineeringUnits.degrees_kelvin.getConversionFactors());

            return new EngineeringUnitsValidator()
            {
                @Override
                public boolean isIdentityTransformation()
                {
                    return false;
                }

                @Override
                public double validate(double value)
                {
                    if (!Double.isNaN(value))
                    {
                        double absoluteValue = converter.convert(value);
                        if (absoluteValue < 0 || absoluteValue > 3000)
                        {
                            return Double.NaN;
                        }
                    }

                    return value;
                }
            };
        }
    }

    static final class Ampere extends EngineeringUnitsValidator.Factory
    {
        @Override
        public EngineeringUnitsValidator build(EngineeringUnitsFactors convertFrom,
                                               EngineeringUnitsFactors convertTo)
        {
            EngineeringUnitsConverter converter = EngineeringUnits.buildConverter(convertFrom, EngineeringUnits.amperes.getConversionFactors());

            return new EngineeringUnitsValidator()
            {
                @Override
                public boolean isIdentityTransformation()
                {
                    return false;
                }

                @Override
                public double validate(double value)
                {
                    if (!Double.isNaN(value))
                    {
                        double valueNormalized = converter.convert(value);
                        if (Math.abs(valueNormalized) > 1_000_000)
                        {
                            return Double.NaN;
                        }
                    }

                    return value;
                }
            };
        }
    }

    static final class Volt extends EngineeringUnitsValidator.Factory
    {
        @Override
        public EngineeringUnitsValidator build(EngineeringUnitsFactors convertFrom,
                                               EngineeringUnitsFactors convertTo)
        {
            EngineeringUnitsConverter converter = EngineeringUnits.buildConverter(convertFrom, EngineeringUnits.volts.getConversionFactors());

            return new EngineeringUnitsValidator()
            {
                @Override
                public boolean isIdentityTransformation()
                {
                    return false;
                }

                @Override
                public double validate(double value)
                {
                    if (!Double.isNaN(value))
                    {
                        double valueNormalized = converter.convert(value);
                        if (Math.abs(valueNormalized) > 100_000_000)
                        {
                            return Double.NaN;
                        }
                    }

                    return value;
                }
            };
        }
    }

    static final class RelativeHumidity extends EngineeringUnitsValidator.Factory
    {
        @Override
        public EngineeringUnitsValidator build(EngineeringUnitsFactors convertFrom,
                                               EngineeringUnitsFactors convertTo)
        {
            EngineeringUnitsConverter converter = EngineeringUnits.buildConverter(convertFrom, EngineeringUnits.percent_relative_humidity.getConversionFactors());

            return new EngineeringUnitsValidator()
            {
                @Override
                public boolean isIdentityTransformation()
                {
                    return false;
                }

                @Override
                public double validate(double value)
                {
                    if (!Double.isNaN(value))
                    {
                        double valueNormalized = converter.convert(value);
                        if (valueNormalized < -1000 || valueNormalized > 1000)
                        {
                            return Double.NaN;
                        }
                    }

                    return value;
                }
            };
        }
    }

    static final class LimitRange extends EngineeringUnitsValidator.Factory
    {
        private final double m_min;
        private final double m_max;

        public LimitRange(double min,
                          double max)
        {
            m_min = min;
            m_max = max;
        }

        @Override
        public EngineeringUnitsValidator build(EngineeringUnitsFactors convertFrom,
                                               EngineeringUnitsFactors convertTo)
        {
            return new EngineeringUnitsValidator()
            {
                @Override
                public boolean isIdentityTransformation()
                {
                    return false;
                }

                @Override
                public double validate(double value)
                {
                    if (!Double.isNaN(value))
                    {
                        if (value < m_min || value > m_max)
                        {
                            return Double.NaN;
                        }
                    }

                    return value;
                }
            };
        }
    }
}
