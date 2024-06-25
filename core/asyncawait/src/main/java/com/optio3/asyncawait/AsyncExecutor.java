/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.asyncawait;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * When applied to a parameter of an asynchronous computation, it tells the runtime which executor to use for background processing.
 */
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface AsyncExecutor
{
}
