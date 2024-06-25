/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.modbus.explorer;

import com.optio3.asyncawait.CompileTime;

public class ModbusExplorer
{
    static
    {
        CompileTime.bootstrap();
    }

    public static void main(String[] args) throws
                                           Exception
    {
        new ModbusExplorerLogic().doMain(args);
    }
}
