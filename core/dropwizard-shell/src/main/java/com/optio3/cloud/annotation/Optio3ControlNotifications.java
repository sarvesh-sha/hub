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
 * When applied to a Hibernate association, this controls the notifications genereted by activity of the referenced entity.
 */
@Documented
@Retention(RUNTIME)
@Target({ FIELD })
public @interface Optio3ControlNotifications
{
    public enum Notify
    {
        IGNORE,
        NEVER,
        ON_ASSOCIATION_CHANGES,
        ALWAYS
    }

    String reason();

    Notify direct();

    Notify reverse();

    String getter() default ("");

    boolean markerForLeftJoin() default (false);
}
