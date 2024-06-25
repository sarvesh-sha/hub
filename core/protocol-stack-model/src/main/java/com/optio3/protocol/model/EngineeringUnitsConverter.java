/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model;

public abstract class EngineeringUnitsConverter
{
    public static final EngineeringUnitsConverter NoOp = new EngineeringUnitsConverter()
    {
        @Override
        public boolean isIdentityTransformation()
        {
            return true;
        }

        @Override
        public double convert(double value)
        {
            return value;
        }
    };

    //--//

    public abstract boolean isIdentityTransformation();

    public abstract double convert(double value);
}
