/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.logic;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(METHOD)
public @interface BackgroundActivityMethod
{
    Class<?> stateClass() default void.class;

    boolean initial() default false;

    boolean autoRetry() default false;

    int maxRetries() default Integer.MAX_VALUE;

    boolean needsSession() default false;
}
