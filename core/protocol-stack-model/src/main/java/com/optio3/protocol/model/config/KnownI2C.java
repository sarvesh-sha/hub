/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.config;

public enum KnownI2C
{
    // @formatter:off
    MMA_8653("MMA8653FC", 0x1D),
    SHT3x   ("SHT3x"    , 0x44),
    MCP3428 ("MCP3428"  , 0x68);
	// @formatter:on

    public final String name;
    public final int    defaultAddress;

    KnownI2C(String name,
             int defaultAddress)
    {
        this.name           = name;
        this.defaultAddress = defaultAddress;
    }

    public static KnownI2C resolveDefaultAddress(int address)
    {
        for (var value : values())
        {
            if (value.defaultAddress == address)
            {
                return value;
            }
        }

        return null;
    }
}
