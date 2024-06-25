/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util;

import java.util.Random;
import java.util.UUID;

public final class IdGenerator
{
    public static String newGuid()
    {
        return UUID.randomUUID()
                   .toString();
    }

    public static String newGuid(Random rnd)
    {
        byte[] bytes = new byte[16];

        rnd.nextBytes(bytes);

        return UUID.nameUUIDFromBytes(bytes)
                   .toString();
    }
}
