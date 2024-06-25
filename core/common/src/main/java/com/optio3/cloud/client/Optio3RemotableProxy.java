/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks an interface as remotable.
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE })
public @interface Optio3RemotableProxy
{
    /**
     * If set, the value will be used to identify the interface between servers, otherwise the interface's class name will be used.
     */
    String uniqueIdentifier() default "";
}
