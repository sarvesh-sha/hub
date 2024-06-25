/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.asyncawait;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Causes an asynchronous computation to be scheduled to run in a background thread, instead of the current thread.
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface AsyncBackground
{
    String reason() default "";
}
