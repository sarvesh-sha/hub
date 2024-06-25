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
 * When applied to a parameter of an asynchronous computation, it tells the runtime to execute the computation in the background after a certain delay.
 * <p>
 * If the parameter is an {@code int} or {@code long}, it provides the value for the delay.
 * <br>
 * If the parameter is an {@link java.util.concurrent.TimeUnit}, it provides the unit for the delay. If missing, {@link java.util.concurrent.TimeUnit#SECONDS} is assumed.
 */
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface AsyncDelay
{
}
