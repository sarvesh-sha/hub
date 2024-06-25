/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util.concurrency;

import java.util.concurrent.TimeoutException;

public class DeadlineTimeoutException extends TimeoutException
{
    public DeadlineTimeoutException()
    {
    }

    public DeadlineTimeoutException(String message)
    {
        super(message);
    }
}
