/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks the mapping between Model classes and Persistence entities one-way, from Entity to Model.
 */
@Documented
@Retention(RUNTIME)
@Target({ FIELD })
public @interface Optio3MapAsReadOnly
{
}
