/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.optio3.protocol.model.bacnet.enums.BACnetConformance;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;

@Retention(RUNTIME)
@Target(FIELD)
@Repeatable(PropertyTypes.class)
public @interface PropertyType
{
    BACnetPropertyIdentifier property();

    Class<?> type();

    BACnetConformance conformance();

    boolean isArray() default (false);

    boolean isList() default (false);

    boolean isOptional() default (false);

    int arrayLength() default (0);
}
