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
 * When applied to a Hibernate association, this controls what happens when the referenced entity is deleted.
 */
@Documented
@Retention(RUNTIME)
@Target({ FIELD })
public @interface Optio3Cascade
{
    public enum Flavor
    {
        CLEAR,
        DELETE,
        PREVENT
    }

    Flavor mode();

    String getter();

    String setter() default ("");
}
