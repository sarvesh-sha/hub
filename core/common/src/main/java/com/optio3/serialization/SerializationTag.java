/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.serialization;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(FIELD)
public @interface SerializationTag
{
    int number();

    int bitOffset() default (-1);

    int width() default (-1);

    int fixedArraySize() default (-1);

    boolean asLittleEndian() default (false);

    boolean asBigEndian() default (false);

    SerializationScaling[] scaling() default {};

    int preProcessorLowerRange() default (Integer.MIN_VALUE);

    int preProcessorUpperRange() default (Integer.MAX_VALUE);

    Class<? extends SerializationValueProcessor> preProcessor() default SerializationValueProcessor.class;

    Class<? extends SerializationValueProcessor> postProcessor() default SerializationValueProcessor.class;
}
