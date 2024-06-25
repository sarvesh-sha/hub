/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.asyncawait;

import java.io.Closeable;

/**
 * Just a marker to flag closable classes that cannot be used around a blocking call.
 */
public interface AsyncPreventBlocking extends Closeable
{
}
