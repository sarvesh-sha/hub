/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks an implementation of a remotable interface.
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE })
public @interface Optio3RemotableEndpoint
{
    /**
     * The interface this endpoint implements.
     */
    Class<?> itf();
}
