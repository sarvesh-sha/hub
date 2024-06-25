/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.ipn.explorer;

import com.optio3.asyncawait.CompileTime;

public class ObdiiExplorer
{
    static
    {
        CompileTime.bootstrap();
    }

    public static void main(String[] args) throws
                                           Exception
    {
        new ObdiiExplorerLogic().doMain(args);
    }
}